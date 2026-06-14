package com.example.mall.order;

import com.example.mall.cart.CartItem;
import com.example.mall.cart.CartItemRepository;
import com.example.mall.cart.CartService;
import com.example.mall.product.Product;
import com.example.mall.product.ProductRepository;
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
    private final ProductRepository productRepository;

    public OrderService(CartService cartService,
                        CartItemRepository cartItemRepository,
                        OrderRepository orderRepository,
                        ShipmentTrackingRepository shipmentTrackingRepository,
                        ProductRepository productRepository) {
        this.cartService = cartService;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.shipmentTrackingRepository = shipmentTrackingRepository;
        this.productRepository = productRepository;
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
            Product product = cartItem.getProduct();
            if (!Integer.valueOf(1).equals(product.getStatus())) {
                throw new IllegalArgumentException(product.getName() + " 已下架");
            }
            if (product.getStock() < cartItem.getQuantity()) {
                throw new IllegalArgumentException(product.getName() + " 库存不足");
            }
            product.setStock(product.getStock() - cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        addTracking(savedOrder.getId(), "CREATED", "订单已创建，等待支付");
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
        Order savedOrder = orderRepository.save(order);
        addTracking(savedOrder.getId(), "PAID", "支付成功，等待仓库处理");
        return savedOrder;
    }

    @Transactional
    public Order cancel(Long orderId) {
        Order order = detail(orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
            throw new IllegalArgumentException("当前订单状态不支持取消");
        }
        restoreStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        addTracking(savedOrder.getId(), "CANCELLED", "订单已取消，库存已释放");
        return savedOrder;
    }

    @Transactional
    public Order confirm(Long orderId) {
        Order order = detail(orderId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalArgumentException("当前订单状态不支持确认收货");
        }
        order.setStatus(OrderStatus.COMPLETED);
        Order savedOrder = orderRepository.save(order);
        addTracking(savedOrder.getId(), "COMPLETED", "用户已确认收货，订单完成");
        return savedOrder;
    }

    public List<ShipmentTracking> tracking(Long orderId) {
        return shipmentTrackingRepository.findByOrderIdOrderByOccurTimeDesc(orderId);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
            product.setStock(product.getStock() + item.getQuantity());
        }
    }

    private void addTracking(Long orderId, String status, String description) {
        ShipmentTracking tracking = new ShipmentTracking();
        tracking.setOrderId(orderId);
        tracking.setStatus(status);
        tracking.setDescription(description);
        tracking.setOccurTime(LocalDateTime.now());
        shipmentTrackingRepository.save(tracking);
    }
}
