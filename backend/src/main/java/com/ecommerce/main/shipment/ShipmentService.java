package com.ecommerce.main.shipment;

import com.ecommerce.main.order.Order;
import com.ecommerce.main.order.OrderRepository;
import com.ecommerce.main.order.OrderStatus;
import com.ecommerce.main.store.StoreRepository;
import com.ecommerce.main.user.Role;
import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /** Corporate/Admin: sipariş için gönderi oluşturur */
    @Transactional
    public ShipmentResponse create(String email, ShipmentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderId()));

        assertStoreAccess(order, email);

        if (shipmentRepository.findByOrderId(order.getId()).isPresent()) {
            throw new IllegalStateException("Shipment already exists for order: " + order.getId());
        }

        Shipment shipment = Shipment.builder()
                .order(order)
                .trackingNumber(generateTrackingNumber())
                .carrier(request.getCarrier())
                .warehouseBlock(request.getWarehouseBlock())
                .modeOfShipment(request.getModeOfShipment())
                .estimatedDelivery(request.getEstimatedDelivery())
                .customerCareCalls(request.getCustomerCareCalls())
                .customerRating(request.getCustomerRating())
                .costOfProduct(request.getCostOfProduct())
                .priorPurchases(request.getPriorPurchases())
                .discountOffered(request.getDiscountOffered())
                .status(ShipmentStatus.PROCESSING)
                .build();

        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);

        return ShipmentResponse.from(shipmentRepository.save(shipment));
    }

    /** Tracking numarasıyla sorgula — herkese açık */
    @Transactional(readOnly = true)
    public ShipmentResponse getByTracking(String trackingNumber) {
        return ShipmentResponse.from(
                shipmentRepository.findByTrackingNumber(trackingNumber)
                        .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + trackingNumber))
        );
    }

    /** Order ID ile sorgula — ilgili taraflara açık */
    @Transactional(readOnly = true)
    public ShipmentResponse getByOrderId(Long orderId, String email) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found for order: " + orderId));
        assertReadAccess(shipment.getOrder(), email);
        return ShipmentResponse.from(shipment);
    }

    /** Individual: kendi siparişlerinin gönderilerini listeler */
    @Transactional(readOnly = true)
    public Page<ShipmentResponse> getMyShipments(String email, Pageable pageable) {
        User user = findUser(email);
        return shipmentRepository.findByOrderUserId(user.getId(), pageable)
                .map(ShipmentResponse::from);
    }

    /** Corporate/Admin: store'a ait gönderileri listeler */
    @Transactional(readOnly = true)
    public Page<ShipmentResponse> getStoreShipments(Long storeId, String email, Pageable pageable) {
        assertStoreOwnerOrAdmin(storeId, email);
        return shipmentRepository.findByOrderStoreId(storeId, pageable)
                .map(ShipmentResponse::from);
    }

    /** Corporate/Admin: gönderi durumunu günceller */
    @Transactional
    public ShipmentResponse updateStatus(Long id, ShipmentStatus newStatus, String email) {
        Shipment shipment = findShipment(id);
        assertStoreAccess(shipment.getOrder(), email);

        shipment.setStatus(newStatus);

        if (newStatus == ShipmentStatus.DELIVERED) {
            shipment.setDeliveredAt(LocalDateTime.now());
            shipment.getOrder().setStatus(OrderStatus.DELIVERED);
            orderRepository.save(shipment.getOrder());
        }

        return ShipmentResponse.from(shipmentRepository.save(shipment));
    }

    /** Corporate/Admin: konum günceller, WebSocket ile broadcast eder */
    @Transactional
    public ShipmentResponse updateLocation(Long id, Double latitude, Double longitude, String email) {
        Shipment shipment = findShipment(id);
        assertStoreAccess(shipment.getOrder(), email);

        shipment.setLatitude(latitude);
        shipment.setLongitude(longitude);
        shipment.setLastLocationUpdate(LocalDateTime.now());

        ShipmentResponse response = ShipmentResponse.from(shipmentRepository.save(shipment));
        messagingTemplate.convertAndSend("/topic/shipment/" + id, response);
        return response;
    }

    private String generateTrackingNumber() {
        return "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Shipment findShipment(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + id));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    private void assertStoreAccess(Order order, String email) {
        User user = findUser(email);
        if (user.getRoleType() == Role.ADMIN) return;
        if (!order.getStore().getOwner().getEmail().equals(email)) {
            throw new IllegalStateException("Access denied");
        }
    }

    private void assertReadAccess(Order order, String email) {
        User user = findUser(email);
        if (user.getRoleType() == Role.ADMIN) return;
        if (order.getUser().getEmail().equals(email)) return;
        if (order.getStore().getOwner().getEmail().equals(email)) return;
        throw new IllegalStateException("Access denied");
    }

    private void assertStoreOwnerOrAdmin(Long storeId, String email) {
        User user = findUser(email);
        if (user.getRoleType() == Role.ADMIN) return;
        storeRepository.findById(storeId).ifPresentOrElse(
                store -> {
                    if (!store.getOwner().getEmail().equals(email)) {
                        throw new IllegalStateException("Access denied");
                    }
                },
                () -> { throw new IllegalArgumentException("Store not found: " + storeId); }
        );
    }
}
