package edu.campus.loop.trade;

import edu.campus.loop.common.BusinessException;
import edu.campus.loop.common.BusinessNotImplementedException;
import edu.campus.loop.common.PageView;
import edu.campus.loop.product.ProductFacade;
import edu.campus.loop.user.CurrentUserContext;
import edu.campus.loop.user.UserFacade;
import edu.campus.loop.user.UserDtos.ActiveUser;
import edu.campus.loop.user.UserDtos.UserStatus;
import java.time.OffsetDateTime;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import static edu.campus.loop.trade.TradeDtos.*;

/**
 * 交易业务服务，负责申请、参与者查询、权限校验及状态流转。
 * 跨模块只通过 UserFacade / ProductFacade 协作，数据库变更共用一个事务。
 * 未配置数据库或尚未提供认证上下文时明确返回未实现，不使用演示用户或内存交易。
 */
@Service
public class TradeService {
    private final ObjectProvider<CurrentUserContext> contexts;
    private final UserFacade users;
    private final ProductFacade products;
    private final ObjectProvider<TradeRepository> repositories;
    private final ObjectProvider<PlatformTransactionManager> transactions;

    /** 注入公开 Facade；延迟取得尚未接入的基础设施，保持原骨架可启动。 */
    public TradeService(ObjectProvider<CurrentUserContext> contexts, UserFacade users, ProductFacade products,
            ObjectProvider<TradeRepository> repositories, ObjectProvider<PlatformTransactionManager> transactions) {
        this.contexts = contexts;
        this.users = users;
        this.products = products;
        this.repositories = repositories;
        this.transactions = transactions;
    }

    /**
     * 发起交易申请：校验活跃买卖双方、商品状态及归属，创建 PENDING 交易及初始日志。
     * 同一商品仅允许一笔待处理交易，由 PostgreSQL 部分唯一索引处理并发防重。
     * @param request 商品 ID 与可选留言（服务层也检查格式，支持非 HTTP 调用）
     * @return 创建后的交易详情
     */
    public TradeView create(CreateTradeRequest request) {
        long buyerId = currentUserId();
        if (request == null || request.productId() == null || request.productId() <= 0
                || (request.message() != null && request.message().length() > 500)) {
            throw error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "商品 ID 必须为正数，留言最多 500 字符");
        }
        try {
            return inTransaction(false, () -> {
                // 与接受交易使用相同的商品行锁，避免商品售出与新建申请交错提交。
                products.lockProduct(request.productId());
                var product = products.getTradableProduct(request.productId());
                if (product.sellerId() == buyerId) {
                    throw error(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION", "不能申请自己的商品");
                }
                requireActive(users.getActiveUser(product.sellerId()));
                TradeRepository repository = repository();
                OffsetDateTime now = OffsetDateTime.now();
                long id = repository.insert(request, buyerId, product.sellerId(), now);
                repository.appendLog(id, null, TradeStatus.PENDING, buyerId, now);
                return repository.withLogs(repository.find(id, false));
            });
        } catch (DuplicateKeyException exception) {
            // 在事务退出并回滚后翻译唯一键冲突，不在已失败的 PostgreSQL 事务内继续查询。
            throw error(HttpStatus.CONFLICT, "ACTIVE_TRADE_EXISTS", "该商品已有待处理交易");
        }
    }

    /**
     * 分页查询当前用户作为买家或卖家参与的交易；使用同一读取快照统计总数并获取日志。
     * @param page 页码，从 1 开始
     * @param size 每页条数，范围为 1～100
     * @param status 状态过滤条件，null 表示不过滤
     * @return 交易列表及分页信息
     */
    public PageView<TradeView> list(int page, int size, TradeStatus status) {
        long userId = currentUserId();
        if (page < 1 || size < 1 || size > 100) {
            throw error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "页码至少为 1，每页条数必须为 1～100");
        }
        return inTransaction(true, () -> {
            TradeRepository repository = repository();
            long total = repository.count(userId, status);
            var items = repository.list(userId, page, size, status).stream().map(repository::withLogs).toList();
            return new PageView<>(items, page, size, total);
        });
    }

    /**
     * 查询交易详情和按时间排列的状态记录，仅交易买家或卖家可查看。
     * @param id 交易 ID
     * @return 交易详情及状态记录
     */
    public TradeView detail(long id) {
        long userId = currentUserId();
        return inTransaction(true, () -> {
            TradeView trade = repository().find(id, false);
            requireParticipant(trade, userId);
            return repository().withLogs(trade);
        });
    }

    /**
     * 卖家接受交易：先锁商品，再锁交易，读取最新在售商品版本并调用 markSold。
     * 商品售出、交易改为 ACCEPTED 和追加日志在同一事务中提交，异常时整体回滚。
     * @param id 待接受的交易 ID
     * @return 接受后的详情；重复接受返回原结果，不重复售出或追加日志
     */
    public TradeView accept(long id) {
        long userId = currentUserId();
        return inTransaction(false, () -> {
            TradeRepository repository = repository();
            TradeView initial = repository.find(id, false);
            requireSeller(initial, userId);
            // 终态不会再次迁移，重试已接受交易时无需要求商品仍然在售。
            if (initial.status() == TradeStatus.ACCEPTED) return repository.withLogs(initial);
            TradeStateMachine.requireTransition(initial.status(), TradeStatus.ACCEPTED);
            products.lockProduct(initial.productId());
            TradeView locked = repository.find(id, true);
            requireSeller(locked, userId);
            if (locked.status() == TradeStatus.ACCEPTED) return repository.withLogs(locked);
            TradeStateMachine.requireTransition(locked.status(), TradeStatus.ACCEPTED);
            var product = products.getTradableProduct(locked.productId());
            if (product.sellerId() != locked.sellerId()) {
                throw error(HttpStatus.CONFLICT, "PRODUCT_NOT_TRADABLE", "商品卖家与交易记录不一致");
            }
            products.markSold(product.id(), product.version());
            return changeStatus(locked, TradeStatus.ACCEPTED, userId);
        });
    }

    /**
     * 卖家拒绝待处理交易；锁定交易后校验权限及迁移，原子更新交易与日志，不修改商品。
     * @param id 待拒绝的交易 ID
     * @return 拒绝后的详情；重复拒绝不新增日志
     */
    public TradeView reject(long id) { return finish(id, TradeStatus.REJECTED); }

    /**
     * 买家取消自己的待处理交易；锁定交易后校验权限及迁移，原子更新交易与日志。
     * @param id 待取消的交易 ID
     * @return 取消后的详情；重复取消不新增日志
     */
    public TradeView cancel(long id) { return finish(id, TradeStatus.CANCELLED); }

    /** 拒绝和取消共用的事务流程，先鉴权再处理幂等，防止越权重试。 */
    private TradeView finish(long id, TradeStatus target) {
        long userId = currentUserId();
        return inTransaction(false, () -> {
            TradeView trade = repository().find(id, true);
            if (target == TradeStatus.CANCELLED) {
                if (trade.buyerId() != userId) throw error(HttpStatus.FORBIDDEN, "FORBIDDEN", "仅交易买家可取消");
            } else {
                requireSeller(trade, userId);
            }
            if (trade.status() == target) return repository().withLogs(trade);
            TradeStateMachine.requireTransition(trade.status(), target);
            return changeStatus(trade, target, userId);
        });
    }

    /** 调用方已经加锁并校验；只更新当前状态，历史日志保持追加写入。 */
    private TradeView changeStatus(TradeView trade, TradeStatus target, long operatorId) {
        TradeRepository repository = repository();
        OffsetDateTime now = OffsetDateTime.now();
        repository.updateStatus(trade.id(), target, now);
        repository.appendLog(trade.id(), trade.status(), target, operatorId, now);
        return repository.withLogs(repository.find(trade.id(), false));
    }

    /** 所有入口都要求已认证且活跃的用户，用户身份不能来自请求参数。 */
    private long currentUserId() {
        CurrentUserContext context = contexts.getIfAvailable();
        if (context == null) throw new BusinessNotImplementedException();
        ActiveUser user = context.requireUser();
        if (user == null) throw error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
        requireActive(user);
        requireActive(users.getActiveUser(user.id()));
        return user.id();
    }

    private void requireActive(ActiveUser user) {
        if (user == null || user.status() != UserStatus.ACTIVE) {
            throw error(HttpStatus.FORBIDDEN, "FORBIDDEN", "用户不存在或已停用");
        }
    }

    private void requireParticipant(TradeView trade, long userId) {
        if (trade.buyerId() != userId && trade.sellerId() != userId) {
            throw error(HttpStatus.FORBIDDEN, "FORBIDDEN", "仅交易双方可查看");
        }
    }

    private void requireSeller(TradeView trade, long userId) {
        if (trade.sellerId() != userId) throw error(HttpStatus.FORBIDDEN, "FORBIDDEN", "仅交易卖家可操作");
    }

    private TradeRepository repository() {
        TradeRepository repository = repositories.getIfAvailable();
        if (repository == null) throw new BusinessNotImplementedException();
        return repository;
    }

    /** 显式事务使 Facade 与交易仓储共用线程绑定连接；读操作使用一致快照。 */
    private <T> T inTransaction(boolean readOnly, Supplier<T> action) {
        PlatformTransactionManager manager = transactions.getIfAvailable();
        if (manager == null) throw new BusinessNotImplementedException();
        TransactionTemplate transaction = new TransactionTemplate(manager);
        transaction.setReadOnly(readOnly);
        if (readOnly) transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return transaction.execute(status -> action.get());
    }

    private BusinessException error(HttpStatus status, String code, String message) {
        return new BusinessException(status, code, message);
    }
}
