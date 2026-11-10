package com.richardgrim.erp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryTests {
 @LocalServerPort int port;
 @Autowired MaterialRepository materials;
 @Autowired StockMovementRepository movements;
 @Autowired WorkOrderRepository orders;
 @Autowired CustomerRepository customers;
 @Autowired AppUserRepository users;
 @Autowired PasswordEncoder encoder;
 TestSession admin;
 @BeforeEach void setup() throws Exception {admin=TestSession.admin(port);}
 long material(String unit) throws Exception {
  var response=admin.send("POST","materials","{\"name\":\"Acero de prueba\",\"unit\":\""+unit+"\"}");
  assertThat(response.statusCode()).isEqualTo(201);
  return JsonMapper.builder().build().readTree(response.body()).get("id").asLong();
 }
 long order(WorkOrder.Status status) {
  var customer=customers.save(new Customer("Inventario demo","70000000",""));
  var order=new WorkOrder(customer,"Trabajo ficticio"); order.status=status;
  return orders.save(order).id;
 }
 String payload(String kind,String quantity,Long order) {
  return "{\"kind\":\""+kind+"\",\"quantity\":"+quantity+",\"orderId\":"+order+",\"reason\":\"Prueba ficticia\"}";
 }
 @Test void fractionalStockAndHistoryPersistTogether() throws Exception {
  long id=material("KG"); long order=order(WorkOrder.Status.IN_PROGRESS);
  assertThat(admin.send("POST","materials/"+id+"/movements",payload("ENTRY","5.750",null)).statusCode()).isEqualTo(201);
  assertThat(admin.send("POST","materials/"+id+"/movements",payload("CONSUME","2.125",order)).statusCode()).isEqualTo(201);
  assertThat(materials.findById(id).orElseThrow().stock).isEqualByComparingTo("3.625");
  var history=movements.findByMaterialIdOrderByIdDesc(id);
  assertThat(history).hasSize(2);
  assertThat(history.getFirst().orderId).isEqualTo(order);
  assertThat(history).allMatch(m->m.actorUsername.equals("admin"));
  assertThat(admin.send("GET","materials/"+id+"/movements","").body()).contains("3").contains("Prueba ficticia");
 }
 @Test void invalidAmountsAndClosedOrdersDoNotChangeStock() throws Exception {
  long id=material("UNIDAD"); long active=order(WorkOrder.Status.IN_PROGRESS);
  for(String quantity:new String[]{"0","-1","0.5","1.0001","100000000000"})
   assertThat(admin.send("POST","materials/"+id+"/movements",payload("ENTRY",quantity,null)).statusCode()).isEqualTo(400);
  assertThat(admin.send("POST","materials/"+id+"/movements",payload("CONSUME","1",active)).statusCode()).isEqualTo(409);
  for(var status:new WorkOrder.Status[]{WorkOrder.Status.PENDING,WorkOrder.Status.COMPLETED,WorkOrder.Status.DELIVERED})
   assertThat(admin.send("POST","materials/"+id+"/movements",payload("CONSUME","1",order(status))).statusCode()).isEqualTo(409);
  assertThat(admin.send("POST","materials/"+id+"/movements",payload("CONSUME","1",null)).statusCode()).isEqualTo(400);
  assertThat(admin.send("POST","materials/"+id+"/movements",payload("ENTRY","1",active)).statusCode()).isEqualTo(400);
  assertThat(materials.findById(id).orElseThrow().stock).isEqualByComparingTo(BigDecimal.ZERO);
  assertThat(movements.findByMaterialIdOrderByIdDesc(id)).isEmpty();
 }
 @Test void operatorCanConsumeButCannotCreateOrReceive() throws Exception {
  long id=material("KG"); long order=order(WorkOrder.Status.IN_PROGRESS);
  admin.send("POST","materials/"+id+"/movements",payload("ENTRY","5",null));
  String name="op"+UUID.randomUUID().toString().substring(0,8);
  users.save(new AppUser(name,"Operador inventario",encoder.encode("Test-operator-only-2026!"),"OPERATOR"));
  var operator=new TestSession(port); assertThat(operator.login(name,"Test-operator-only-2026!").statusCode()).isEqualTo(204);
  assertThat(operator.send("GET","materials","").statusCode()).isEqualTo(200);
  assertThat(operator.send("POST","materials","{\"name\":\"X\",\"unit\":\"KG\"}").statusCode()).isEqualTo(403);
  assertThat(operator.send("POST","materials/"+id+"/movements",payload("ENTRY","2",null)).statusCode()).isEqualTo(403);
  assertThat(operator.send("POST","materials/"+id+"/movements",payload("CONSUME","2",order)).statusCode()).isEqualTo(201);
  assertThat(movements.findByMaterialIdOrderByIdDesc(id).getFirst().actorUsername).isEqualTo(name);
 }
 @Test void simultaneousConsumersCannotOverdrawStock() throws Exception {
  long id=material("KG"); long first=order(WorkOrder.Status.IN_PROGRESS); long second=order(WorkOrder.Status.IN_PROGRESS);
  admin.send("POST","materials/"+id+"/movements",payload("ENTRY","5",null));
  var a=admin.client.sendAsync(admin.request("POST","materials/"+id+"/movements",payload("CONSUME","4",first)),HttpResponse.BodyHandlers.ofString());
  var b=admin.client.sendAsync(admin.request("POST","materials/"+id+"/movements",payload("CONSUME","4",second)),HttpResponse.BodyHandlers.ofString());
  assertThat(java.util.List.of(a.join().statusCode(),b.join().statusCode())).containsExactlyInAnyOrder(201,409);
  assertThat(materials.findById(id).orElseThrow().stock).isEqualByComparingTo("1");
  assertThat(movements.findByMaterialIdOrderByIdDesc(id)).hasSize(2);
 }
}
