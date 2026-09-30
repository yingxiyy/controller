package net.flex.dci.otn.controller.app.monitor.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AppRebootService {

    // 【安全】定义允许操作的服务白名单（强烈建议使用）
    private static final Set<String> ALLOWED_SERVICES = new HashSet<>(Arrays.asList(
            "adapter",
            "advance",
            "allocate-app",
            "system-rest",
            "device-maintenance",
            "app-monitor",
            "auth-rest",
            "data-analyzer",
            "db-monitor",
            "feeder",
            "flink",
            "gateway-rest",
            "hbase",
            "idc-manager",
            "impl-app",
            "maintenance-rest",
            "neManager",
            "nginx",
            "nms-rest",
            "notifier",
            "otdr-rest",
            "pm-query",
            "pm-stream",
            "redis",
            "sftpserver",
            "status",
            "taskinfo",
            "ts",
            "user-manager",
            "mongodb@0",
            "mongodb@1",
            "mongodb@2",
            "mongodb@*",
            "zookeeper@0",
            "zookeeper@1",
            "zookeeper@2",
            "zookeeper@*",
            "kafka@0",
            "kafka@1",
            "kafka@2",
            "kafka@*"
    ));

    // 超时时间（秒）
    private static final long TIMEOUT_SECONDS = 120;

    /**
     * 重启指定服务（使用 service xxx restart）
     *
     * @param appName 服务名称（必须在白名单中）
     * @return 操作结果描述
     * @throws IllegalArgumentException 如果 appName 不合法
     * @throws RuntimeException         如果命令执行失败
     */
    public String reboot(String appName) {
            validateAppName(appName);
            // 方式1：直接使用 restart（推荐，如果系统支持）
            return executeCommand("restart", appName);
    }

    /**
     * 停止服务
     */
    public String stop(String appName) {
        validateAppName(appName);
        return executeCommand("stop", appName);
    }

    /**
     * 启动服务
     */
    public String start(String appName) {
        validateAppName(appName);
        return executeCommand("start", appName);
    }

    /**
     * 使用 stop + start 组合实现重启（兼容不支持 restart 的服务）
     */
    public String rebootByStopStart(String appName) {
        validateAppName(appName);
        String stopResult = executeCommand("stop", appName);
        String startResult = executeCommand("start", appName);
        return "Stop: " + stopResult + "; Start: " + startResult;
    }

    // --- 内部方法 ---

    private void validateAppName(String appName) {
        if (appName == null || appName.trim().isEmpty()) {
            throw new IllegalArgumentException("应用名称不能为空");
        }
        // 【关键安全措施】只允许白名单中的服务
        if (!ALLOWED_SERVICES.contains(appName)) {
            log.warn("拒绝非法服务名请求: {}", appName);
            throw new IllegalArgumentException("不支持的服务名称: " + appName);
        }
    }

    private String executeCommand(String action, String appName) {
        // 使用 ProcessBuilder，参数分开传，避免 shell 注入
        ProcessBuilder pb = new ProcessBuilder("/usr/sbin/service", appName, action);
        pb.redirectErrorStream(true); // 合并 stderr 到 stdout

        try {
            Process process = pb.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            if (!finished) {
                process.destroyForcibly();
                log.error("命令超时: service {} {}", appName, action);
                throw new RuntimeException("执行超时（超过 " + TIMEOUT_SECONDS + " 秒）");
            }

            int exitCode = process.exitValue();
            String result = output.toString().trim();

            if (exitCode == 0) {
                log.info("成功执行: service {} {} | 输出: {}", appName, action, result);
                return "Success: " + (result.isEmpty() ? "No output" : result);
            } else {
                log.error("命令失败 (exit code {}): service {} {} | 输出: {}", exitCode, appName, action, result);
                throw new RuntimeException("命令执行失败，退出码: " + exitCode + "，输出: " + result);
            }

        } catch (IOException | InterruptedException e) {
            log.error("执行命令时发生异常: service {} {}", appName, action, e);
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new RuntimeException("执行命令时发生异常: " + e.getMessage(), e);
        }
    }
}