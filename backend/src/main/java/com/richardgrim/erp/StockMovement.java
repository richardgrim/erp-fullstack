package com.richardgrim.erp;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="stock_movements")
public class StockMovement {
 enum Kind { ENTRY, CONSUME }
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false) Long materialId;
 Long orderId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) Kind kind;
 @Column(nullable=false,precision=14,scale=3) BigDecimal quantity;
 @Column(nullable=false,length=250) String reason;
 @Column(nullable=false,length=40) String actorUsername;
 @Column(nullable=false) Instant createdAt;
 protected StockMovement() {}
 StockMovement(Long materialId,Long orderId,Kind kind,BigDecimal quantity,String reason,String actor) {
  this.materialId=materialId; this.orderId=orderId; this.kind=kind; this.quantity=quantity;
  this.reason=reason.strip(); this.actorUsername=actor; this.createdAt=Instant.now();
 }
}
