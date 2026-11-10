package com.richardgrim.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTests {
    @Value("${local.server.port}") int port;
    @Autowired AppUserRepository users;
    @Autowired CustomerRepository customers;
    @Autowired WorkOrderRepository orders;
    @Autowired OrderHistoryRepository history;
    @Autowired PasswordEncoder encoder;

    @Test void anonymousCannotReadBusinessDataOrWriteWithoutCsrf() throws Exception {
        var session = new TestSession(port);
        assertThat(session.send("GET", "orders", "").statusCode()).isEqualTo(401);
        assertThat(session.send("GET", "users", "").statusCode()).isEqualTo(401);
        assertThat(session.send("POST", "customers", "{}").statusCode()).isEqualTo(403);
        session.refreshCsrf();
        assertThat(session.send("POST", "customers", "{}").statusCode()).isEqualTo(401);
    }

    @Test void loginRotatesSessionAndLogoutInvalidatesAccess() throws Exception {
        var session = new TestSession(port);
        session.refreshCsrf();
        String before = session.cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("ERPSESSION")).findFirst().orElseThrow().getValue();
        assertThat(session.login("admin", "Test-admin-only-2026!").statusCode()).isEqualTo(204);
        String after = session.cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("ERPSESSION")).findFirst().orElseThrow().getValue();
        assertThat(after).isNotEqualTo(before);
        var me = session.send("GET", "auth/me", "");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains("ADMIN").doesNotContain("password", "hash");
        assertThat(session.send("POST", "auth/logout", "{}").statusCode()).isEqualTo(204);
        assertThat(session.send("GET", "orders", "").statusCode()).isEqualTo(401);
    }

    @Test void wrongPasswordFailsAndCsrfIsRequiredEvenAfterLogin() throws Exception {
        var session = new TestSession(port);
        assertThat(session.login("admin", "wrong-password").statusCode()).isEqualTo(401);
        assertThat(session.send("GET", "orders", "").statusCode()).isEqualTo(401);
        session = TestSession.admin(port);
        session.csrf = null;
        assertThat(session.send("POST", "customers", "{}").statusCode()).isEqualTo(403);
    }

    @Test void administratorCreatesOperatorWithHashButCannotCreateDuplicate() throws Exception {
        var admin = TestSession.admin(port);
        String name = "op-" + UUID.randomUUID().toString().substring(0, 12);
        String body = "{\"username\":\"" + name + "\",\"displayName\":\"Operador ficticio\",\"password\":\"Test-operator-only-2026!\",\"role\":\"ADMIN\"}";
        var created = admin.send("POST", "users", body);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("OPERATOR").doesNotContain("password", "hash", "Test-operator");
        AppUser stored = users.findByUsername(name).orElseThrow();
        assertThat(stored.passwordHash).isNotEqualTo("Test-operator-only-2026!");
        assertThat(encoder.matches("Test-operator-only-2026!", stored.passwordHash)).isTrue();
        assertThat(admin.send("POST", "users", body).statusCode()).isEqualTo(409);
        assertThat(admin.send("POST", "users", "{\"username\":\"weak\",\"displayName\":\"Prueba\",\"password\":\"short\"}").statusCode()).isEqualTo(400);
    }

    @Test void operatorCanWorkButCannotCreateAccountsReceiveOrdersOrDeliver() throws Exception {
        String username = "worker-" + UUID.randomUUID().toString().substring(0, 8);
        users.save(new AppUser(username, "Operador de prueba", encoder.encode("Test-operator-only-2026!"), "OPERATOR"));
        var operator = new TestSession(port);
        assertThat(operator.login(username, "Test-operator-only-2026!").statusCode()).isEqualTo(204);
        assertThat(operator.send("GET", "users", "").statusCode()).isEqualTo(403);
        assertThat(operator.send("POST", "users", "{}").statusCode()).isEqualTo(403);
        assertThat(operator.send("POST", "customers", "{}").statusCode()).isEqualTo(403);
        assertThat(operator.send("POST", "orders", "{}").statusCode()).isEqualTo(403);
        assertThat(operator.send("GET", "orders", "").statusCode()).isEqualTo(200);
        var customer = customers.save(new Customer("Cliente ficticio de roles", "70000000", ""));
        var order = orders.save(new WorkOrder(customer, "Trabajo de prueba de roles"));
        for (String status : new String[]{"IN_PROGRESS", "COMPLETED"}) {
            assertThat(operator.send("PATCH", "orders/" + order.id + "/status", "{\"status\":\"" + status + "\"}").statusCode()).isEqualTo(200);
        }
        assertThat(operator.send("PATCH", "orders/" + order.id + "/status", "{\"status\":\"DELIVERED\"}").statusCode()).isEqualTo(403);
        assertThat(orders.findById(order.id).orElseThrow().status).isEqualTo(WorkOrder.Status.COMPLETED);
        assertThat(history.findByOrderIdOrderByIdAsc(order.id)).hasSize(2).allMatch(entry -> username.equals(entry.actorUsername));
        var admin = TestSession.admin(port);
        assertThat(admin.send("PATCH", "orders/" + order.id + "/status", "{\"status\":\"DELIVERED\"}").statusCode()).isEqualTo(200);
        assertThat(history.findByOrderIdOrderByIdAsc(order.id).getLast().actorUsername).isEqualTo("admin");
    }
}
