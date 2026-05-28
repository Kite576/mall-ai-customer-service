package com.example.mall.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTracking, Long> {

    List<ShipmentTracking> findByOrderIdOrderByOccurTimeDesc(Long orderId);
}
