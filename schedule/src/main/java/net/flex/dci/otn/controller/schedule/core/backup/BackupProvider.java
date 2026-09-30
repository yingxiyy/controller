package net.flex.dci.otn.controller.schedule.core.backup;

import java.io.IOException;

/**
 * 2026/5/31
 *
 * @author musa
 * @version 1.0
 **/
public interface BackupProvider {

    public void backup(String path) throws IOException, InterruptedException;

    void restore(String path) throws IOException;

    boolean isAvailable();
}
