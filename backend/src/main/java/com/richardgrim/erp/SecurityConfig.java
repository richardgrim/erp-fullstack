package com.richardgrim.erp;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean UserDetailsService userDetailsService(AppUserRepository users) {
        return username -> {
            AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
            return User.withUsername(user.username).password(user.passwordHash).roles(user.role).build();
        };
    }

    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http
            .csrf(Customizer.withDefaults())
            .requestCache(cache -> cache.disable())
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/customers", "/api/orders", "/api/materials").hasRole("ADMIN")
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "OPERATOR")
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, ex) -> json(res, 401, "Inicia sesión para continuar."))
                .accessDeniedHandler((req, res, ex) -> json(res, 403, "No tienes permiso o la sesión de seguridad debe actualizarse.")))
            .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) -> res.setStatus(204))
                .failureHandler((req, res, ex) -> json(res, 401, "Usuario o contraseña incorrectos.")))
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .deleteCookies("ERPSESSION")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
            .build();
    }
    private static void json(jakarta.servlet.http.HttpServletResponse res, int status, String detail)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}
