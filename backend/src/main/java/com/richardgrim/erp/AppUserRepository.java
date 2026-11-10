package com.richardgrim.erp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByRole(String role);
    boolean existsByUsername(String username);
}
