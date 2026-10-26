package edu.campus.loop.product;

import edu.campus.loop.common.BusinessNotImplementedException;
import edu.campus.loop.common.PageView;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import static edu.campus.loop.product.ProductDtos.*;

@Service
public class ProductService implements ProductFacade {
    // TODO 成员 B：UserFacade 活跃卖家校验、所有权校验、版本控制、合法状态迁移及分页查询。
    public ProductView create(CreateProductRequest request) { throw new BusinessNotImplementedException(); }
    public PageView<ProductView> list(int page, int size, String keyword, String category,
            BigDecimal minPrice, BigDecimal maxPrice, String condition, ProductStatus status) {
        throw new BusinessNotImplementedException();
    }
    public ProductView detail(long id) { throw new BusinessNotImplementedException(); }
    public ProductView update(long id, UpdateProductRequest request) { throw new BusinessNotImplementedException(); }
    public ProductView changeStatus(long id, StatusRequest request) { throw new BusinessNotImplementedException(); }
    public TradableProduct getTradableProduct(long productId) { throw new BusinessNotImplementedException(); }
    public void markSold(long productId, long expectedVersion) { throw new BusinessNotImplementedException(); }
}
