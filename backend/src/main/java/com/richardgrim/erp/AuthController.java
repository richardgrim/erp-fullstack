package com.richardgrim.erp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    public AuthController(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }
    record UserView(Long id, String username, String displayName, String role) {
        static UserView from(AppUser u) { return new UserView(u.id, u.username, u.displayName, u.role); }
    }
    record OperatorInput(@NotNull @Pattern(regexp="[a-z][a-z0-9._-]{2,39}") String username,
                         @NotBlank @Size(max=80) String displayName,
                         @NotNull @Size(min=12,max=72) String password) {}

    @GetMapping("/auth/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }
    @GetMapping("/auth/me")
    UserView me(Authentication auth) {
        return UserView.from(users.findByUsername(auth.getName()).orElseThrow());
    }
    @GetMapping("/users")
    List<UserView> users() { return users.findAll().stream().map(UserView::from).toList(); }

    @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED) @Transactional
    UserView createOperator(@Valid @RequestBody OperatorInput input) {
        checkPassword(input.password());
        if (users.existsByUsername(input.username()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese nombre de usuario ya está registrado.");
        return UserView.from(users.save(new AppUser(input.username(), input.displayName(), encoder.encode(input.password()), "OPERATOR")));
    }
    static void checkPassword(String password) {
        if (password == null || password.isBlank() || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usa una contraseña de al menos 12 caracteres y hasta 72 bytes UTF-8.");
    }
}
