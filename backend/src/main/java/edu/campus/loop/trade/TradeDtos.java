package edu.campus.loop.trade;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 交易模块的数据传输对象集合，定义请求参数、响应字段和状态枚举。
 * record 用于承载数据；其中的约束注解在控制器通过 @Valid 校验请求体时生效。
 */
public final class TradeDtos {
    /** 工具容器不需要实例化，内部 DTO 直接通过类型名使用。 */
    private TradeDtos() { }

    /** 预期状态流转：PENDING → ACCEPTED / REJECTED / CANCELLED，终态不再迁移。 */
    public enum TradeStatus {
        /** 待处理：买家已申请，等待卖家处理或买家取消。 */
        PENDING,
        /** 已接受：卖家接受交易，对应商品应同步标记为已售。 */
        ACCEPTED,
        /** 已拒绝：卖家拒绝交易。 */
        REJECTED,
        /** 已取消：买家取消交易。 */
        CANCELLED
    }

    /**
     * 创建交易的请求体，仅允许指定商品和留言。
     * @param productId 商品 ID，必填且大于 0
     * @param message 可选留言，允许为 null，最多 500 字符
     */
    public record CreateTradeRequest(@NotNull @Positive Long productId, @Size(max=500) String message) { }

    /**
     * 对外展示的一条交易状态记录，不包含数据库审计表中的操作者等内部字段。
     * @param status 本次记录的交易状态
     * @param at 状态记录时间，包含时区偏移量
     */
    public record StatusLog(TradeStatus status, OffsetDateTime at) { }

    /**
     * 创建、详情查询、状态操作及分页列表共用的交易响应对象。
     * @param id 交易 ID
     * @param productId 关联商品 ID
     * @param buyerId 发起交易的买家用户 ID
     * @param sellerId 创建交易时从商品读取的卖家用户 ID
     * @param message 买家申请时填写的留言
     * @param status 当前交易状态
     * @param createdAt 交易创建时间，包含时区偏移量
     * @param updatedAt 交易最近更新时间，包含时区偏移量
     * @param logs 交易状态记录列表
     */
    public record TradeView(long id, long productId, long buyerId, long sellerId, String message,
            TradeStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt, List<StatusLog> logs) { }
}
