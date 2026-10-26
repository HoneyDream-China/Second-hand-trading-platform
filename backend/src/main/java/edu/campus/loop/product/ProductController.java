package edu.campus.loop.product;

import edu.campus.loop.common.ApiResponse;
import edu.campus.loop.common.PageView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import static edu.campus.loop.product.ProductDtos.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }
    @PostMapping
    public ApiResponse<ProductView> create(@Valid @RequestBody CreateProductRequest request) {
        return new ApiResponse<>("OK", "success", service.create(request), null);
    }
    @GetMapping
    public ApiResponse<PageView<ProductView>> list(@RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            @RequestParam(required=false) String keyword, @RequestParam(required=false) String category,
            @RequestParam(required=false) @DecimalMin("0") BigDecimal minPrice,
            @RequestParam(required=false) @DecimalMin("0") BigDecimal maxPrice,
            @RequestParam(required=false) String condition, @RequestParam(required=false) ProductStatus status) {
        return new ApiResponse<>("OK", "success", service.list(page,size,keyword,category,minPrice,maxPrice,condition,status), null);
    }
    @GetMapping("/{id}")
    public ApiResponse<ProductView> detail(@PathVariable long id) { return new ApiResponse<>("OK", "success", service.detail(id), null); }
    @PutMapping("/{id}")
    public ApiResponse<ProductView> update(@PathVariable long id, @Valid @RequestBody UpdateProductRequest request) {
        return new ApiResponse<>("OK", "success", service.update(id,request), null);
    }
    @PatchMapping("/{id}/status")
    public ApiResponse<ProductView> status(@PathVariable long id, @Valid @RequestBody StatusRequest request) {
        return new ApiResponse<>("OK", "success", service.changeStatus(id,request), null);
    }
}
