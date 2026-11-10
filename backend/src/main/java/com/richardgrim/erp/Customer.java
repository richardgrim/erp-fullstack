package com.richardgrim.erp;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="customers")
public class Customer {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false,length=120) String name;
 @Column(nullable=false,length=30) String phone;
 @Column(nullable=false,length=160) String email;
 @Column(nullable=false) Instant createdAt;
 protected Customer() {}
 public Long getId() { return id; }
 public String getName() { return name; }
 Customer(String name,String phone,String email) {
  this.name=name.strip(); this.phone=phone.strip(); this.email=email.strip(); createdAt=Instant.now();
 }
}
