package com.richardgrim.erp;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="work_orders")
public class WorkOrder {
 enum Status { PENDING, IN_PROGRESS, COMPLETED, DELIVERED }
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY)
 @JoinColumn(name="customer_id",nullable=false) Customer customer;
 @Column(nullable=false,length=1000) String description;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) Status status;
 @Column(nullable=false) Instant createdAt;
 protected WorkOrder() {}
 boolean canAdvanceTo(Status next) {
  return switch (status) {
   case PENDING -> next == Status.IN_PROGRESS;
   case IN_PROGRESS -> next == Status.COMPLETED;
   case COMPLETED -> next == Status.DELIVERED;
   case DELIVERED -> false;
  };
 }
 WorkOrder(Customer customer,String description) {
  this.customer=customer; this.description=description.strip(); status=Status.PENDING; createdAt=Instant.now();
 }
}
