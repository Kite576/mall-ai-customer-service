package com.example.mall.cart;

import com.example.mall.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/api/cart")
    public ApiResponse<List<CartItem>> list() {
        return ApiResponse.success(cartService.list());
    }

    @PostMapping("/api/cart/items")
    public ApiResponse<CartItem> add(@Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success(cartService.add(request));
    }

    @PutMapping("/api/cart/items/{cartItemId}")
    public ApiResponse<CartItem> update(@PathVariable Long cartItemId, @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(cartService.update(cartItemId, request));
    }

    @DeleteMapping("/api/cart/items/{cartItemId}")
    public ApiResponse<Void> delete(@PathVariable Long cartItemId) {
        cartService.delete(cartItemId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/api/cart")
    public ApiResponse<Void> clear() {
        cartService.clear();
        return ApiResponse.success(null);
    }
}
