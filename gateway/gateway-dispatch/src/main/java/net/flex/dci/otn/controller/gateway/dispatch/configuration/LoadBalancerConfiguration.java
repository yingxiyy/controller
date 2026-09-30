package net.flex.dci.otn.controller.gateway.dispatch.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.BalanceStrategy;
import net.flex.dci.otn.controller.gateway.dispatch.MicroserviceSelector;
import net.flex.dci.otn.controller.gateway.dispatch.loadbalance.Balancer;
import net.flex.dci.otn.controller.gateway.dispatch.loadbalance.impl.RandomBalancer;
import net.flex.dci.otn.controller.gateway.dispatch.loadbalance.impl.RoundRobinBalancer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 2022/4/14 16:09
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class LoadBalancerConfiguration {


    @Bean
    public Balancer balancer(LoadBalancerProperties loadBalancerProperties) {
        log.debug("init remote rpc load balancer");
        String strategy = loadBalancerProperties.getStrategy();
        Balancer balancer = new RandomBalancer();
        if (strategy != null) {
            try {
                BalanceStrategy balanceStrategy = BalanceStrategy.valueOf(strategy);
                if (balanceStrategy.equals(BalanceStrategy.Random)) {
                    balancer = new RandomBalancer();
                } else if (balanceStrategy.equals(BalanceStrategy.RoundRobin)) {
                    balancer = new RoundRobinBalancer();
                } else if (balanceStrategy.equals(BalanceStrategy.LeastConnection)) {
                    //temp method for load balance
                    balancer = new RandomBalancer();
                }

            } catch (IllegalArgumentException ex) {
                log.error("the strategy is invalid", ex);
            }

        }
        MicroserviceSelector.setBalancer(balancer);
        return balancer;
    }

}
