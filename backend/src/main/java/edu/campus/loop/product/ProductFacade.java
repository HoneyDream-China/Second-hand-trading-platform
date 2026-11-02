package edu.campus.loop.product;

/** 交易模块的唯一商品写入边界。 */
public interface ProductFacade {
    /**
     * 在调用方的事务中锁定商品行（SELECT ... FOR UPDATE），必须先于交易行加锁。
     * 商品模块实现时必须使用同一 DataSource 和事务，锁保持到外层事务结束；
     * 此方法只锁定和检查存在性，不要求商品仍在售，以支持重复接受请求。
     * 默认明确拒绝操作，避免尚未接入商品模块时绕过并发保护。
     */
    default void lockProduct(long productId) {
        throw new edu.campus.loop.common.BusinessNotImplementedException();
    }

    ProductDtos.TradableProduct getTradableProduct(long productId);
    void markSold(long productId, long expectedVersion);
}
