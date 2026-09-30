package net.flex.dci.otn.controller.db.monitor.utils;

import static net.flex.dci.otn.controller.db.monitor.utils.Constants.ID;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.type.DataStoreType;
import net.flex.dci.otn.controller.db.monitor.core.mapper.CollectionRefNetConfMapper;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.CollectionDto;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.NetConfEventChangeDto;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

/**
 * @version 1.0
 * @date 10/8/2023 2:19 PM
 */
@Slf4j
public class ChangeObjectUtils {

    /***
     * build unset object
     * @param subPaths
     * @param unsetDoc
     * @return
     */
    public static ChangeObject buildUnsetObject(Document sourceDocument, String[] subPaths,
            Document unsetDoc) {
        log.debug("build remove attribute document {}", unsetDoc.toJson());
        Object targetDoc = sourceDocument;
        if (targetDoc == null) {
            return null;
        }
        for (int j = 0; j < subPaths.length; j++) {
            if (!StringUtils.isNumeric(subPaths[j])) {
                targetDoc = ((Document) targetDoc).get(subPaths[j]);
            } else {
                List<Object> details = (List<Object>) targetDoc;
                targetDoc = details.get(Integer.parseInt(subPaths[j]));
                break;
            }
        }
        Document directDocument = (Document) targetDoc;
        for (String key : directDocument.keySet()) {
            if (key.contains(ID)) {
                unsetDoc = ((Document) unsetDoc).append(key, directDocument.get(key));
                break;
            }
        }
        ChangeObject changeObject = new ChangeObject();
        changeObject.setChangeBody(unsetDoc);
        return changeObject;
    }


    public static NetConfEventChangeDto buildNetConfEventChangeDto(ChangeObject changeObject,
            CollectionDto collectionDto, DataStoreType dataStoreType, Document sourceDoc) {
        if (org.springframework.util.StringUtils.hasText(changeObject.getCollectionName())) {
            collectionDto = CollectionRefNetConfMapper.extractColKeyName(
                    changeObject.getCollectionName());
        }
        NetConfEventChangeDto netConfEventChangeDto = new NetConfEventChangeDto();
        netConfEventChangeDto.setObjectType(changeObject.getObjectType());
        netConfEventChangeDto.setTopologyRef(collectionDto.getTopologyRef());
        netConfEventChangeDto.setTopologyType(collectionDto.getTopologyType());
        netConfEventChangeDto.setChangeData(changeObject.getChangeBody());
        netConfEventChangeDto.setDataStoreType(dataStoreType);
        netConfEventChangeDto.setObjectId(sourceDoc.getString(collectionDto.getKeyName()));
        netConfEventChangeDto.setEventType(changeObject.getEventType());
        netConfEventChangeDto.setObjectKeyName(changeObject.getObjectKeyName());
        return netConfEventChangeDto;
    }

    public static NetConfEventChangeDto buildNetConfEventChangeDto(ChangeObject changeObject,
            CollectionDto collectionDto, DataStoreType dataStoreType) {
        if (org.springframework.util.StringUtils.hasText(changeObject.getCollectionName())) {
            collectionDto = CollectionRefNetConfMapper.extractColKeyName(
                    changeObject.getCollectionName());
        }
        NetConfEventChangeDto netConfEventChangeDto = new NetConfEventChangeDto();
        netConfEventChangeDto.setObjectType(changeObject.getObjectType());
        netConfEventChangeDto.setTopologyRef(collectionDto.getTopologyRef());
        netConfEventChangeDto.setTopologyType(collectionDto.getTopologyType());
        netConfEventChangeDto.setChangeData(changeObject.getChangeBody());
        netConfEventChangeDto.setDataStoreType(dataStoreType);
        netConfEventChangeDto.setEventType(changeObject.getEventType());
        netConfEventChangeDto.setObjectKeyName(changeObject.getObjectKeyName());
//        netConfEventChangeDto.setObjectId(sourceDoc.getString(collectionDto.getKeyName()));
        return netConfEventChangeDto;
    }
}
