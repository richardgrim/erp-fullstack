package com.richardgrim.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.URI;
import java.net.http.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderLifecycleTests {
    @Value("${local.server.port}") int port;
    @Autowired CustomerRepository customers;
    @Autowired WorkOrderRepository orders;
    @Autowired OrderHistoryRepository history;
    HttpClient client;
    String csrf;
    @org.junit.jupiter.api.BeforeEach void authenticate() throws Exception {
        var session = TestSession.admin(port); client = session.client; csrf = session.csrf;
    }

    HttpRequest request(String method, String path, String body) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/" + path))
            .header("Content-Type", "application/json")
            .header("X-CSRF-TOKEN", csrf)
            .method(method, HttpRequest.BodyPublishers.ofString(body)).build();
    }
    HttpResponse<String> send(String method, String path, String body) throws Exception {
        return client.send(request(method, path, body), HttpResponse.BodyHandlers.ofString());
    }
    Long newOrder() throws Exception {
        var customer = customers.save(new Customer("Cliente ficticio", "70000000", ""));
        String description = "Prueba " + UUID.randomUUID();
        var response = send("POST", "orders", "{\"customerId\":" + customer.id + ",\"description\":\"" + description + "\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        return orders.findAll().stream().filter(o -> o.description.equals(description)).findFirst().orElseThrow().id;
    }
    HttpResponse<String> advance(Long id, String status) throws Exception {
        return send("PATCH", "orders/" + id + "/status", "{\"status\":\"" + status + "\"}");
    }

    @Test void fullLifecyclePersistsChronologicalHistory() throws Exception {
        Long id = newOrder();
        for (String status : new String[]{"IN_PROGRESS", "COMPLETED", "DELIVERED"}) {
            var response = advance(id, status);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"status\":\"" + status + "\"");
        }
        assertThat(orders.findById(id).orElseThrow().status).isEqualTo(WorkOrder.Status.DELIVERED);
        var entries = history.findByOrderIdOrderByIdAsc(id);
        assertThat(entries).hasSize(4);
        assertThat(entries.getFirst().fromStatus).isNull();
        assertThat(entries).extracting(e -> e.toStatus).containsExactly(
            WorkOrder.Status.PENDING, WorkOrder.Status.IN_PROGRESS, WorkOrder.Status.COMPLETED, WorkOrder.Status.DELIVERED);
        assertThat(entries).extracting(e -> e.changedAt).isSorted();
        var response = send("GET", "orders/" + id + "/history", "");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("changedAt", "IN_PROGRESS", "DELIVERED");
        assertThat(advance(id, "PENDING").statusCode()).isEqualTo(409);
        assertThat(history.findByOrderIdOrderByIdAsc(id)).hasSize(4);
    }

    @Test void skipsAndBackwardsChangesDoNotAlterHistory() throws Exception {
        Long id = newOrder();
        assertThat(advance(id, "DELIVERED").statusCode()).isEqualTo(409);
        assertThat(orders.findById(id).orElseThrow().status).isEqualTo(WorkOrder.Status.PENDING);
        assertThat(history.findByOrderIdOrderByIdAsc(id)).hasSize(1);
        assertThat(advance(id, "IN_PROGRESS").statusCode()).isEqualTo(200);
        assertThat(advance(id, "PENDING").statusCode()).isEqualTo(409);
        assertThat(history.findByOrderIdOrderByIdAsc(id)).hasSize(2);
    }

    @Test void concurrentDuplicateRequestsRecordOnlyOneTransition() throws Exception {
        Long id = newOrder();
        var request = request("PATCH", "orders/" + id + "/status", "{\"status\":\"IN_PROGRESS\"}");
        var first = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        var second = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        assertThat(java.util.List.of(first.join().statusCode(), second.join().statusCode()))
            .containsExactlyInAnyOrder(200, 409);
        assertThat(history.findByOrderIdOrderByIdAsc(id)).hasSize(2);
    }

    @Test void invalidStatusAndMissingOrderAreRejected() throws Exception {
        Long id = newOrder();
        assertThat(advance(id, "UNKNOWN").statusCode()).isEqualTo(400);
        assertThat(send("PATCH", "orders/" + id + "/status", "{}").statusCode()).isEqualTo(400);
        assertThat(advance(999999L, "IN_PROGRESS").statusCode()).isEqualTo(404);
        assertThat(send("GET", "orders/999999/history", "").statusCode()).isEqualTo(404);
        assertThat(history.findByOrderIdOrderByIdAsc(id)).hasSize(1);
    }
}
