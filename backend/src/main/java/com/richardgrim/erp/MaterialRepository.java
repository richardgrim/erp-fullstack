package com.richardgrim.erp;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface MaterialRepository extends JpaRepository<Material,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select m from Material m where m.id=:id")
 Optional<Material> findForUpdate(@Param("id") Long id);
}
