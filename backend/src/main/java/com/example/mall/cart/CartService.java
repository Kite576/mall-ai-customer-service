package com.example.mall.cart;

import com.example.mall.product.Product;
import com.example.mall.product.ProductRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final Long demoUserId;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(@Value("${mall.demo-user-id}") Long demoUserId,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository) {
        this.demoUserId = demoUserId;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    public List<CartItem> list() {
        return cartItemRepository.findByUserId(demoUserId);
    }

    @Transactional
    public CartItem add(AddCartItemRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (product.getStock() < request.quantity()) {
            throw new IllegalArgumentException("商品库存不足");
        }
        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(demoUserId, request.productId())
                .orElseGet(CartItem::new);
        cartItem.setUserId(demoUserId);
        cartItem.setProduct(product);
        cartItem.setQuantity((cartItem.getQuantity() == null ? 0 : cartItem.getQuantity()) + request.quantity());
        cartItem.setSelected(true);
        return cartItemRepository.save(cartItem);
    }

    @Transactional
    public CartItem update(Long cartItemId, UpdateCartItemRequest request) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("购物车商品不存在"));
        cartItem.setQuantity(request.quantity());
        cartItem.setSelected(request.selected() == null || request.selected());
        return cartItemRepository.save(cartItem);
    }

    @Transactional
    public void delete(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    @Transactional
    public void clear() {
        cartItemRepository.deleteByUserId(demoUserId);
    }

    public Long demoUserId() {
        return demoUserId;
    }
}
