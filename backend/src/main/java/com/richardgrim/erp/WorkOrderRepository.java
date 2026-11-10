package com.richardgrim.erp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

interface WorkOrderRepository extends JpaRepository<WorkOrder,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from WorkOrder o where o.id = :id")
    Optional<WorkOrder> findForUpdate(Long id);
}
