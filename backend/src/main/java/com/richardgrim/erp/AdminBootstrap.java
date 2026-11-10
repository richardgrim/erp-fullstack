package com.richardgrim.erp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Configuración inicial local; no modifica cuentas ni contraseñas existentes. */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String initialPassword;
    public AdminBootstrap(AppUserRepository users, PasswordEncoder encoder,
                          @Value("${erp.bootstrap.admin-password:}") String initialPassword) {
        this.users = users; this.encoder = encoder; this.initialPassword = initialPassword;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (initialPassword.isEmpty() || users.existsByRole("ADMIN")) return;
        AuthController.checkPassword(initialPassword);
        users.save(new AppUser("admin", "Administrador", encoder.encode(initialPassword), "ADMIN"));
    }
}
