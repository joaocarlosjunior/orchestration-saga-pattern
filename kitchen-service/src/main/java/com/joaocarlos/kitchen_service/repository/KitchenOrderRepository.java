package com.joaocarlos.kitchen_service.repository;

import com.joaocarlos.kitchen_service.domain.KitchenOrder;
import com.joaocarlos.kitchen_service.domain.KitchenOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitchenOrderRepository extends JpaRepository<KitchenOrder, String> {

    List<KitchenOrder> findByStatus(KitchenOrderStatus status);
}
