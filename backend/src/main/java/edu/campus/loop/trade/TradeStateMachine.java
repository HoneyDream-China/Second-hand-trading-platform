package edu.campus.loop.trade;

import edu.campus.loop.common.BusinessException;
import org.springframework.http.HttpStatus;
import static edu.campus.loop.trade.TradeDtos.TradeStatus;

/** 集中定义状态迁移；同一终态的幂等重试在服务层提前返回。 */
final class TradeStateMachine {
    private TradeStateMachine() { }

    static void requireTransition(TradeStatus from, TradeStatus to) {
        if (from != TradeStatus.PENDING || to == TradeStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_TRADE_TRANSITION", "当前交易状态不允许该操作");
        }
    }
}
