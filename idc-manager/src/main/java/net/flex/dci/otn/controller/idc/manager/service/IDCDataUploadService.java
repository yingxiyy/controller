package net.flex.dci.otn.controller.idc.manager.service;

import java.util.Collection;
import net.flex.dci.otn.controller.idc.manager.model.IdcData;

/**
 * @version 1.0
 * @date 2022/3/27 11:44
 */
public interface IDCDataUploadService {

    void batchUploadIdcData(Collection<IdcData> idcDatas);

}
