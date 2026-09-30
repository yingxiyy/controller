/// *
// *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
// *
// *  This program and the accompanying materials are made available under the
// *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
// *  and is available at http://www.eclipse.org/legal/epl-v10.html
// */
//
//package net.flex.dci.otn.controller.nms.nms.handler;
//
//import java.util.ArrayList;
//import java.util.List;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otn.controller.nms.nms.handler.impl.resource.ScheduleJobs;
//import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
//import net.flex.dci.otn.controller.nms.utils.PagedList;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.common.type.rev220821.sort.query.params.SortInfos;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.common.type.rev220821.sort.query.params.SortInfosBuilder;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSchedulePagedInput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSchedulePagedOutput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSchedulePagedOutputBuilder;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.DeleteSchedulesInput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.DeleteSchedulesOutput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.delete.schedules.input.Tasks;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.schedule.list.Schedule;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
//import org.springframework.stereotype.Component;
//
///**
// * @date: 2021/4/13
// */
//@Slf4j
//@Component
//public class ScheduleJobHandler extends AbstractBaseHandler {
//
//    private ScheduleJobs scheduleJobs;
//
//    public ScheduleJobHandler(
//            NetconfTopology netconfTopology) {
//        super(netconfTopology);
//        scheduleJobs = new ScheduleJobs(netconfTopology);
//    }
//
//    @Override
//    public GetSchedulePagedOutput getSchedulePaged(GetSchedulePagedInput input) throws Exception {
//        log.info("getSchedulePaged input :{}", input);
//
//        GetSchedulePagedOutputBuilder builder = new GetSchedulePagedOutputBuilder();
//        PagedList page = new PagedList(this.scheduleJobs.getSchedules());
//        page.setFilter(input.getFilter());
//        if (input.getSortInfos() != null
//                && input.getSortInfos().size() > 0) {
//            page.sort(input.getSortInfos());
//        } else {
//            List<SortInfos> sorts = new ArrayList<>();
//            SortInfosBuilder b = new SortInfosBuilder();
//            b.setAscending(false);
//            b.setSortName("task-start");
//            sorts.add(b.build());
//            page.sort(sorts);
//        }
//
//        Integer startPos = input.getStartPos() == null ? 0 : input.getStartPos();
//        builder.setTotalRecords(page.getRecordsNumber());
//        builder.setStartPos(startPos);
//        builder.setSchedule((List<Schedule>) page.getPage(startPos, input.getHowMany()));
//        return builder.build();
//
//    }
////
////
////    @Override
////    public DeleteSchedulesOutput deleteSchedules(DeleteSchedulesInput input) {
////        List<Tasks> delTasks = input.getTasks();
////        if (delTasks != null && !delTasks.isEmpty()) {
////            delTasks.forEach(task -> {
////                netconfTopology.deleteScheduleTask(task.getId());
////            });
////        }
////        DeleteSchedulesOutputBuilder outputBuilder = new DeleteSchedulesOutputBuilder();
////        outputBuilder.setReturnCode(RpcResultType.Success);
////        return outputBuilder.build();
////    }
//}
