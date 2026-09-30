package net.flex.dci.otn.controller.idc.manager.service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.validation.ValidationException;
import net.flex.dci.otn.controller.idc.manager.dto.RegionInfo;
import net.flex.dci.otn.controller.idc.manager.model.BatchIdcDto;
import net.flex.dci.otn.controller.idc.manager.model.IdcData;
import net.flex.dci.otn.controller.idc.manager.model.IdcDto;
import net.flex.dci.otn.controller.idc.manager.model.IdcRpc;
import net.flex.dci.otn.controller.idc.manager.model.PageIdcData;
import net.flex.dci.otn.controller.idc.manager.model.RpcCityInput;
import net.flex.dci.otn.controller.idc.manager.model.RpcInput;
import net.flex.dci.otn.controller.idc.manager.model.RpcRegionInput;

/**
 * @version 1.0
 * @date 2022/1/20 10:51
 */
public interface IdcDataService {

    void BatchSave(Collection<IdcData> idcData);

    PageIdcData listAllIdcByCondition(int offset, int limit);

    List<IdcData> listAllIdc() throws Exception;

    void addNewIdc(IdcData idcData) throws ValidationException;

    void removeIdcById(Long id);

    void editIdc(IdcDto idcDto);

    IdcRpc getAllCityByRegion(RpcInput input) throws IOException;

    IdcRpc getAllCityByRegion(RpcRegionInput input) throws IOException;

    IdcRpc getAllIdcListByCity(RpcInput input);

    IdcRpc getAllIdcListByCity(RpcCityInput input);

    List<String> getAllRegion();

    List<RegionInfo> getAllRegions();

    void batchEditeIdc(BatchIdcDto batchIdcDto);
}
