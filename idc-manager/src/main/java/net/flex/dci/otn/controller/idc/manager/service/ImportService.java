package net.flex.dci.otn.controller.idc.manager.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * @version 1.0
 * @date 2022/1/17 14:17
 */
public interface ImportService {

    Object importSiteInfo(MultipartFile file) throws Exception;

}
