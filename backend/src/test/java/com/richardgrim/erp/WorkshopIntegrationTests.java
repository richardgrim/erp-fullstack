package com.richardgrim.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkshopIntegrationTests {
 @Value("${local.server.port}") int port;
 @Autowired CustomerRepository customers;
 @Autowired WorkOrderRepository orders;
 HttpClient client;
 String csrf;
 @org.junit.jupiter.api.BeforeEach void authenticate() throws Exception {
  var session = TestSession.admin(port); client = session.client; csrf = session.csrf;
 }
 HttpResponse<String> post(String path,String body) throws Exception {
  return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/"+path))
   .header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf).POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
 }
 @Test void createsCustomerAndLinkedPendingOrder() throws Exception {
  var customer=post("customers","{\"name\":\"Taller de prueba\",\"phone\":\"70000000\",\"email\":\"demo@example.com\"}");
  assertThat(customer.statusCode()).isEqualTo(201);
  var saved=customers.findAll().stream().filter(c->c.name.equals("Taller de prueba")).findFirst().orElseThrow();
  var order=post("orders","{\"customerId\":"+saved.id+",\"description\":\"Fabricar eje de prueba\"}");
  assertThat(order.statusCode()).isEqualTo(201);
  assertThat(order.body()).contains("PENDING","Taller de prueba","Fabricar eje de prueba");
  var listing=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/orders")).GET().build(),HttpResponse.BodyHandlers.ofString());
  assertThat(listing.statusCode()).isEqualTo(200);
  assertThat(listing.body()).contains("Fabricar eje de prueba", "Taller de prueba", "\"customerId\":"+saved.id);
 }
 @Test void rejectsEmptyCustomerWithoutWriting() throws Exception {
  long before=customers.count();
  assertThat(post("customers","{\"name\":\"  \",\"phone\":\"\",\"email\":\"invalid\"}").statusCode()).isEqualTo(400);
  assertThat(customers.count()).isEqualTo(before);
 }
 @Test void rejectsMissingCustomerWithoutOrder() throws Exception {
  long before=orders.count();
  assertThat(post("orders","{\"customerId\":999999,\"description\":\"Prueba\"}").statusCode()).isEqualTo(404);
  assertThat(orders.count()).isEqualTo(before);
 }
 @Test void rejectsBlankDescription() throws Exception {
  assertThat(post("orders","{\"customerId\":1,\"description\":\"  \"}").statusCode()).isEqualTo(400);
 }
}
