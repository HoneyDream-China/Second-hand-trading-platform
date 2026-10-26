package edu.campus.loop.trade;

import edu.campus.loop.common.BusinessNotImplementedException;
import edu.campus.loop.common.PageView;
import org.springframework.stereotype.Service;
import static edu.campus.loop.trade.TradeDtos.*;

@Service
public class TradeService {
    // TODO 成员 C：只通过 UserFacade / ProductFacade 校验、读取和变更商品。
    // TODO 接受交易：同一事务，先锁商品后锁交易；markSold、交易状态、日志必须原子提交。
    // TODO 创建通过数据库部分唯一索引防重；所有状态操作校验操作者、幂等和合法迁移。
    public TradeView create(CreateTradeRequest request) { throw new BusinessNotImplementedException(); }
    public PageView<TradeView> list(int page, int size, TradeStatus status) { throw new BusinessNotImplementedException(); }
    public TradeView detail(long id) { throw new BusinessNotImplementedException(); }
    public TradeView accept(long id) { throw new BusinessNotImplementedException(); }
    public TradeView reject(long id) { throw new BusinessNotImplementedException(); }
    public TradeView cancel(long id) { throw new BusinessNotImplementedException(); }
}
