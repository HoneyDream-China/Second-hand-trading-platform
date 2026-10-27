package edu.campus.loop.trade;

import edu.campus.loop.common.BusinessNotImplementedException;
import edu.campus.loop.common.PageView;
import org.springframework.stereotype.Service;
import static edu.campus.loop.trade.TradeDtos.*;

/**
 * 交易业务服务，负责申请、参与者查询、权限校验及状态流转。
 * <p>当前所有方法均为占位实现，仅抛出 BusinessNotImplementedException；
 * 下列业务规则描述后续实现要求，并不表示已完成认证、持久化或事务处理。</p>
 * <p>跨模块只通过 UserFacade / ProductFacade 协作，不直接访问其他模块的数据表。</p>
 */
@Service
public class TradeService {
    // TODO 成员 C：只通过 UserFacade / ProductFacade 校验、读取和变更商品。
    // TODO 接受交易：同一事务，先锁商品后锁交易；markSold、交易状态、日志必须原子提交。
    // TODO 创建通过数据库部分唯一索引防重；所有状态操作校验操作者、幂等和合法迁移。
    /**
     * 发起交易申请。
     * <p>待实现：校验当前用户可交易、商品可交易且不属于自己；从商品快照读取卖家，
     * 创建 PENDING 交易并记录初始状态。依赖数据库部分唯一索引，
     * 保证同一商品最多一笔 PENDING 交易，并处理并发重复申请。</p>
     * @param request 商品 ID 与可选留言，控制器已对请求体做格式校验
     * @return 创建后的交易详情
     * @throws BusinessNotImplementedException 当前业务尚未实现
     */
    public TradeView create(CreateTradeRequest request) { throw new BusinessNotImplementedException(); }

    /**
     * 分页查询当前用户参与的交易。
     * <p>待实现：限定当前用户为买家或卖家，再按可选状态过滤并分页，统计总条数。</p>
     * @param page 页码，从 1 开始
     * @param size 每页条数，接口声明范围为 1～100
     * @param status 状态过滤条件，null 表示不过滤状态
     * @return 交易列表及分页信息
     * @throws BusinessNotImplementedException 当前业务尚未实现
     */
    public PageView<TradeView> list(int page, int size, TradeStatus status) { throw new BusinessNotImplementedException(); }

    /**
     * 查询指定交易及状态记录。
     * <p>待实现：检查交易存在，且当前用户为该交易的买家或卖家。</p>
     * @param id 交易 ID
     * @return 交易详情及状态记录
     * @throws BusinessNotImplementedException 当前业务尚未实现
     */
    public TradeView detail(long id) { throw new BusinessNotImplementedException(); }

    /**
     * 卖家接受交易，预期状态流转为 PENDING → ACCEPTED。
     * <p>待实现：在同一事务内先锁商品、再锁交易；校验卖家权限、商品版本、
     * 商品 ON_SALE 状态及交易 PENDING 状态。调用 ProductFacade.markSold，
     * 更新交易并追加状态日志，任一步失败都整体回滚；重复请求需按幂等规则处理。</p>
     * @param id 待接受的交易 ID
     * @return 接受后的交易详情
     * @throws BusinessNotImplementedException 当前业务尚未实现
     */
    public TradeView accept(long id) { throw new BusinessNotImplementedException(); }

    /**
     * 卖家拒绝交易，预期状态流转为 PENDING → REJECTED。
     * <p>待实现：锁定交易，校验卖家权限、合法迁移及幂等性，
     * 在同一事务内更新交易和追加日志，不修改商品状态。</p>
     * @param id 待拒绝的交易 ID
     * @return 拒绝后的交易详情
     * @throws BusinessNotImplementedException 当前业务尚未实现
     */
    public TradeView reject(long id) { throw new BusinessNotImplementedException(); }

    /**
     * 买家取消交易，预期状态流转为 PENDING → CANCELLED。
     * <p>待实现：锁定交易，校验当前用户为买家、合法迁移及幂等性，
     * 在同一事务内更新交易和追加日志，不修改商品状态。</p>
     * @param id 待取消的交易 ID
     * @return 取消后的交易详情
     * @throws BusinessNotImplementedException 当前业务尚未实现
     */
    public TradeView cancel(long id) { throw new BusinessNotImplementedException(); }
}
