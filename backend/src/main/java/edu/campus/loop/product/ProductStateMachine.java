package edu.campus.loop.product;

import edu.campus.loop.product.ProductDtos.ProductStatus;

/**
 * 商品生命周期状态机：DRAFT -> ON_SALE -> SOLD / OFF_SHELF，下架后允许重新上架。
 * <p>SOLD 是终态，只能由交易模块通过 {@link ProductFacade#markSold(long, long)} 写入，
 * 卖家接口不能直接把商品状态改为 SOLD。</p>
 */
public final class ProductStateMachine {
    private ProductStateMachine() { }

    /** 卖家可编辑/上下架的状态；已售商品不允许再修改。 */
    public static boolean editable(ProductStatus status) {
        return status != null && status != ProductStatus.SOLD;
    }

    /** 判断卖家主动操作是否属于合法状态迁移。 */
    public static boolean canTransition(ProductStatus from, ProductStatus to) {
        if (from == null || to == null || from == to) return false;
        return switch (from) {
            case DRAFT -> to == ProductStatus.ON_SALE;
            case OFF_SHELF -> to == ProductStatus.ON_SALE;
            case ON_SALE -> to == ProductStatus.OFF_SHELF || to == ProductStatus.SOLD;
            case SOLD -> false;
        };
    }
}
