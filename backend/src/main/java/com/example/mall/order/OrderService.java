package com.example.mall.order;

import com.example.mall.cart.CartItem;
import com.example.mall.cart.CartItemRepository;
import com.example.mall.cart.CartService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final CartService cartService;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final ShipmentTrackingRepository shipmentTrackingRepository;

    public OrderService(CartService cartService,
                        CartItemRepository cartItemRepository,
                        OrderRepository orderRepository,
                        ShipmentTrackingRepository shipmentTrackingRepository) {
        this.cartService = cartService;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.shipmentTrackingRepository = shipmentTrackingRepository;
    }

    @Transactional
    public Order create(CreateOrderRequest request) {
        List<CartItem> selectedItems = cartItemRepository.findByUserIdAndSelected(cartService.demoUserId(), true);
        if (selectedItems.isEmpty()) {
            throw new IllegalArgumentException("请先选择购物车商品");
        }
        Order order = new Order();
        order.setOrderNo("MO" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()));
        order.setUserId(cartService.demoUserId());
        order.setReceiverName(request.receiverName());
        order.setReceiverPhone(request.receiverPhone());
        order.setReceiverAddress(request.receiverAddress());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(LocalDateTime.now());
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : selectedItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProduct().getId());
            orderItem.setProductName(cartItem.getProduct().getName());
            orderItem.setPrice(cartItem.getProduct().getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(cartItem.getProduct().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        selectedItems.forEach(cartItemRepository::delete);
        return savedOrder;
    }

    public List<Order> list(OrderStatus status) {
        if (status == null) {
            return orderRepository.findByUserIdOrderByCreatedAtDesc(cartService.demoUserId());
        }
        return orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(cartService.demoUserId(), status);
    }

    public Order detail(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
    }

    public Order detailByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
    }

    @Transactional
    public Order pay(Long orderId) {
        Order order = detail(orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("当前订单状态不支持支付");
        }
        order.setStatus(OrderStatus.PENDING_SHIPMENT);
        order.setPaidAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancel(Long orderId) {
        Order order = detail(orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
            throw new IllegalArgumentException("当前订单状态不支持取消");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    public List<ShipmentTracking> tracking(Long orderId) {
        return shipmentTrackingRepository.findByOrderIdOrderByOccurTimeDesc(orderId);
    }
}
