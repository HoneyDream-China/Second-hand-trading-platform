package edu.campus.loop.product;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class ProductDtos {
    private ProductDtos() { }
    public enum ProductStatus { DRAFT, ON_SALE, SOLD, OFF_SHELF }
    public record CreateProductRequest(@NotBlank @Size(max=120) String title,
            @NotBlank String description, @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal price,
            @NotBlank @Size(max=64) String category, @NotBlank @Size(max=32) String condition,
            @NotNull List<@NotBlank @Size(max=512) String> imageUrls) { }
    public record UpdateProductRequest(@NotBlank @Size(max=120) String title,
            @NotBlank String description, @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal price,
            @NotBlank @Size(max=64) String category, @NotBlank @Size(max=32) String condition,
            @NotNull List<@NotBlank @Size(max=512) String> imageUrls, @NotNull @Min(0) Long version) { }
    public record StatusRequest(@NotNull ProductStatus status, @NotNull @Min(0) Long version) { }
    public record ProductView(long id, long sellerId, String sellerName, String title,
            String description, BigDecimal price, String category, String condition,
            List<String> imageUrls, ProductStatus status, long version,
            OffsetDateTime createdAt, OffsetDateTime updatedAt) { }
    public record TradableProduct(long id, long sellerId, BigDecimal price, long version) { }
}
