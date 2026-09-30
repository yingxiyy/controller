package net.flex.dci.otn.controller.resource.statistic.export.file;

import java.nio.file.Path;

/**
 * 2026/6/10
 *
 * @author musa
 * @version 1.0
 **/
public interface ExportFileService {

    void initExportDir();

    Path generateExportFilePath(String fileName, String suffix);
}
