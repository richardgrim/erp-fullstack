package com.richardgrim.erp;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController @RequestMapping("/api/materials") @Transactional
public class InventoryController {
 private final MaterialRepository materials;
 private final StockMovementRepository movements;
 private final WorkOrderRepository orders;
 public InventoryController(MaterialRepository materials,StockMovementRepository movements,WorkOrderRepository orders) {
  this.materials=materials; this.movements=movements; this.orders=orders;
 }
 record MaterialInput(@NotBlank @Size(max=120) String name,@NotNull Material.Unit unit) {}
 record MaterialView(Long id,String name,Material.Unit unit,BigDecimal stock) {
  static MaterialView from(Material m) { return new MaterialView(m.id,m.name,m.unit,m.stock); }
 }
 record MovementInput(@NotNull StockMovement.Kind kind,
  @NotNull @DecimalMin("0.001") @Digits(integer=11,fraction=3) BigDecimal quantity,
  @Positive Long orderId,@NotBlank @Size(max=250) String reason) {}
 record MovementView(Long id,Long orderId,StockMovement.Kind kind,BigDecimal quantity,String reason,String actorUsername,Instant createdAt) {
  static MovementView from(StockMovement m) { return new MovementView(m.id,m.orderId,m.kind,m.quantity,m.reason,m.actorUsername,m.createdAt); }
 }
 @GetMapping @Transactional(readOnly=true)
 List<MaterialView> list() { return materials.findAll(Sort.by("name")).stream().map(MaterialView::from).toList(); }
 @PostMapping @ResponseStatus(HttpStatus.CREATED)
 MaterialView create(@Valid @RequestBody MaterialInput input) {
  return MaterialView.from(materials.save(new Material(input.name(),input.unit())));
 }
 @GetMapping("/{id}/movements") @Transactional(readOnly=true)
 List<MovementView> history(@PathVariable Long id) {
  if (!materials.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Material no encontrado.");
  return movements.findByMaterialIdOrderByIdDesc(id).stream().map(MovementView::from).toList();
 }
 @PostMapping("/{id}/movements") @ResponseStatus(HttpStatus.CREATED)
 MaterialView move(@PathVariable Long id,@Valid @RequestBody MovementInput input,Authentication auth) {
  if(input.kind()==StockMovement.Kind.ENTRY) {
   if(auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_ADMIN")))
    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el administrador registra entradas.");
   if(input.orderId()!=null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Una entrada no debe tener orden.");
  } else {
   if(input.orderId()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona una orden en proceso.");
   // Orden primero: serializa consumo y cierre del trabajo sin invertir el orden de bloqueos.
   WorkOrder order=orders.findForUpdate(input.orderId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Orden no encontrada."));
   if(order.status!=WorkOrder.Status.IN_PROGRESS)
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo puedes consumir materiales en una orden en proceso.");
  }
  Material material=materials.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Material no encontrado."));
  if(material.unit==Material.Unit.UNIDAD && input.quantity().stripTrailingZeros().scale()>0)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Los materiales por unidad requieren cantidades enteras.");
  BigDecimal next=input.kind()==StockMovement.Kind.ENTRY ? material.stock.add(input.quantity()) : material.stock.subtract(input.quantity());
  if(next.signum()<0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Stock insuficiente. Actualiza las existencias.");
  if(next.compareTo(new BigDecimal("99999999999.999"))>0)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La cantidad supera la capacidad del inventario.");
  material.stock=next;
  movements.save(new StockMovement(id,input.orderId(),input.kind(),input.quantity(),input.reason(),auth.getName()));
  return MaterialView.from(material);
 }
}
