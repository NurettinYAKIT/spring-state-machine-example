package com.nurettin.orderservice;

import com.nurettin.orderservice.domain.OrderEvent;
import com.nurettin.orderservice.domain.OrderState;
import com.nurettin.orderservice.gateway.h2.OrderRepository;
import com.nurettin.orderservice.gateway.h2.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

import static com.nurettin.orderservice.domain.Constants.ORDER_ID_HEADER;
import static com.nurettin.orderservice.domain.Constants.PAYMENT_CONFIRMATION_NUMBER;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository repository;
    private final StateMachineFactory<OrderState, OrderEvent> stateMachineFactory;

    public Order create(Date date) {
        return this.repository.save(new Order(date, com.nurettin.orderservice.domain.OrderState.SUBMITTED));
    }

    StateMachine<OrderState, OrderEvent> pay(Long orderId, String paymentConfirmationNumber) {
        StateMachine<OrderState, OrderEvent> stateMachine = this.build(orderId);

        Message<OrderEvent> paymentMessage = MessageBuilder.withPayload(com.nurettin.orderservice.domain.OrderEvent.PAY)
                .setHeader(ORDER_ID_HEADER, orderId)
                .setHeader(PAYMENT_CONFIRMATION_NUMBER, paymentConfirmationNumber)
                .build();
        stateMachine.sendEvent(paymentMessage);

        return stateMachine;
    }

    public StateMachine<OrderState, OrderEvent> fulfill(Long orderId) {
        StateMachine<OrderState, OrderEvent> stateMachine = this.build(orderId);

        Message<OrderEvent> fulfilmentMessage = MessageBuilder.withPayload(com.nurettin.orderservice.domain.OrderEvent.FULFIL)
                .setHeader(ORDER_ID_HEADER, orderId)
                .build();
        stateMachine.sendEvent(fulfilmentMessage);

        return stateMachine;
    }

    public Order getOrderById(Long id) {
        return repository.findById(id).get();
    }

    private StateMachine<OrderState, OrderEvent> build(Long orderId) {
        Order order = this.repository.findById(orderId).get();
        String orderIdKey = Long.toString(orderId);
        StateMachine<OrderState, OrderEvent> stateMachine = this.stateMachineFactory.getStateMachine(orderIdKey);
        stateMachine.stop();

        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(stateMachineAccess -> {
                    stateMachineAccess.addStateMachineInterceptor(new StateMachineInterceptorAdapter<OrderState, OrderEvent>() {
                        @Override
                        public void preStateChange(State<OrderState, OrderEvent> state, Message<OrderEvent> message, Transition<OrderState, OrderEvent> transition, StateMachine<OrderState, OrderEvent> stateMachine1) {
                            Optional.ofNullable(message).ifPresent(msg -> {
                                Optional.ofNullable(Long.class.cast(msg.getHeaders().getOrDefault(ORDER_ID_HEADER, -1L)))
                                        .ifPresent(orderId1 -> {
                                            repository.findById(orderId1).ifPresent(order1 -> {
                                                order1.setOrderState(state.getId());
                                                repository.save(order1);
                                            });

                                        });
                            });
                        }
                    });
                    stateMachineAccess.resetStateMachine(new DefaultStateMachineContext<OrderState, OrderEvent>(order.getOrderState(), null, null, null));
                });
        stateMachine.start();

        return stateMachine;
    }
}
