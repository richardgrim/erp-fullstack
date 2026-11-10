package com.richardgrim.erp;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {
    List<OrderHistory> findByOrderIdOrderByIdAsc(Long orderId);
}
