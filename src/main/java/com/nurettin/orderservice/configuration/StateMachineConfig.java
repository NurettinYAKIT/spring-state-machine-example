package com.nurettin.orderservice.configuration;

import com.nurettin.orderservice.domain.OrderEvent;
import com.nurettin.orderservice.domain.OrderState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

@Slf4j
@Configuration
@EnableStateMachineFactory
public class StateMachineConfig extends StateMachineConfigurerAdapter<OrderState, OrderEvent> {

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderState, OrderEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(com.nurettin.orderservice.domain.OrderState.SUBMITTED).target(com.nurettin.orderservice.domain.OrderState.PAID).event(com.nurettin.orderservice.domain.OrderEvent.PAY)
                .and()
                .withExternal()
                .source(com.nurettin.orderservice.domain.OrderState.PAID).target(com.nurettin.orderservice.domain.OrderState.FULFILLED).event(com.nurettin.orderservice.domain.OrderEvent.FULFIL)
                .and().withExternal()
                .source(com.nurettin.orderservice.domain.OrderState.SUBMITTED).target(com.nurettin.orderservice.domain.OrderState.CANCELLED).event(com.nurettin.orderservice.domain.OrderEvent.CANCEL)
                .and().withExternal()
                .source(com.nurettin.orderservice.domain.OrderState.PAID).target(com.nurettin.orderservice.domain.OrderState.CANCELLED).event(com.nurettin.orderservice.domain.OrderEvent.CANCEL);
    }


    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states) throws Exception {
        states.withStates()
                .initial(com.nurettin.orderservice.domain.OrderState.SUBMITTED)
                .state(com.nurettin.orderservice.domain.OrderState.PAID)
                .end(com.nurettin.orderservice.domain.OrderState.CANCELLED)
                .end(com.nurettin.orderservice.domain.OrderState.FULFILLED);
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<OrderState, OrderEvent> config) throws
            Exception {
        StateMachineListenerAdapter<OrderState, OrderEvent> adapter = new StateMachineListenerAdapter<OrderState, OrderEvent>() {
            @Override
            public void stateChanged(State<OrderState, OrderEvent> from, State<OrderState, OrderEvent> to) {
                log.info(String.format("stateChanged( from: %s, to %s)", from + "", to + ""));
            }
        };
        config.withConfiguration()
                .autoStartup(false)
                .listener(adapter);
    }
}
