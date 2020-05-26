package com.example.orderservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Component;

@Slf4j
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    enum OrderEvents {
        FULFIL,
        PAY,
        CANCEL
    }

    enum OrderStates {
        SUBMITTED,
        PAID,
        FULFILLED,
        CANCELLED
    }

    @Component
    @RequiredArgsConstructor
    class Runner implements ApplicationRunner {
        private final StateMachineFactory<OrderStates, OrderEvents> factory;

        @Override
        public void run(ApplicationArguments args) throws Exception {
            Long orderId = 1234L;
            StateMachine<OrderStates, OrderEvents> machine = this.factory.getStateMachine(Long.toString(orderId));
            machine.getExtendedState().getVariables().putIfAbsent("orderId", orderId);
            machine.start();
            log.info("Current state :" + machine.getState().getId().name());
            machine.sendEvent(OrderEvents.FULFIL);
            log.info("Current state :" + machine.getState().getId().name());
            machine.sendEvent(OrderEvents.PAY);
            log.info("Current state :" + machine.getState().getId().name());
            Message<OrderEvents> message = MessageBuilder
                    .withPayload(OrderEvents.FULFIL)
                    .setHeader("a", "b")
                    .build();
            machine.sendEvent(message);
            log.info("Current state :" + machine.getState().getId().name());
        }
    }

    @Configuration
    @EnableStateMachineFactory
    class SimpleEnumStateMachineConfiguration extends StateMachineConfigurerAdapter<OrderStates, OrderEvents> {

        @Override
        public void configure(StateMachineTransitionConfigurer<OrderStates, OrderEvents> transitions) throws Exception {
            transitions
                    .withExternal()
                    .source(OrderStates.SUBMITTED).target(OrderStates.PAID).event(OrderEvents.PAY)
                    .and()
                    .withExternal()
                    .source(OrderStates.PAID).target(OrderStates.FULFILLED).event(OrderEvents.FULFIL)
                    .and().withExternal()
                    .source(OrderStates.SUBMITTED).target(OrderStates.CANCELLED).event(OrderEvents.CANCEL)
                    .and().withExternal()
                    .source(OrderStates.PAID).target(OrderStates.CANCELLED).event(OrderEvents.CANCEL);
        }


        @Override
        public void configure(StateMachineStateConfigurer<OrderStates, OrderEvents> states) throws Exception {
            states.withStates()
                    .initial(OrderStates.SUBMITTED)
                    .stateEntry(OrderStates.SUBMITTED, stateContext -> {
                        stateContext.getEvent().name();
                        Long orderId = Long.class.cast(stateContext.getExtendedState().getVariables().getOrDefault("orderId", -1L));
                        log.info("Order id is {}", orderId);
                        log.info("Entering submitted state!");
                    })
                    .state(OrderStates.PAID)
                    .end(OrderStates.CANCELLED)
                    .end(OrderStates.FULFILLED);
        }

        @Override
        public void configure(StateMachineConfigurationConfigurer<OrderStates, OrderEvents> config) throws
                Exception {
            StateMachineListenerAdapter<OrderStates, OrderEvents> adapter = new StateMachineListenerAdapter<OrderStates, OrderEvents>() {
                @Override
                public void stateChanged(State<OrderStates, OrderEvents> from, State<OrderStates, OrderEvents> to) {
                    log.info(String.format("stateChanged( from: %s, to %s)", from + "", to + ""));
                }
            };
            config.withConfiguration()
                    .autoStartup(false)
                    .listener(adapter);
        }
    }
}
