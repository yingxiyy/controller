package net.flex.dci.otn.controller.sftp.manager.monitor;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @version 1.0
 * @date 9/24/2025 3:18 PM
 */
@Slf4j
public class DirectoryUploadProgressMonitor {

    private final int totalFiles;
    private final AtomicInteger completed = new AtomicInteger(0);

    public DirectoryUploadProgressMonitor(File root) {
        this.totalFiles = countFiles(root);
    }

    private int countFiles(File dir) {
        if (dir.isFile()) {
            return 1;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return 0;
        }
        int count = 0;
        for (File child : children) {
            count += countFiles(child);
        }
        return count;
    }

    public void fileUploaded(String filePath) {
        int done = completed.incrementAndGet();
        double percent = (done * 100.0) / totalFiles;
        log.info("current file uploaded progress is : {}/{} ({}%)", done, totalFiles,
                String.format("%.2f", percent));

        // 这里也可以通过 WebSocket 推送 JSON 给前端
        // sendProgress("DIRECTORY", filePath, done, totalFiles, percent);
    }

}
