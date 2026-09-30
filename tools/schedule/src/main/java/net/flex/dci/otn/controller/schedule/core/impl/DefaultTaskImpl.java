package net.flex.dci.otn.controller.schedule.core.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.mdoel.schedule.MoSchedule;
import net.flex.dci.otc.serialization.JsonUtil;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Component
public class DefaultTaskImpl extends AbstractTaskImpl {

  @Override
  protected void addProcessor() {
    getTaskFactory().addProcessor(MoSchedule.TaskType.unknown, this);
  }

  protected String execLocal(String shellCommand) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    CommandLine commandLine = CommandLine.parse(shellCommand);

    // 设置执行处理参数
    DefaultExecutor exec = new DefaultExecutor();
    // 利用监视狗来设置超时，毫秒
    ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
    exec.setWatchdog(watchdog);

    PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
    exec.setStreamHandler(streamHandler);
    exec.setExitValue(0);

    // 执行命令
    try {
      exec.execute(commandLine);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    log.debug("result message: " + outputStream.toString());
    return outputStream.toString().trim();
  }


}
