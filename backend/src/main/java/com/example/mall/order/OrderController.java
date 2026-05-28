package com.example.mall.order;

import com.example.mall.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/api/orders")
    public ApiResponse<Order> create(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.create(request));
    }

    @GetMapping("/api/orders")
    public ApiResponse<List<Order>> list(@RequestParam(required = false) OrderStatus status) {
        return ApiResponse.success(orderService.list(status));
    }

    @GetMapping("/api/orders/{orderId}")
    public ApiResponse<Order> detail(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.detail(orderId));
    }

    @PutMapping("/api/orders/{orderId}/pay")
    public ApiResponse<Order> pay(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.pay(orderId));
    }

    @PutMapping("/api/orders/{orderId}/cancel")
    public ApiResponse<Order> cancel(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.cancel(orderId));
    }

    @GetMapping("/api/orders/{orderId}/tracking")
    public ApiResponse<List<ShipmentTracking>> tracking(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.tracking(orderId));
    }
}
