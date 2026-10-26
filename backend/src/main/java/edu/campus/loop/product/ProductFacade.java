package edu.campus.loop.product;

/** 交易模块的唯一商品写入边界。 */
public interface ProductFacade {
    ProductDtos.TradableProduct getTradableProduct(long productId);
    void markSold(long productId, long expectedVersion);
}
