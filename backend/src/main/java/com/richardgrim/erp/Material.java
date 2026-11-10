package com.richardgrim.erp;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="materials")
public class Material {
 enum Unit { UNIDAD, KG, METRO, LITRO }
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false,length=120) String name;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) Unit unit;
 @Column(nullable=false,precision=14,scale=3) BigDecimal stock=BigDecimal.ZERO;
 protected Material() {}
 Material(String name,Unit unit) { this.name=name.strip(); this.unit=unit; }
}
