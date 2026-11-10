package com.richardgrim.erp;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "order_history")
public class OrderHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    WorkOrder order;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    WorkOrder.Status fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    WorkOrder.Status toStatus;

    @Column(nullable = false)
    Instant changedAt;

    @Column(length = 40)
    String actorUsername;

    protected OrderHistory() {}

    OrderHistory(WorkOrder order, WorkOrder.Status from, WorkOrder.Status to, Instant time) {
        this.order = order;
        this.fromStatus = from;
        this.toStatus = to;
        this.changedAt = time;
    }
    OrderHistory(WorkOrder order, WorkOrder.Status from, WorkOrder.Status to, Instant time, String actor) {
        this(order, from, to, time);
        this.actorUsername = actor;
    }
}
