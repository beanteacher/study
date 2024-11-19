package com.study.jpa.entity;

import com.study.jpa.entity.item.Item;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class OrderItem {
    @Id @GeneratedValue
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "item_id")
    private Item item;

    private int orderPrice;
    private int count;
}
