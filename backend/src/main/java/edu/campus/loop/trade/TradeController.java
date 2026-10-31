package edu.campus.loop.trade;

import edu.campus.loop.common.ApiResponse;
import edu.campus.loop.common.PageView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import static edu.campus.loop.trade.TradeDtos.*;

/**
 * 交易 HTTP 接口入口，统一路径为 /api/v1/trades。
 * <p>负责接收参数、触发请求体校验，并将服务层结果包装为 ApiResponse。
 * 身份与业务权限校验由后续业务实现负责；当前服务方法均为占位实现，
 * 会抛出 BusinessNotImplementedException，由统一异常处理返回 HTTP 501。</p>
 */
@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {
    private final TradeService service;

    /**
     * 由 Spring 注入交易业务服务，供各接口委托调用。
     * @param service 交易业务服务
     */
    public TradeController(TradeService service) { this.service = service; }

    /**
     * POST /api/v1/trades：登录用户向商品卖家发起交易申请。
     * <p>@Valid 触发 DTO 校验：商品 ID 必须为正数，留言最多 500 字符。
     * 按设计，买家身份取自当前登录用户，卖家身份取自商品，不能由客户端指定。</p>
     * @param request 商品 ID 与可选留言
     * @return 统一响应，data 为创建后的交易详情（预期初始状态为 PENDING）
     */
    @PostMapping
    public ApiResponse<TradeView> create(@Valid @RequestBody CreateTradeRequest request) {
        return new ApiResponse<>("OK", "success", service.create(request), null);
    }
    /**
     * GET /api/v1/trades：分页查询当前用户作为买家或卖家参与的交易。
     * <p>参数声明了页码、页大小范围；状态不传时不按状态筛选。
     * 用户范围过滤需由服务层实现。</p>
     * @param page 页码，从 1 开始，默认 1
     * @param size 每页条数，默认 20，声明范围为 1～100
     * @param status 可选状态：PENDING、ACCEPTED、REJECTED、CANCELLED
     * @return 统一响应，data 含交易列表、页码、页大小及总条数
     */
    @GetMapping
    public ApiResponse<PageView<TradeView>> list(@RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            @RequestParam(required=false) TradeStatus status) {
        return new ApiResponse<>("OK", "success", service.list(page,size,status), null);
    }
    /**
     * GET /api/v1/trades/{id}：查询单笔交易及其状态记录。
     * @param id 交易 ID；按设计仅交易买家或卖家可查看，权限校验待实现
     * @return 统一响应，data 为交易详情
     */
    @GetMapping("/{id}")
    public ApiResponse<TradeView> detail(@PathVariable long id) { return new ApiResponse<>("OK", "success", service.detail(id), null); }
    /**
     * PATCH /api/v1/trades/{id}/accept：卖家接受待处理交易。
     * <p>预期将交易从 PENDING 改为 ACCEPTED，同时将商品标记为已售并记录日志；
     * 这些变更需在服务层同一事务中完成，目前尚未实现。</p>
     * @param id 待接受的交易 ID
     * @return 统一响应，data 为操作后的交易详情
     */
    @PatchMapping("/{id}/accept")
    public ApiResponse<TradeView> accept(@PathVariable long id) { return new ApiResponse<>("OK", "success", service.accept(id), null); }
    /**
     * PATCH /api/v1/trades/{id}/reject：卖家拒绝待处理交易。
     * <p>预期将 PENDING 改为 REJECTED 并记录日志，不修改商品状态；规则待实现。</p>
     * @param id 待拒绝的交易 ID
     * @return 统一响应，data 为操作后的交易详情
     */
    @PatchMapping("/{id}/reject")
    public ApiResponse<TradeView> reject(@PathVariable long id) { return new ApiResponse<>("OK", "success", service.reject(id), null); }
    /**
     * PATCH /api/v1/trades/{id}/cancel：买家取消自己发起的待处理交易。
     * <p>预期将 PENDING 改为 CANCELLED 并记录日志，不修改商品状态；规则待实现。</p>
     * @param id 待取消的交易 ID
     * @return 统一响应，data 为操作后的交易详情
     */
    @PatchMapping("/{id}/cancel")
    public ApiResponse<TradeView> cancel(@PathVariable long id) { return new ApiResponse<>("OK", "success", service.cancel(id), null); }
}
