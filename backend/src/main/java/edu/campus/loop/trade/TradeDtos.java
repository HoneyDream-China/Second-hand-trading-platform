package edu.campus.loop.trade;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;

public final class TradeDtos {
    private TradeDtos() { }
    public enum TradeStatus { PENDING, ACCEPTED, REJECTED, CANCELLED }
    public record CreateTradeRequest(@NotNull @Positive Long productId, @Size(max=500) String message) { }
    public record StatusLog(TradeStatus status, OffsetDateTime at) { }
    public record TradeView(long id, long productId, long buyerId, long sellerId, String message,
            TradeStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt, List<StatusLog> logs) { }
}
