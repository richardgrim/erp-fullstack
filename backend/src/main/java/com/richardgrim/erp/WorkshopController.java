package com.richardgrim.erp;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.Authentication;

@RestController @RequestMapping("/api") @Transactional
public class WorkshopController {
 private final CustomerRepository customers;
 private final WorkOrderRepository orders;
 private final OrderHistoryRepository history;
 public WorkshopController(CustomerRepository customers,WorkOrderRepository orders,OrderHistoryRepository history) {
  this.customers=customers; this.orders=orders; this.history=history;
 }
 record CustomerInput(@NotBlank @Size(max=120) String name,
   @NotBlank @Size(max=30) String phone,@NotNull @Email @Size(max=160) String email) {}
 record CustomerView(Long id,String name,String phone,String email) {
  static CustomerView from(Customer c) { return new CustomerView(c.id,c.name,c.phone,c.email); }
 }
 record OrderInput(@NotNull @Positive Long customerId,@NotBlank @Size(max=1000) String description) {}
 record OrderView(Long id,Long customerId,String customerName,String description,WorkOrder.Status status,Instant createdAt) {
  static OrderView from(WorkOrder o) { return new OrderView(o.id,o.customer.getId(),o.customer.getName(),o.description,o.status,o.createdAt); }
 }
 @GetMapping("/customers") @Transactional(readOnly=true)
 List<CustomerView> customers() {
  return customers.findAll(Sort.by("name")).stream().map(CustomerView::from).toList();
 }
 @PostMapping("/customers") @ResponseStatus(HttpStatus.CREATED)
 CustomerView createCustomer(@Valid @RequestBody CustomerInput input) {
  return CustomerView.from(customers.save(new Customer(input.name(),input.phone(),input.email())));
 }
 @GetMapping("/orders") @Transactional(readOnly=true)
 List<OrderView> orders() {
  return orders.findAll(Sort.by(Sort.Direction.DESC,"id")).stream().map(OrderView::from).toList();
 }
 @PostMapping("/orders") @ResponseStatus(HttpStatus.CREATED)
 OrderView createOrder(@Valid @RequestBody OrderInput input, Authentication auth) {
  Customer customer=customers.findById(input.customerId()).orElseThrow(
   ()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Cliente no encontrado"));
  WorkOrder order=orders.save(new WorkOrder(customer,input.description()));
  history.save(new OrderHistory(order,null,order.status,order.createdAt,auth.getName()));
  return OrderView.from(order);
 }
 record StatusInput(@NotNull WorkOrder.Status status) {}
 record HistoryView(Long id,WorkOrder.Status fromStatus,WorkOrder.Status toStatus,Instant changedAt,String actorUsername) {
  static HistoryView from(OrderHistory entry) {
   return new HistoryView(entry.id,entry.fromStatus,entry.toStatus,entry.changedAt,entry.actorUsername);
  }
 }
 @PatchMapping("/orders/{id}/status")
 OrderView advance(@PathVariable Long id,@Valid @RequestBody StatusInput input, Authentication auth) {
  if (input.status() == WorkOrder.Status.DELIVERED && auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el administrador puede confirmar la entrega.");
  WorkOrder order=orders.findForUpdate(id).orElseThrow(
   ()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Orden no encontrada"));
  if (!order.canAdvanceTo(input.status())) {
   throw new ResponseStatusException(HttpStatus.CONFLICT,
    "La orden no permite ese cambio. Actualiza el listado para consultar su estado actual.");
  }
  var previous=order.status;
  order.status=input.status();
  history.save(new OrderHistory(order,previous,order.status,Instant.now(),auth.getName()));
  return OrderView.from(order);
 }
 @GetMapping("/orders/{id}/history") @Transactional(readOnly=true)
 List<HistoryView> history(@PathVariable Long id) {
  if (!orders.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Orden no encontrada");
  return history.findByOrderIdOrderByIdAsc(id).stream().map(HistoryView::from).toList();
 }
}
