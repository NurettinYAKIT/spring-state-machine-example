package com.nurettin.orderservice.gateway.h2.entity;

import com.nurettin.orderservice.domain.OrderState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

@Entity(name = "ORDERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue
    private Long id;
    private Date dateTime;
    private String state;

    public Order(Date date, OrderState states) {
        this.dateTime = date;
        this.state = states.name();
    }

    public OrderState getOrderState() {
        return com.nurettin.orderservice.domain.OrderState.valueOf(this.state);
    }

    public void setOrderState(OrderState orderStates) {
        this.state = orderStates.name();
    }
}
