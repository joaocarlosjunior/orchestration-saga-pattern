package com.joaocarlos.delivery_service.repository;

import com.joaocarlos.delivery_service.domain.Delivery;
import com.joaocarlos.delivery_service.domain.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, String> {

    List<Delivery> findByStatus(DeliveryStatus status);
}
