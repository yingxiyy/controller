package net.flex.dci.otc.controller.status.core.handler;

import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.common.model.ne.StatusChangeEvent;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/4/6 16:55
 */
@Component
@Slf4j
public class StateChangeChainHandler {


    private AbstractStateChangeChainHandler chain;

    //    static Map<Integer, Class<?>> objects = new TreeMap<>();
//    static List<Class<?>> list = new ArrayList<>();
    public StateChangeChainHandler(List<AbstractStateChangeChainHandler> stateChangeChainHandlers) {
        init(stateChangeChainHandlers);
    }

    private void init(List<AbstractStateChangeChainHandler> stateChangeChainHandlers) {
        log.debug("initializing the state change chain handler");
        if (CollectionUtils.isEmpty(stateChangeChainHandlers)) {
            throw new IllegalStateException("No stateChange handler beans found!");
        }
        stateChangeChainHandlers.sort(Comparator.comparingInt(this::normalizeOrder));
        //construct the responsibility chain
        AbstractStateChangeChainHandler chainTemp = stateChangeChainHandlers.get(
                stateChangeChainHandlers.size() - 1);
        for (int i = stateChangeChainHandlers.size() - 2; i >= 0; i--) {
            AbstractStateChangeChainHandler next = stateChangeChainHandlers.get(i);
            next.setNext(chainTemp);
            chainTemp = next;
        }
        chain = chainTemp;
        log.debug("state change handler chain constructing  finished,size:{}",
                stateChangeChainHandlers.size());

    }

    private int normalizeOrder(AbstractStateChangeChainHandler handler) {
        int order = handler.getOrder();
        if (order > AbstractStateChangeChainHandler.HIGHEST_PRECEDENCE
                || order < AbstractStateChangeChainHandler.LOWEST_PRECEDENCE) {
            log.warn("the order for state calculator is range from "
                    + AbstractStateChangeChainHandler.LOWEST_PRECEDENCE + " to "
                    + AbstractStateChangeChainHandler.HIGHEST_PRECEDENCE);
            order = AbstractStateChangeChainHandler.HIGHEST_PRECEDENCE;
        }
        return order;
    }

//    @Override
//    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        Map<String, AbstractStateChangeChainHandler> sonClass = applicationContext.getBeansOfType(
//                AbstractStateChangeChainHandler.class);
//        sonClass.forEach((k, v) -> {
//            compareOrderRange(v);
//        });
//        list = putAllItemsInList();
//    }

//    private void compareOrderRange(AbstractStateChangeChainHandler stateChainCalculator) {
//        int order = stateChainCalculator.getOrder();
//        if (order > AbstractStateChangeChainHandler.HIGHEST_PRECEDENCE
//                || order < AbstractStateChangeChainHandler.LOWEST_PRECEDENCE) {
//            throw new RuntimeException("the order for state calculator is range from "
//                    + AbstractStateChangeChainHandler.LOWEST_PRECEDENCE + " to "
//                    + AbstractStateChangeChainHandler.HIGHEST_PRECEDENCE);
//        }
//        objects.put(order, stateChainCalculator.getClass());
//    }
//
//    private List<Class<?>> putAllItemsInList() {
//        List<Class<?>> list = new ArrayList<>();
//        objects.forEach((k, v) -> list.add(v));
//        return list;
//    }


    public void executeAllStateChangeChainHandle(Node phyNode) {
        long start = System.currentTimeMillis();
        String neId = phyNode.getNodeId().getValue();
        log.debug("[{}] start to calculator the alarm change", neId);
//        AbstractStateChangeChainHandler stateChainCalculator = setCalculatorNext();
        chain.stateChange(phyNode);
        long totalCost = System.currentTimeMillis() - start;
        log.info("[{}] executeAllStateChangeChainHandle TOTAL cost={}ms", neId, totalCost);
    }

    public void executeAllAlarmStateChangeChainHandle(String key, List<Alarm> alarms,
            PhyNodeCache phyNodeCache) {
        log.info("start to calculator the alarm change the key:{}", key);
//        AbstractStateChangeChainHandler stateChainCalculator = setCalculatorNext();
        chain.alarmStateChange(key, alarms, phyNodeCache);
    }

    public void executeAllAlarmClearStateChangeChainHandle(String key, List<Alarm> alarms,
            PhyNodeCache phyNodeCache) {
        log.debug("start to calculator the alarm change the neId:{}", key);
//        AbstractStateChangeChainHandler stateChainCalculator = setCalculatorNext();
        chain.alarmClearStateChange(key, alarms, phyNodeCache);
    }

//    private AbstractStateChangeChainHandler setCalculatorNext() {
//        log.debug("----set handle next calculator for the state change ----");
//        int size = list.size();
//        if (size == 1) {
//            return (AbstractStateChangeChainHandler) applicationContext.getBean(list.get(0));
//        }
//        log.debug("----------size----------{}", size);
//        AbstractStateChangeChainHandler calculator = (AbstractStateChangeChainHandler) applicationContext.getBean(
//                list.get(size - 1));
//        for (int i = size - 2; i >= 0; i--) {
//            AbstractStateChangeChainHandler nextCalculator = (AbstractStateChangeChainHandler) applicationContext.getBean(
//                    list.get(i));
//            nextCalculator.setNext(calculator);
//            calculator = nextCalculator;
//        }
//        log.debug("------sort calculator finish------");
//        return calculator;
//    }


    public void executeAllNodeRemoveStateChangeChainHandle(String neId) {
        log.debug("start to calculator the physical node remove state change");
//        AbstractStateChangeChainHandler stateChangeChainHandler = setCalculatorNext();
        chain.phyNodeRemoveStateChange(neId);
    }

    public void processStatusEventChange(StatusChangeEvent statusChangeEvent) {
        log.debug("start to calculate the status change event");
        chain.statusEventChange(statusChangeEvent);
    }
}
