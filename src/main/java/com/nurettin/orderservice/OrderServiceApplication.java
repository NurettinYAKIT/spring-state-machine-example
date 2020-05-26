package com.nurettin.orderservice;

import com.nurettin.orderservice.domain.OrderEvent;
import com.nurettin.orderservice.domain.OrderState;
import com.nurettin.orderservice.gateway.h2.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Slf4j
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    @Component
    @RequiredArgsConstructor
    class Runner implements ApplicationRunner {
        private final StateMachineFactory<OrderState, OrderEvent> factory;
        private final OrderService orderService;

        @Override
        public void run(ApplicationArguments args) {

            log.info("====================================");
            Order order = orderService.create(new Date());

            StateMachine<OrderState, OrderEvent> paymentStateMachine = orderService.pay(order.getId(), UUID.randomUUID().toString());
            log.info("State after calling pay() {}", paymentStateMachine.getState().getId().name());
            log.info("Order : {}", orderService.getOrderById(order.getId()));

            StateMachine<OrderState, OrderEvent> fulfilStateMachine = orderService.fulfill(order.getId());
            log.info("State after calling fulfill() {}", fulfilStateMachine.getState().getId().name());
            log.info("Order : {}", orderService.getOrderById(order.getId()));
            log.info("====================================");
        }
    }
}
