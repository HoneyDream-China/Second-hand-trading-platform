package edu.campus.loop.trade;

import edu.campus.loop.common.BusinessException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import static edu.campus.loop.trade.TradeDtos.*;

/** 仅访问交易域的两张表；事务边界由 TradeService 负责。 */
public class TradeRepository {
    private final JdbcTemplate jdbc;
    private static final RowMapper<TradeView> MAPPER = (rs, row) -> new TradeView(
            rs.getLong("id"), rs.getLong("product_id"), rs.getLong("buyer_id"),
            rs.getLong("seller_id"), rs.getString("message"), TradeStatus.valueOf(rs.getString("status")),
            rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class), List.of());

    public TradeRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** 创建主记录，ID 由数据库生成；唯一索引负责并发防重。 */
    public long insert(CreateTradeRequest request, long buyerId, long sellerId, OffsetDateTime now) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO trades(product_id, buyer_id, seller_id, message, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'PENDING', ?, ?)
                """, new String[] {"id"});
            statement.setLong(1, request.productId());
            statement.setLong(2, buyerId);
            statement.setLong(3, sellerId);
            statement.setString(4, request.message());
            statement.setObject(5, now);
            statement.setObject(6, now);
            return statement;
        }, key);
        return java.util.Objects.requireNonNull(key.getKey()).longValue();
    }

    /** 可选行锁；不存在时统一返回资源不存在错误。 */
    public TradeView find(long id, boolean lock) {
        List<TradeView> rows = jdbc.query("SELECT * FROM trades WHERE id = ?" + (lock ? " FOR UPDATE" : ""), MAPPER, id);
        if (rows.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "交易不存在");
        }
        return rows.getFirst();
    }

    /** 按当前参与者和可选状态查询，按创建时间及 ID 倒序保持分页顺序稳定。 */
    public List<TradeView> list(long userId, int page, int size, TradeStatus status) {
        List<Object> args = arguments(userId, status);
        args.add(size);
        args.add(((long) page - 1) * size);
        return jdbc.query("SELECT * FROM trades" + filter(status)
                + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?", MAPPER, args.toArray());
    }

    public long count(long userId, TradeStatus status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM trades" + filter(status), Long.class,
                arguments(userId, status).toArray());
    }

    /** 状态更新后追加日志，禁止覆盖历史记录。 */
    public void updateStatus(long id, TradeStatus status, OffsetDateTime now) {
        jdbc.update("UPDATE trades SET status = ?, updated_at = ? WHERE id = ?", status.name(), now, id);
    }

    public void appendLog(long id, TradeStatus from, TradeStatus to, long operatorId, OffsetDateTime now) {
        jdbc.update("""
                INSERT INTO trade_status_logs(trade_id, from_status, to_status, operator_id, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, id, from == null ? null : from.name(), to.name(), operatorId, now);
    }

    public TradeView withLogs(TradeView trade) {
        List<StatusLog> logs = jdbc.query("""
                SELECT to_status, created_at FROM trade_status_logs
                WHERE trade_id = ? ORDER BY created_at, id
                """, (rs, row) -> new StatusLog(TradeStatus.valueOf(rs.getString("to_status")),
                rs.getObject("created_at", OffsetDateTime.class)), trade.id());
        return new TradeView(trade.id(), trade.productId(), trade.buyerId(), trade.sellerId(), trade.message(),
                trade.status(), trade.createdAt(), trade.updatedAt(), List.copyOf(logs));
    }

    private String filter(TradeStatus status) {
        return " WHERE (buyer_id = ? OR seller_id = ?)" + (status == null ? "" : " AND status = ?");
    }

    private List<Object> arguments(long userId, TradeStatus status) {
        List<Object> args = new ArrayList<>(List.of(userId, userId));
        if (status != null) args.add(status.name());
        return args;
    }
}
