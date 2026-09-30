/// *
// *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
// *
// *  This program and the accompanying materials are made available under the
// *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
// *  and is available at http://www.eclipse.org/legal/epl-v10.html
// */
//
//package net.flex.dci.otn.controller.nms.nms.handler.impl.resource;
//
//import java.util.ArrayList;
//import java.util.List;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.schedule.list.Schedule;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.schedule.list.ScheduleBuilder;
//
///**
// * @author: xinyzhao
// * @date: 2021/4/13
// */
//@Slf4j
//public class ScheduleJobs extends AbstractTopResource {
//
//    public ScheduleJobs(
//            NetconfTopology netconfTopology) {
//        super(netconfTopology);
//    }
//
//
//    @Override
//    public List<Schedule> getSchedules() {
//        List<Schedule> schedules = netconfTopology.getSchedules();
//        List<Schedule> list = new ArrayList<>();
//        if (schedules != null && !schedules.isEmpty()) {
//            schedules.forEach(sch -> {
//                ScheduleBuilder builder =
//                        new ScheduleBuilder();
//                builder.setId(sch.getId());
//                builder.setName(sch.getName());
//                builder.setStart(sch.getStart());
//                builder.setEnd(sch.getEnd());
//                builder.setOperation(sch.getOperation());
//                builder.setStatus(sch.getStatus());
//                builder.setResult(sch.getResult());
//                builder.setTaskStart(sch.getTaskStart());
//                builder.setTaskEnd(sch.getTaskEnd());
//                list.add(builder.build());
//            });
//        }
//        return list;
//    }
//}
