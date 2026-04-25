package com.ecommerce.main.order;

import com.ecommerce.main.product.Product;
import com.ecommerce.main.product.ProductRepository;
import com.ecommerce.main.store.Store;
import com.ecommerce.main.store.StoreRepository;
import com.ecommerce.main.user.Role;
import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(String email, OrderRequest request) {
        User user = findUser(email);
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + request.getStoreId()));

        Order order = Order.builder()
                .user(user)
                .store(store)
                .paymentMethod(request.getPaymentMethod())
                .shippingAddress(request.getShippingAddress())
                .fulfilment(request.getFulfilment())
                .salesChannel(request.getSalesChannel())
                .shipServiceLevel(request.getShipServiceLevel())
                .incrementId(request.getIncrementId())
                .status(OrderStatus.PENDING)
                .grandTotal(0.0)
                .build();

        double total = 0.0;
        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemReq.getProductId()));

            if (!product.getStore().getId().equals(store.getId())) {
                throw new IllegalArgumentException("Product " + product.getId() + " does not belong to this store");
            }
            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName());
            }

            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            productRepository.save(product);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getUnitPrice())
                    .build();
            order.getItems().add(item);
            total += itemReq.getQuantity() * product.getUnitPrice();
        }

        order.setGrandTotal(total);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id, String email) {
        Order order = findOrder(id);
        assertAccess(order, email);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(String email, OrderStatus status, Pageable pageable) {
        Page<Order> orders = (status != null)
                ? orderRepository.findByUserEmailAndStatus(email, status, pageable)
                : orderRepository.findByUserEmail(email, pageable);
        return orders.map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getStoreOrders(Long storeId, String email, OrderStatus status, Pageable pageable) {
        assertStoreAccess(storeId, email);
        Page<Order> orders = (status != null)
                ? orderRepository.findByStoreIdAndStatus(storeId, status, pageable)
                : orderRepository.findByStoreId(storeId, pageable);
        return orders.map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus, String email) {
        Order order = findOrder(id);
        User requestor = findUser(email);

        boolean isAdmin = requestor.getRoleType() == Role.ADMIN;
        boolean isCorporate = requestor.getRoleType() == Role.CORPORATE
                && order.getStore().getOwner().getEmail().equals(email);

        if (!isAdmin && !isCorporate) {
            throw new IllegalStateException("Access denied");
        }

        if (newStatus == OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        order.setStatus(newStatus);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(Long id, String email) {
        Order order = findOrder(id);

        if (!order.getUser().getEmail().equals(email)) {
            throw new IllegalStateException("Access denied");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be cancelled");
        }

        restoreStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public byte[] exportMyOrdersCsv(String email) {
        List<Order> orders = orderRepository.findAllByUserEmail(email);
        StringBuilder sb = new StringBuilder();
        sb.append("OrderID,Date,Status,GrandTotal,PaymentMethod,ShippingAddress,Items\n");
        for (Order o : orders) {
            String items = o.getItems().stream()
                    .map(i -> i.getProduct().getSku() + "x" + i.getQuantity())
                    .collect(java.util.stream.Collectors.joining("|"));
            sb.append(o.getId()).append(",")
              .append(o.getCreatedAt()).append(",")
              .append(o.getStatus()).append(",")
              .append(o.getGrandTotal()).append(",")
              .append(csvEscape(o.getPaymentMethod())).append(",")
              .append(csvEscape(o.getShippingAddress())).append(",")
              .append(csvEscape(items)).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    private void assertAccess(Order order, String email) {
        User requestor = findUser(email);
        boolean isAdmin = requestor.getRoleType() == Role.ADMIN;
        boolean isOwner = order.getUser().getEmail().equals(email);
        boolean isStoreOwner = order.getStore().getOwner().getEmail().equals(email);
        if (!isAdmin && !isOwner && !isStoreOwner) {
            throw new IllegalStateException("Access denied");
        }
    }

    private void assertStoreAccess(Long storeId, String email) {
        User requestor = findUser(email);
        if (requestor.getRoleType() == Role.ADMIN) return;
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        if (!store.getOwner().getEmail().equals(email)) {
            throw new IllegalStateException("Access denied");
        }
    }
}
