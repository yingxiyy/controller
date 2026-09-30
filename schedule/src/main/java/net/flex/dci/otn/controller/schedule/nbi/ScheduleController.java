/// *
// *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
// *
// *  This program and the accompanying materials are made available under the
// *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
// *  and is available at http://www.eclipse.org/legal/epl-v10.html
// */
//
//package net.flex.dci.otn.controller.schedule.nbi;
//
//import java.util.stream.Collectors;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.common.exception.CommonException;
//import net.flex.dci.otc.common.exception.CommonExceptionType;
//import net.flex.dci.otc.serialization.util.SerializeUtil;
////import net.flex.dci.otn.controller.schedule.core.ScheduleRepo;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSchedulePagedInput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.CreateScheduleInput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.CreateScheduleOutput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.DeleteSchedulesInput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.DeleteSchedulesOutput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.DeleteSchedulesOutputBuilder;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.MarkScheduleFinishInput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.StartNowInput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.delete.schedules.input.Tasks;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.ResponseBody;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@Slf4j
//@RequiredArgsConstructor
//public class ScheduleController {
//
//
//    private final ScheduleRepo repo;
//
//    @PostMapping(value = {"/restconf/operations/schedule:create-schedule"},
//            produces = "application/json;charset=UTF-8")
//    public @ResponseBody String createSchedule(@RequestBody String json) {
//        log.debug("createSchedule input ");
//
//        CreateScheduleInput input;
//        try {
//            input = SerializeUtil.parseRpcInput(json, CreateScheduleInput.class);
//
//        } catch (Exception e) {
//            log.error("Failed to parse json input:{}", json, e);
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage(), e);
//
//        }
//        CreateScheduleOutput output;
//        try {
//            output = repo.createIt(input);
//            return SerializeUtil.serializeRpcOutput2Json(output);
//        } catch (Exception e) {
//            log.error("create schedule fail:{}", json, e);
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
//                    e);
//
//        }
//    }
//
//    @GetMapping(value = {"/restconf/config/schedule:schedules"},
//            produces = "application/json;charset=UTF-8")
//    public @ResponseBody String getAllSchedules() {
//        log.info("get all schedules");
//        String result = repo.getAllAsJosn();
//        return result;
//    }
//
//
//    @PostMapping(value = {
//            "/restconf/operations/schedule:get-schedule-paged"},
//            produces = "application/json;charset=UTF-8")
//    public @ResponseBody String getSchedulesPaged(@RequestBody String json) {
//        log.info("get schedules paged!");
//        GetSchedulePagedInput input;
//        try {
//            input = SerializeUtil.parseRpcInput(json, GetSchedulePagedInput.class);
//        } catch (Exception e) {
//            log.error("Failed to parse json input:{}", json, e);
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage(), e);
//        }
//        String result = repo.getPagedString(
//                input.getFilter(),
//                input.getSortInfos(),
//                input.getStartPos(),
//                input.getHowMany() == -1 ? input.getStartPos()
//                        : input.getStartPos() / input.getHowMany(),
//                input.getHowMany() == -1 ? Integer.MAX_VALUE : input.getHowMany());
//        return result;
//    }
//
//    @GetMapping(value = {
//            "/restconf/config/schedule:schedules/schedule/{id}"}, produces = "application/json;charset=UTF-8")
//    public @ResponseBody String getScheduleById(@PathVariable String id) {
//        log.info("get schedule by id {} ", id);
////        String result = repo.getScheduleById(id);
////        return result;
//        return "";
//    }
//
//    @RequestMapping(value = {
//            "/restconf/operations/schedule:delete-schedules"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
//    public @ResponseBody String deleteSchedule(@RequestBody String json) throws Exception {
//        log.info("delete schedules with IDs!");
//        DeleteSchedulesInput input;
//        try {
//            input = SerializeUtil.parseRpcInput(json, DeleteSchedulesInput.class);
//        } catch (Exception e) {
//            log.error("Failed to parse json input:{}", json, e);
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage(), e);
//
//        }
//        String msg = repo.deleteByIds(
//                input.getTasks().stream().map(Tasks::getId).collect(Collectors.toList()));
//
//        DeleteSchedulesOutput output = new DeleteSchedulesOutputBuilder()
//                .setReturnCode(RpcResultType.Success)
//                .setReturnMessage(msg)
//                .build();
//        return SerializeUtil.serializeRpcOutput2Json(output);
//    }
//
//    @RequestMapping(value = "/restconf/operations/schedule:mark-schedule-finish", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
//    public @ResponseBody void makeDone(@RequestBody String json) {
//        log.info("mark the schedule finished ");
//        MarkScheduleFinishInput input;
//        try {
//            input = SerializeUtil.parseRpcInput(json, MarkScheduleFinishInput.class);
//        } catch (Exception e) {
//            log.error("Failed to parse json input:{}", json, e);
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage(), e);
//
//        }
//        String msg = repo.makeDone(input.getTaskId());
//        log.debug(msg);
//    }
//
//    @RequestMapping(value = "/restconf/operations/schedule:start-now", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
//    public @ResponseBody void startNow(@RequestBody String json) {
//        log.info("start the schedule now");
//        StartNowInput input;
//        try {
//            input = SerializeUtil.parseRpcInput(json, StartNowInput.class);
//        } catch (Exception e) {
//            log.error("Failed to parse json input:{}", json, e);
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage(), e);
//
//        }
//        String msg = repo.restart(input.getTaskId());
//        log.debug(msg);
//    }
//}
