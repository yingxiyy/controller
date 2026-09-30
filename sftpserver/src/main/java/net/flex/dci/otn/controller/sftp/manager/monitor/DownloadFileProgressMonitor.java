package net.flex.dci.otn.controller.sftp.manager.monitor;

import com.jcraft.jsch.SftpProgressMonitor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @version 1.0
 * @date 9/24/2025 11:32 AM
 */
@Slf4j
public class DownloadFileProgressMonitor implements SftpProgressMonitor {

    private long transferred = 0;

    private long fileSize = 0;

    @Override
    public void init(int op, String src, String dest, long max) {
        this.fileSize = max;
        log.info("start to download the file:{}->dest:{} file size:{} bytes ", src, dest, max);
    }

    @Override
    public boolean count(long count) {
        transferred += count;
        double percent = (fileSize > 0) ? (transferred * 100.0 / fileSize) : 0.0;
        log.debug("download_progress bytes_transferred={} total_bytes={} percent={}%",
                transferred, fileSize, String.format("%.2f", percent));
        return true;
    }

    @Override
    public void end() {
        log.info("filed download success");
    }
}
