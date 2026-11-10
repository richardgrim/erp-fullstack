package com.richardgrim.erp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StockMovementRepository extends JpaRepository<StockMovement,Long> {
 List<StockMovement> findByMaterialIdOrderByIdDesc(Long materialId);
}
