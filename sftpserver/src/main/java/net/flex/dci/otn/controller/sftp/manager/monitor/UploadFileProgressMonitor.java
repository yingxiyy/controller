package net.flex.dci.otn.controller.sftp.manager.monitor;

import com.jcraft.jsch.SftpProgressMonitor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @version 1.0
 * @date 9/24/2025 1:23 PM
 */
@Slf4j
public class UploadFileProgressMonitor implements SftpProgressMonitor {

    private long transferred = 0;
    private long fileSize = 0;

    public UploadFileProgressMonitor(long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public void init(int op, String src, String dest, long max) {
        if (max > 0) {
            this.fileSize = max;
        }
        this.transferred = 0;
        log.info("start to upload {} ->{} (size {} bytes)", src, dest, max);
    }

    @Override
    public boolean count(long count) {
        transferred += count;
        if (fileSize > 0) {
            double percent = (transferred * 100.0) / fileSize;
            log.debug("upload process: {}/{} ({}%)", transferred, fileSize,
                    String.format("%.2f", percent));
        } else {
            log.debug("already upload:{} bytes", transferred);

        }
        return true;
    }

    @Override
    public void end() {
        log.info("upload finished");
    }
}
