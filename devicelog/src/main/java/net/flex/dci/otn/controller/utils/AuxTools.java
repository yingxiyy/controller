package net.flex.dci.otn.controller.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

@Slf4j
public class AuxTools {

    public static String getDateStr() {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    }

    public static String execLocal(String shellCommand) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ByteArrayOutputStream errorStream = new ByteArrayOutputStream()) {
            CommandLine commandLine = CommandLine.parse(shellCommand);

            // 设置执行处理参数
            DefaultExecutor exec = new DefaultExecutor();
            // 利用监视狗来设置超时，毫秒
            ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
            exec.setWatchdog(watchdog);

            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            exec.setStreamHandler(streamHandler);
            exec.setExitValues(null);

//            exec.execute(commandLine);
//
//            log.debug("exec {} and result message: {}", shellCommand,
//                    outputStream.toString());
//            return outputStream.toString().trim();
            int exitCode = exec.execute(commandLine);
            String stdout = outputStream.toString().trim();
            String stderr = errorStream.toString().trim();

            // Throw exception for non-zero exit codes (command execution failed)
            if (exitCode != 0) {
                log.error(
                        "Command execution failed! Command: {}, Exit code: {}, Stdout: {}, Stderr: {}",
                        shellCommand, exitCode, stdout, stderr);
                throw new RuntimeException(String.format(
                        "Command execution failed!%nCommand: %s%nExit code: %d%nError message: %s",
                        shellCommand, exitCode, stderr
                ));
            }

            log.debug("Command executed successfully! Command: {}, Output: {}", shellCommand,
                    stdout);
            return stdout;
        } catch (ExecuteException e) {
            throw new RuntimeException(
                    "Command execution aborted (timeout/terminated): " + shellCommand, e);
        } catch (IOException e) {
            throw new RuntimeException("Command IO exception: " + shellCommand, e);
        }

    }
}