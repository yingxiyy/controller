package net.flex.dci.otc.controller.rpc.client.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.BalanceStrategy;
import net.flex.dci.otc.controller.rpc.client.balancer.Balancer;
import net.flex.dci.otc.controller.rpc.client.balancer.RandomBalancer;
import net.flex.dci.otc.controller.rpc.client.balancer.RoundRobinBalancer;
import net.flex.dci.otc.controller.rpc.client.httpclient.OdlRpcClient;
import net.flex.dci.otc.controller.rpc.client.httpclient.SimpleHttpClient;
import net.flex.dci.otc.controller.rpc.client.properties.BalancerProperties;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterManagerRpc;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterRpc;
import net.flex.dci.otc.controller.rpc.client.rpcs.AlarmRpc;
import net.flex.dci.otc.controller.rpc.client.rpcs.CollectorManagerRpc;
import net.flex.dci.otc.controller.rpc.client.rpcs.FtpRpc;
import net.flex.dci.otc.controller.rpc.client.rpcs.HealthChecker;
import net.flex.dci.otc.controller.rpc.client.rpcs.ImplementorRpc;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.controller.rpc.client.rpcs.NmsRpc;
import net.flex.dci.otc.controller.rpc.client.rpcs.OTDRRpc;
import net.flex.dci.otc.controller.rpc.client.rpcs.TelemetryServerManagerRpc;
import net.flex.dci.otc.controller.rpc.client.rpcs.impl.AdapterManagerRpcImpl;
import net.flex.dci.otc.controller.rpc.client.rpcs.impl.AdapterRpcImpl;
import net.flex.dci.otc.controller.rpc.client.rpcs.impl.AlarmRpcImpl;
import net.flex.dci.otc.controller.rpc.client.rpcs.impl.CollectorManagerRpcImpl;
import net.flex.dci.otc.controller.rpc.client.rpcs.impl.FtpRpcImpl;
import net.flex.dci.otc.controller.rpc.client.rpcs.impl.HealthCheckerImpl;
import net.flex.dci.otc.controller.rpc.client.rpcs.impl.ImplementorRpcImpl;
import net.flex.dci.otc.controller.rpc.client.rpcs.impl.NeManagerRpcImpl;
import net.flex.dci.otc.controller.rpc.client.rpcs.impl.NmsRpcImpl;
import net.flex.dci.otc.controller.rpc.client.rpcs.impl.OTDRRpcImpl;
import net.flex.dci.otc.controller.rpc.client.rpcs.impl.TelemetryServerManagerRpcImpl;
import net.flex.dci.otc.controller.rpc.client.utils.ModuleUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * net.flex.dci.otc.controller.rpc.client.rpcs.impl.AdapterRpcImpl,\
 * net.flex.dci.otc.controller.rpc.client.rpcs.impl.NeManagerRpcImpl,\
 * net.flex.dci.otc.controller.rpc.client.rpcs.impl.AdapterManagerRpcImpl,\
 * net.flex.dci.otc.controller.rpc.client.rpcs.impl.CollectorManagerRpcImpl,\
 * net.flex.dci.otc.controller.rpc.client.rpcs.impl.AlarmRpcImpl,\
 * net.flex.dci.otc.controller.rpc.client.rpcs.impl.NmsRpcImpl,\
 * net.flex.dci.otc.controller.rpc.client.rpcs.impl.ImplementorRpcImpl,\
 * net.flex.dci.otc.controller.rpc.client.rpcs.impl.AllocatorRpcImpl,\
 * net.flex.dci.otc.controller.rpc.client.rpcs.impl.TelemetryServerManagerRpcImpl\
 *
 * @version 1.0
 * @date 2022/3/14 19:02
 */
@Configuration
@Import(BalancerProperties.class)
@Slf4j
public class RpcClientAutoConfigure {


    @Bean
    public Balancer balancer(BalancerProperties loadBalancerProperties) {
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
        ModuleUtils.setBalancer(balancer);
        return balancer;
    }

    @Bean
    @ConditionalOnBean(value = Balancer.class)
    public SimpleHttpClient simpleHttpClient() {
        return new SimpleHttpClient();
    }


    @Bean
    @ConditionalOnBean(value = SimpleHttpClient.class)
    public OdlRpcClient odlRpcClient(SimpleHttpClient simpleHttpClient) {
        return new OdlRpcClient(simpleHttpClient);
    }

    @Bean
    @ConditionalOnBean(value = Balancer.class)
    public AdapterRpc adapterRpc() {
        return new AdapterRpcImpl();
    }

    @Bean
    @ConditionalOnBean(value = Balancer.class)
    public AlarmRpc alarmRpc() {
        return new AlarmRpcImpl();
    }

    @Bean
    @ConditionalOnBean(value = Balancer.class)
    public NeManagerRpc neManagerRpc() {
        return new NeManagerRpcImpl();
    }

    @Bean
    @ConditionalOnBean(value = Balancer.class)
    public AdapterManagerRpc adapterManagerRpc() {
        return new AdapterManagerRpcImpl();
    }

    @Bean
    @ConditionalOnBean(value = Balancer.class)
    public CollectorManagerRpc collectorManagerRpc() {
        return new CollectorManagerRpcImpl();
    }

    @Bean
    @ConditionalOnBean(value = Balancer.class)
    public NmsRpc nmsRpc() {
        return new NmsRpcImpl();
    }

    @Bean
    @ConditionalOnBean(value = Balancer.class)
    public ImplementorRpc implementorRpc() {
        return new ImplementorRpcImpl();
    }

    @Bean
    @ConditionalOnBean(value = Balancer.class)
    public TelemetryServerManagerRpc telemetryServerManagerRpc() {
        return new TelemetryServerManagerRpcImpl();
    }

//    @Bean
//    @ConditionalOnBean(value = Balancer.class)
//    public ControllerRpc controllerRpc() {
//        return new ControllerRpcImpl();
//    }

    @Bean
    @ConditionalOnBean(value = Balancer.class)
    public OTDRRpc otdrRpc() {
        return new OTDRRpcImpl();
    }

    @Bean
    @ConditionalOnBean(value = Balancer.class)
    public FtpRpc sftpRpc() {
        return new FtpRpcImpl();
    }

    @Bean
    @ConditionalOnBean(value = Balancer.class)
    public HealthChecker healthChecker() {
        return new HealthCheckerImpl();
    }
}
