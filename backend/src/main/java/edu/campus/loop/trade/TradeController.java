package edu.campus.loop.trade;

import edu.campus.loop.common.ApiResponse;
import edu.campus.loop.common.PageView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import static edu.campus.loop.trade.TradeDtos.*;

@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {
    private final TradeService service;
    public TradeController(TradeService service) { this.service = service; }
    @PostMapping
    public ApiResponse<TradeView> create(@Valid @RequestBody CreateTradeRequest request) {
        return new ApiResponse<>("OK", "success", service.create(request), null);
    }
    @GetMapping
    public ApiResponse<PageView<TradeView>> list(@RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            @RequestParam(required=false) TradeStatus status) {
        return new ApiResponse<>("OK", "success", service.list(page,size,status), null);
    }
    @GetMapping("/{id}")
    public ApiResponse<TradeView> detail(@PathVariable long id) { return new ApiResponse<>("OK", "success", service.detail(id), null); }
    @PatchMapping("/{id}/accept")
    public ApiResponse<TradeView> accept(@PathVariable long id) { return new ApiResponse<>("OK", "success", service.accept(id), null); }
    @PatchMapping("/{id}/reject")
    public ApiResponse<TradeView> reject(@PathVariable long id) { return new ApiResponse<>("OK", "success", service.reject(id), null); }
    @PatchMapping("/{id}/cancel")
    public ApiResponse<TradeView> cancel(@PathVariable long id) { return new ApiResponse<>("OK", "success", service.cancel(id), null); }
}
