package edu.campus.loop.product;

import edu.campus.loop.product.ProductDtos.ProductStatus;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/**
 * 只访问商品域的 products / product_images 两张表。
 * <p>本类不开启事务：Service 的写操作和交易模块的 accept 流程必须在同一线程绑定事务中调用，
 * 这样 {@link #lock(long)} 与 {@link #markSold(long, long, OffsetDateTime)} 才能和交易记录一起提交或回滚。</p>
 */
public class ProductRepository {
    /** 商品行快照；只在商品域内部使用，对外统一转换为 ProductView。 */
    public record StoredProduct(long id, long sellerId, String title, String description, BigDecimal price,
            String category, String condition, ProductStatus status, long version,
            OffsetDateTime createdAt, OffsetDateTime updatedAt) { }

    /** 列表筛选条件，null 表示该条件不参与过滤。 */
    public record Filter(String keyword, String category, BigDecimal minPrice, BigDecimal maxPrice,
            String condition, ProductStatus status) { }

    private record Where(String sql, List<Object> arguments) { }

    private static final RowMapper<StoredProduct> MAPPER = (rows, row) -> new StoredProduct(
            rows.getLong("id"), rows.getLong("seller_id"), rows.getString("title"),
            rows.getString("description"), rows.getBigDecimal("price"), rows.getString("category"),
            rows.getString("condition"), ProductStatus.valueOf(rows.getString("status")),
            rows.getLong("version"), rows.getObject("created_at", OffsetDateTime.class),
            rows.getObject("updated_at", OffsetDateTime.class));

    private final JdbcTemplate jdbc;

    public ProductRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** 插入商品主记录，初始状态固定为 DRAFT、版本为 0；ID 由数据库生成。 */
    public StoredProduct insert(long sellerId, String title, String description, BigDecimal price,
            String category, String condition, OffsetDateTime now) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO products(seller_id, title, description, price, category, condition,
                                         status, version, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', 0, ?, ?)
                    """, new String[] {"id"});
            statement.setLong(1, sellerId);
            statement.setString(2, title);
            statement.setString(3, description);
            statement.setBigDecimal(4, price);
            statement.setString(5, category);
            statement.setString(6, condition);
            statement.setObject(7, now);
            statement.setObject(8, now);
            return statement;
        }, key);
        Long id = key.getKey() == null ? null : key.getKey().longValue();
        if (id == null) throw new IllegalStateException("Missing generated product id");
        return find(id);
    }

    /** 按输入顺序写入图片，sort_order 从 0 开始，保证同一商品内唯一。 */
    public void insertImages(long productId, List<String> urls, OffsetDateTime now) {
        if (urls == null || urls.isEmpty()) return;
        List<Object[]> batch = new ArrayList<>(urls.size());
        for (int index = 0; index < urls.size(); index++) {
            batch.add(new Object[] {productId, urls.get(index), index, now});
        }
        jdbc.batchUpdate("""
                INSERT INTO product_images(product_id, url, sort_order, created_at) VALUES (?, ?, ?, ?)
                """, batch);
    }

    /** 原子替换图片集合；先删后插，避免旧顺序与唯一索引冲突。 */
    public void replaceImages(long productId, List<String> urls, OffsetDateTime now) {
        jdbc.update("DELETE FROM product_images WHERE product_id = ?", productId);
        insertImages(productId, urls, now);
    }

    public StoredProduct find(long id) {
        List<StoredProduct> rows = jdbc.query("SELECT * FROM products WHERE id = ?", MAPPER, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    /**
     * 在调用方事务中锁定商品行，必须先于交易行加锁。
     * @return 商品存在返回 true；不存在返回 false，由调用方决定错误码
     */
    public boolean lock(long id) {
        List<Long> rows = jdbc.query("SELECT id FROM products WHERE id = ? FOR UPDATE",
                (rows_, row) -> rows_.getLong(1), id);
        return !rows.isEmpty();
    }

    /** 分页查询；排序固定为创建时间倒序、ID 倒序，保证翻页稳定。 */
    public List<StoredProduct> list(Filter filter, int size, long offset) {
        Where where = where(filter);
        List<Object> arguments = new ArrayList<>(where.arguments());
        arguments.add(size);
        arguments.add(offset);
        return jdbc.query("SELECT * FROM products" + where.sql()
                + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?", MAPPER, arguments.toArray());
    }

    public long count(Filter filter) {
        Where where = where(filter);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM products" + where.sql(),
                Long.class, where.arguments().toArray());
        return total == null ? 0 : total;
    }

    /** 详情或编辑后读取单商品图片，按 sort_order 升序。 */
    public List<String> images(long productId) {
        return jdbc.query("""
                SELECT url FROM product_images WHERE product_id = ? ORDER BY sort_order, id
                """, (rows, row) -> rows.getString("url"), productId);
    }

    /** 列表批量加载图片，避免逐商品查询造成 N+1。 */
    public Map<Long, List<String>> images(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Map.of();
        List<Long> ids = List.copyOf(productIds);
        String placeholders = String.join(", ", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT product_id, url FROM product_images WHERE product_id IN ("
                + placeholders + ") ORDER BY product_id, sort_order, id";
        Map<Long, List<String>> result = jdbc.query(sql,
                (ResultSetExtractor<Map<Long, List<String>>>) rows -> {
                    Map<Long, List<String>> images = new LinkedHashMap<>();
                    while (rows.next()) {
                        images.computeIfAbsent(rows.getLong("product_id"), key -> new ArrayList<>())
                                .add(rows.getString("url"));
                    }
                    return images;
                }, ids.toArray());
        return result == null ? Map.of() : result;
    }

    /**
     * 乐观锁更新核心字段。WHERE 同时限制卖家、版本和未售状态，
     * 受影响行数为 0 时由 Service 查询最新状态以区分不存在、越权、已售和版本冲突。
     */
    public int update(long id, long sellerId, long expectedVersion, String title, String description,
            BigDecimal price, String category, String condition, OffsetDateTime now) {
        return jdbc.update("""
                UPDATE products
                SET title = ?, description = ?, price = ?, category = ?, condition = ?,
                    version = version + 1, updated_at = ?
                WHERE id = ? AND seller_id = ? AND version = ? AND status <> 'SOLD'
                """, title, description, price, category, condition, now, id, sellerId, expectedVersion);
    }

    /** 状态迁移的乐观锁更新；要求当前状态仍为 expectedStatus。 */
    public int updateStatus(long id, long sellerId, long expectedVersion, ProductStatus expectedStatus,
            ProductStatus target, OffsetDateTime now) {
        return jdbc.update("""
                UPDATE products
                SET status = ?, version = version + 1, updated_at = ?
                WHERE id = ? AND seller_id = ? AND version = ? AND status = ?
                """, target.name(), now, id, sellerId, expectedVersion, expectedStatus.name());
    }

    /** 交易成交专用更新：只在售商品可售出，并校验期望版本。 */
    public int markSold(long id, long expectedVersion, OffsetDateTime now) {
        return jdbc.update("""
                UPDATE products
                SET status = 'SOLD', version = version + 1, updated_at = ?
                WHERE id = ? AND version = ? AND status = 'ON_SALE'
                """, now, id, expectedVersion);
    }

    private Where where(Filter filter) {
        StringBuilder sql = new StringBuilder(" WHERE 1 = 1");
        List<Object> arguments = new ArrayList<>();
        if (filter != null) {
            if (filter.keyword() != null) {
                sql.append(" AND (LOWER(title) LIKE ? OR LOWER(description) LIKE ?)");
                String pattern = "%" + filter.keyword().toLowerCase(java.util.Locale.ROOT) + "%";
                arguments.add(pattern);
                arguments.add(pattern);
            }
            if (filter.category() != null) {
                sql.append(" AND category = ?");
                arguments.add(filter.category());
            }
            if (filter.minPrice() != null) {
                sql.append(" AND price >= ?");
                arguments.add(filter.minPrice());
            }
            if (filter.maxPrice() != null) {
                sql.append(" AND price <= ?");
                arguments.add(filter.maxPrice());
            }
            if (filter.condition() != null) {
                sql.append(" AND condition = ?");
                arguments.add(filter.condition());
            }
            if (filter.status() != null) {
                sql.append(" AND status = ?");
                arguments.add(filter.status().name());
            }
        }
        return new Where(sql.toString(), arguments);
    }
}
