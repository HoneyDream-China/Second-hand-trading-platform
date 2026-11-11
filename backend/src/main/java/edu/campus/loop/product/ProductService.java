package edu.campus.loop.product;

import edu.campus.loop.common.BusinessException;
import edu.campus.loop.common.BusinessNotImplementedException;
import edu.campus.loop.common.PageView;
import edu.campus.loop.product.ProductRepository.StoredProduct;
import edu.campus.loop.user.CurrentUserContext;
import edu.campus.loop.user.UserDtos.ActiveUser;
import edu.campus.loop.user.UserDtos.UserStatus;
import edu.campus.loop.user.UserFacade;
import edu.campus.loop.user.UserFailure;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import static edu.campus.loop.product.ProductDtos.*;

/**
 * 商品业务服务：发布、公开查询、卖家编辑、上下架以及交易模块所需的成交边界。
 * <p>所有写操作使用商品 version 做乐观锁；编辑和上下架还要求当前用户就是商品卖家。
 * 商品状态迁移集中在 {@link ProductStateMachine}，SOLD 只能由 {@link #markSold(long, long)} 写入。</p>
 * <p>未配置数据库或认证上下文时返回明确的 501，不使用内存商品或演示用户。</p>
 */
@Service
public class ProductService implements ProductFacade {
    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");

    private final ObjectProvider<CurrentUserContext> contexts;
    private final UserFacade users;
    private final ObjectProvider<ProductRepository> repositories;
    private final ObjectProvider<PlatformTransactionManager> transactions;

    /** 注入公开 Facade；持久化和认证基础设施延迟取得，保证未配置数据库时骨架仍可启动。 */
    public ProductService(ObjectProvider<CurrentUserContext> contexts, UserFacade users,
            ObjectProvider<ProductRepository> repositories, ObjectProvider<PlatformTransactionManager> transactions) {
        this.contexts = contexts;
        this.users = users;
        this.repositories = repositories;
        this.transactions = transactions;
    }

    /**
     * 发布商品：校验当前活跃卖家与请求字段，写入 DRAFT 商品及按输入顺序排列的图片。
     * @param request 商品字段与图片地址
     * @return 初始状态为 DRAFT、version 为 0 的商品视图
     */
    public ProductView create(CreateProductRequest request) {
        if (request == null) throw validation("商品信息不能为空");
        ActiveUser seller = currentUser();
        ValidatedProduct product = validate(request.title(), request.description(), request.price(),
                request.category(), request.condition(), request.imageUrls());
        return inTransaction(false, () -> {
            ProductRepository repository = repository();
            OffsetDateTime now = OffsetDateTime.now();
            StoredProduct row = repository.insert(seller.id(), product.title(), product.description(),
                    product.price(), product.category(), product.condition(), now);
            repository.insertImages(row.id(), product.imageUrls(), now);
            return view(row, product.imageUrls(), seller.nickname());
        });
    }

    /**
     * 公开分页查询商品。未指定状态时只返回 ON_SALE；关键词匹配标题或描述，价格区间为闭区间。
     * @param page 页码，从 1 开始
     * @param size 每页条数，1～100
     * @param keyword 可选关键词
     * @param category 可选分类
     * @param minPrice 可选最低价
     * @param maxPrice 可选最高价
     * @param condition 可选成色
     * @param status 可选状态，null 时使用 ON_SALE
     * @return 商品分页视图
     */
    public PageView<ProductView> list(int page, int size, String keyword, String category,
            BigDecimal minPrice, BigDecimal maxPrice, String condition, ProductStatus status) {
        if (page < 1) throw validation("页码至少为 1");
        if (size < 1 || size > 100) throw validation("每页条数必须为 1～100");
        if (minPrice != null && minPrice.signum() < 0) throw validation("最低价格不能小于 0");
        if (maxPrice != null && maxPrice.signum() < 0) throw validation("最高价格不能小于 0");
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw validation("最低价格不能大于最高价格");
        }
        ProductRepository.Filter filter = new ProductRepository.Filter(
                text(keyword), text(category), minPrice, maxPrice, text(condition),
                status == null ? ProductStatus.ON_SALE : status);
        return inTransaction(true, () -> {
            ProductRepository repository = repository();
            long total = repository.count(filter);
            List<StoredProduct> rows = repository.list(filter, size, (long) (page - 1) * size);
            Map<Long, List<String>> images = repository.images(rows.stream().map(StoredProduct::id).toList());
            Map<Long, String> sellers = new HashMap<>();
            List<ProductView> items = rows.stream().map(row -> view(row,
                    images.getOrDefault(row.id(), List.of()),
                    sellers.computeIfAbsent(row.sellerId(), this::sellerName))).toList();
            return new PageView<>(items, page, size, total);
        });
    }

    /**
     * 查询商品详情，图片按 sort_order 返回；只包含公开卖家昵称，不暴露认证信息。
     * @param id 商品 ID
     * @return 商品详情
     */
    public ProductView detail(long id) {
        return inTransaction(true, () -> {
            ProductRepository repository = repository();
            StoredProduct row = repository.find(id);
            if (row == null) throw notFound("商品不存在");
            return view(row, repository.images(id), sellerName(row.sellerId()));
        });
    }

    /**
     * 卖家编辑商品：必须携带当前 version，非卖家返回 FORBIDDEN，版本不匹配返回 PRODUCT_VERSION_CONFLICT，
     * 已售商品不可再修改。图片集合与商品字段在同一事务中原子替换。
     * @param id 商品 ID
     * @param request 商品字段、图片和期望版本
     * @return 更新后的商品视图
     */
    public ProductView update(long id, UpdateProductRequest request) {
        if (request == null) throw validation("商品信息不能为空");
        long expectedVersion = version(request.version());
        ActiveUser actor = currentUser();
        ValidatedProduct product = validate(request.title(), request.description(), request.price(),
                request.category(), request.condition(), request.imageUrls());
        return inTransaction(false, () -> {
            ProductRepository repository = repository();
            StoredProduct existing = repository.find(id);
            if (existing == null) throw notFound("商品不存在");
            requireOwner(existing, actor.id());
            if (!ProductStateMachine.editable(existing.status())) throw notTradable("已售商品不可修改");
            if (existing.version() != expectedVersion) throw versionConflict();
            OffsetDateTime now = OffsetDateTime.now();
            int changed = repository.update(id, actor.id(), expectedVersion, product.title(),
                    product.description(), product.price(), product.category(), product.condition(), now);
            if (changed == 0) assertUpdated(repository, id, actor.id(), expectedVersion);
            repository.replaceImages(id, product.imageUrls(), now);
            StoredProduct updated = repository.find(id);
            return view(updated, product.imageUrls(), actor.nickname());
        });
    }

    /**
     * 卖家上下架：DRAFT/OFF_SHELF 可以上架为 ON_SALE，ON_SALE 可以下架为 OFF_SHELF。
     * 重复提交同一状态时幂等返回当前商品，不递增 version；SOLD 只能由交易模块写入。
     * @param id 商品 ID
     * @param request 目标状态和期望版本
     * @return 操作后的商品视图
     */
    public ProductView changeStatus(long id, StatusRequest request) {
        if (request == null || request.status() == null) throw validation("目标状态不能为空");
        long expectedVersion = version(request.version());
        ActiveUser actor = currentUser();
        ProductStatus target = request.status();
        return inTransaction(false, () -> {
            ProductRepository repository = repository();
            StoredProduct existing = repository.find(id);
            if (existing == null) throw notFound("商品不存在");
            requireOwner(existing, actor.id());
            if (existing.status() == target) {
                return view(existing, repository.images(id), actor.nickname());
            }
            if (target == ProductStatus.SOLD) {
                throw error(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION",
                        "商品售出必须通过交易模块完成");
            }
            if (!ProductStateMachine.canTransition(existing.status(), target)) {
                throw notTradable("商品当前状态不允许变更为 " + target);
            }
            if (existing.version() != expectedVersion) throw versionConflict();
            OffsetDateTime now = OffsetDateTime.now();
            int changed = repository.updateStatus(id, actor.id(), expectedVersion, existing.status(), target, now);
            if (changed == 0) {
                StoredProduct latest = repository.find(id);
                if (latest == null) throw notFound("商品不存在");
                if (latest.sellerId() != actor.id()) throw forbidden("仅商品卖家可以操作该商品");
                if (latest.status() == target) {
                    return view(latest, repository.images(id), actor.nickname());
                }
                if (!ProductStateMachine.editable(latest.status())) throw notTradable("商品当前状态不允许变更");
                throw versionConflict();
            }
            StoredProduct updated = repository.find(id);
            return view(updated, repository.images(id), actor.nickname());
        });
    }

    /**
     * 交易模块读取可交易商品快照；只有 ON_SALE 商品可交易。
     * @param productId 商品 ID
     * @return 卖家、价格和当前版本
     */
    @Override
    public TradableProduct getTradableProduct(long productId) {
        StoredProduct row = repository().find(productId);
        if (row == null) throw notFound("商品不存在");
        if (row.status() != ProductStatus.ON_SALE) throw notTradable("商品不在售，无法交易");
        return new TradableProduct(row.id(), row.sellerId(), row.price(), row.version());
    }

    /**
     * 交易成交：按期望版本把 ON_SALE 商品改为 SOLD 并递增 version。
     * <p>此方法不自行开启事务，必须由交易模块在已锁定商品行的事务中调用。</p>
     * @param productId 商品 ID
     * @param expectedVersion 期望版本
     */
    @Override
    public void markSold(long productId, long expectedVersion) {
        int changed = repository().markSold(productId, expectedVersion, OffsetDateTime.now());
        if (changed == 1) return;
        StoredProduct row = repository().find(productId);
        if (row == null) throw notFound("商品不存在");
        if (row.status() != ProductStatus.ON_SALE) throw notTradable("商品不在售，无法成交");
        throw versionConflict();
    }

    /**
     * 在调用方事务中锁定商品行；不存在时返回 RESOURCE_NOT_FOUND。
     * @param productId 商品 ID
     */
    @Override
    public void lockProduct(long productId) {
        if (!repository().lock(productId)) throw notFound("商品不存在");
    }

    /** 当前登录用户必须存在且处于 ACTIVE；身份只来自认证上下文。 */
    private ActiveUser currentUser() {
        CurrentUserContext context = contexts.getIfAvailable();
        if (context == null) throw new BusinessNotImplementedException();
        ActiveUser user;
        try {
            user = context.requireUser();
        } catch (UserFailure failure) {
            throw translate(failure);
        }
        if (user == null) throw error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
        if (user.status() != UserStatus.ACTIVE) throw forbidden("用户不存在或已停用");
        try {
            ActiveUser active = users.getActiveUser(user.id());
            if (active == null || active.status() != UserStatus.ACTIVE) throw forbidden("用户不存在或已停用");
            return active;
        } catch (UserFailure failure) {
            throw forbidden("用户不存在或已停用");
        }
    }

    /**
     * 通过用户模块公开协议读取卖家昵称；已停用/已删除卖家仍保留商品，但展示为已注销用户。
     * 商品列表会按卖家去重，避免逐商品查询。
     */
    private String sellerName(long sellerId) {
        try {
            ActiveUser seller = users.getActiveUser(sellerId);
            if (seller != null && seller.status() == UserStatus.ACTIVE) return seller.nickname();
        } catch (UserFailure inactiveOrMissing) {
            // 商品不因卖家停用而消失；仅降级展示名称。
        }
        return "已注销用户";
    }

    private void requireOwner(StoredProduct product, long userId) {
        if (product.sellerId() != userId) throw forbidden("仅商品卖家可以操作该商品");
    }

    /** update 受影响行数为 0 时复查最新行，给出确定的不存在、越权、已售或版本冲突错误。 */
    private void assertUpdated(ProductRepository repository, long id, long sellerId, long expectedVersion) {
        StoredProduct latest = repository.find(id);
        if (latest == null) throw notFound("商品不存在");
        if (latest.sellerId() != sellerId) throw forbidden("仅商品卖家可以操作该商品");
        if (!ProductStateMachine.editable(latest.status())) throw notTradable("已售商品不可修改");
        if (latest.version() != expectedVersion) throw versionConflict();
        throw versionConflict();
    }

    private ValidatedProduct validate(String title, String description, BigDecimal price, String category,
            String condition, List<String> imageUrls) {
        String validTitle = required(title, 120, "商品标题不能为空且最多 120 个字符");
        if (description == null || description.isBlank()) throw validation("商品描述不能为空");
        BigDecimal validPrice = money(price);
        String validCategory = required(category, 64, "商品分类不能为空且最多 64 个字符");
        String validCondition = required(condition, 32, "商品成色不能为空且最多 32 个字符");
        return new ValidatedProduct(validTitle, description.trim(), validPrice, validCategory, validCondition,
                imageUrls(imageUrls));
    }

    private String required(String value, int maxLength, String message) {
        if (value == null || value.isBlank()) throw validation(message);
        String text = value.trim();
        if (text.length() > maxLength) throw validation(message);
        return text;
    }

    private BigDecimal money(BigDecimal price) {
        if (price == null || price.signum() < 0) throw validation("价格必须大于等于 0");
        final BigDecimal amount;
        try {
            amount = price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException tooManyDecimals) {
            throw validation("价格最多保留两位小数");
        }
        if (amount.compareTo(MAX_PRICE) > 0) throw validation("价格超出允许范围");
        return amount;
    }

    private List<String> imageUrls(List<String> imageUrls) {
        if (imageUrls == null) throw validation("图片列表不能为空");
        if (imageUrls.size() > 9) throw validation("商品图片最多上传 9 张");
        List<String> urls = new ArrayList<>(imageUrls.size());
        for (String imageUrl : imageUrls) {
            if (imageUrl == null || imageUrl.isBlank()) throw validation("图片地址不能为空");
            String url = imageUrl.trim();
            if (url.length() > 512) throw validation("图片地址最多 512 个字符");
            urls.add(url);
        }
        return List.copyOf(urls);
    }

    private long version(Long version) {
        if (version == null || version < 0) throw validation("版本号必须大于等于 0");
        return version;
    }

    private String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ProductView view(StoredProduct row, List<String> images, String sellerName) {
        return new ProductView(row.id(), row.sellerId(), sellerName, row.title(), row.description(),
                row.price(), row.category(), row.condition(), List.copyOf(images), row.status(),
                row.version(), row.createdAt(), row.updatedAt());
    }

    private ProductRepository repository() {
        ProductRepository repository = repositories.getIfAvailable();
        if (repository == null) throw new BusinessNotImplementedException();
        return repository;
    }

    /** 显式事务使商品仓储与交易模块的 Facade 调用共用线程绑定连接；读操作使用一致快照。 */
    private <T> T inTransaction(boolean readOnly, Supplier<T> action) {
        PlatformTransactionManager manager = transactions.getIfAvailable();
        if (manager == null) throw new BusinessNotImplementedException();
        TransactionTemplate transaction = new TransactionTemplate(manager);
        transaction.setReadOnly(readOnly);
        if (readOnly) transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return transaction.execute(status -> action.get());
    }

    private BusinessException translate(UserFailure failure) {
        String message = failure.getReason() == null ? "登录状态无效" : failure.getReason();
        return error(HttpStatus.valueOf(failure.getStatusCode().value()), failure.code(), message);
    }

    private BusinessException validation(String message) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private BusinessException forbidden(String message) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    private BusinessException notFound(String message) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    private BusinessException versionConflict() {
        return error(HttpStatus.CONFLICT, "PRODUCT_VERSION_CONFLICT", "商品已被更新，请刷新后重试");
    }

    private BusinessException notTradable(String message) {
        return error(HttpStatus.CONFLICT, "PRODUCT_NOT_TRADABLE", message);
    }

    private BusinessException error(HttpStatus status, String code, String message) {
        return new BusinessException(status, code, message);
    }

    /** 服务层校验后的商品字段，所有字符串已去除首尾空白，价格固定为两位小数。 */
    private record ValidatedProduct(String title, String description, BigDecimal price, String category,
            String condition, List<String> imageUrls) { }
}
