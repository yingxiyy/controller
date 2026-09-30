//package net.flex.dci.otn.controller.schedule.core;
//
//import java.math.BigInteger;
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.mongo.dao.ScheduleDao;
//import net.flex.dci.otc.mongo.mdoel.schedule.MoSchedule;
//import net.flex.dci.otc.serialization.JsonUtil;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.common.type.rev220821.sort.query.params.SortInfos;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.CreateScheduleInput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.CreateScheduleOutput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.CreateScheduleOutputBuilder;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.Schedules;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.SchedulesBuilder;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.TaskStatus;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.schedule.list.Schedule;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.schedule.list.ScheduleBuilder;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class ScheduleRepo {
//
//    private final ScheduleDao dao;
//
//    private final JsonUtil jsonUtil;
//
//    public void save(MoSchedule moSchedule) {
//        dao.save(moSchedule);
//    }
//
//    public void update(MoSchedule moSchedule) {
//        dao.update(moSchedule);
//    }
//
//    public CreateScheduleOutput createIt(CreateScheduleInput input) {
////    init();
//        String id = input.getOperation().getSimpleName() + "_" + System.currentTimeMillis();
//
//        ScheduleBuilder sb = new ScheduleBuilder();
//        sb.fieldsFrom(input);
//        sb.setId(id);
//        MoSchedule schedule = new MoSchedule(sb.build());
//
////        String scheduleTaskIID = "/schedule:schedules/schedule:schedule/" + id + "/task-data";
////        schedule.set_taskInfo(jsonUtil.fromDataObjectToJson(scheduleTaskIID, sb.getTaskData()));
//
//        MoSchedule saved = dao.save(schedule);
//        return new CreateScheduleOutputBuilder().setTaskId(saved.getId()).build();
//    }
//
//    public CreateScheduleOutput createIt_discard(CreateScheduleInput input) {
////    init();
//        ScheduleBuilder sb = new ScheduleBuilder();
//        sb.fieldsFrom(input);
//        String id = input.getOperation().getSimpleName() + "_" + System.currentTimeMillis();
//        sb.setId(id);
//
//        List<Schedule> tmp = new ArrayList<>();
//        SchedulesBuilder ssb = new SchedulesBuilder();
//        tmp.add(sb.build());
//        ssb.setSchedule(tmp);
//        String scheduleIID = "/schedule:schedules/schedule:schedule/" + id + "/task-data";
//        String scstr = jsonUtil.fromDataObjectToJson(scheduleIID, sb.getTaskData());
//
//        dao.saveSchedule_discard(sb.build());
//        return new CreateScheduleOutputBuilder().setTaskId(sb.getId()).build();
//    }
//
//
//    public List<MoSchedule> getAll() {
//        return dao.getAll();
//    }
//
//    public String getAllAsJosn() {
//        List<MoSchedule> moSchedules = getAll();
//        return toString(moSchedules, true);
//    }
//
//    private String toString(List<MoSchedule> moSchedules, boolean fullInfo) {
//        List<Schedule> schedules = moSchedules.stream().map(mo -> mo.convert(fullInfo).build())
//                .collect(Collectors.toList());
//        Schedules scs = new SchedulesBuilder()
//                .setSchedule(schedules).build();
//
//        String schedulesIID = "/schedule:schedules";
//        return jsonUtil.fromDataObjectToJson(schedulesIID, scs);
//    }
//
//    /**
//     * taskId == 0 controllerDB backup CANNOT be removed.
//     *
//     * @param tasks
//     * @return
//     */
//    public String deleteByIds(List<String> tasks) {
//        if (tasks != null || !tasks.isEmpty()) {
//            List<MoSchedule> removed = dao.deleteWithIds(tasks);
//            if (removed != null && !removed.isEmpty()) {
//                List<String> removedId = removed.stream().map(ele -> ele.getId())
//                        .collect(Collectors.toList());
//
//                return "removed tasks are: " + removedId;
//            }
//        }
//        return "";
//    }
//
//
//    public MoSchedule getSchedule(String id) {
//        return dao.getById(id);
//    }
//
//    public String getScheduleById(String id) {
//        MoSchedule mo = dao.getById(id);
//        mo.convert(true);
//
//        String schedulesIID = "/schedule:schedules/schedule/" + id;
//        return jsonUtil.fromDataObjectToJson(schedulesIID, mo.build());
//    }
//
//
//    public String getPagedString(String filter, List<SortInfos> sortInfos, int startPos, int page,
//            int pageSize) {
//        List<MoSchedule> moList = getPaged(filter, sortInfos, startPos, page, pageSize);
//
//        return toString(moList, false);
//    }
//
//    public List<MoSchedule> getPaged(String filter, List<SortInfos> sortInfos, int startPos,
//            int page, int pageSize) {
//        return dao.getPaged(filter, sortInfos, startPos, page, pageSize);
//    }
//
//    public String makeDone(String taskId) {
//        String msg;
//
//        MoSchedule mo = dao.getById(taskId);
//        if (mo != null) {
//            mo.setEnd(BigInteger.valueOf(System.currentTimeMillis()));
//            mo.setStatus(TaskStatus.Expired);
//            dao.update(mo);
//            msg = "schedule updated (markDone) {} " + mo.getName();
//        } else {
//            msg = "cannot find required schedule: " + taskId;
//        }
//
//        return msg;
//    }
//
//    public String restart(String taskId) {
//        String msg;
//
//        MoSchedule mo = dao.getById(taskId);
//        if (mo != null) {
//            mo.setStart(BigInteger.valueOf(System.currentTimeMillis()));
//            mo.setStatus(TaskStatus.Activated);
//            dao.update(mo);
//            msg = "schedule updated (restart) {} " + mo.getName();
//        } else {
//            msg = "cannot find required schedule: " + taskId;
//        }
//
//        return msg;
//    }
//
//    public void done(MoSchedule mo) {
//        long now = Instant.now().getEpochSecond();
//        Long end = mo.getEnd().longValue();
//        Integer repeat = mo.getRepeatInterval();
//
//        if (end != null && end < now) {
//            mo.setStatus(TaskStatus.Expired);
//        } else if (repeat != null && repeat > 0) {
//            mo.setStatus(TaskStatus.Activated);  // repeating task stays active
//        } else {
//            mo.setStatus(TaskStatus.Expired);    // one-time task, after done → expired
//        }
//
//        dao.update(mo);
//        log.debug("schedule updated (markDone) {} ", mo.getName());
//    }
//
//
//}
