//package net.flex.dci.otn.controller.schedule.core;
//
//import java.math.BigInteger;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.mongo.mdoel.schedule.MoSchedule;
//import net.flex.dci.otn.controller.schedule.utils.ConfigLoader;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.controller.rev190906.CtrlDbBackup;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.controller.rev190906.TaskData1;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.controller.rev190906.TaskData1Builder;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.TaskStatus;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.schedule.attributes.TaskDataBuilder;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.schedule.list.ScheduleBuilder;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.schedule.list.ScheduleKey;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DefaultTask {
//
//    private final ConfigLoader configLoader;
//    private final ScheduleRepo repo;
//
//    public List<MoSchedule> build(String sftpServerName) {
//        log.debug("remove default ctrlDBBackup and create again");
//
//        List<MoSchedule> ret = new ArrayList<>();
//        repo.deleteByIds(Arrays.asList("0"));
//        int repeatInterval = 24 * 3600;
//        MoSchedule moSchedule = new MoSchedule(new ScheduleBuilder()
//                .setName("Daily Controller DB backup")
//                .setId("0")
//                .setCreator("system")
//                .setKey(new ScheduleKey("0"))
//                .setOperation(CtrlDbBackup.class)
//                .setStatus(TaskStatus.Activated)
//                .setTaskData(new TaskDataBuilder().addAugmentation(TaskData1.class,
//                                new TaskData1Builder()
//                                        .setFtpServerName(sftpServerName)
//                                        .setName(configLoader.getControllerDbBackupDir())
//                                        .build())
//                        .build())
//                .setStart(Task.calculateNextRunTime(null, repeatInterval))
//                .setRepeatInterval(repeatInterval)
//                .setEnd(BigInteger.valueOf(Long.MAX_VALUE))
//                .build());
//
//        ret.add(moSchedule);
//        return ret;
//    }
//
//}
