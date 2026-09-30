package net.flex.dci.otn.controller.db.monitor.core.service.handler;


import static net.flex.dci.otn.controller.db.monitor.utils.Constants.DATA;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.DELETE_OP;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.DIFF_KEY;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.DIFF_VERSION_KEY;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.OP_SET;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.OP_UNSET;

import io.debezium.connector.mongodb.MongoDbFieldName;
import io.debezium.connector.mongodb.SourceInfo;
import io.debezium.data.Envelope.FieldName;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.base.core.SimpleMongoDao;
import net.flex.dci.otn.controller.db.monitor.core.NetConfEventChangeHandlerDispatcher;
import net.flex.dci.otn.controller.db.monitor.core.enums.OperationMethod;
import net.flex.dci.otn.controller.db.monitor.core.service.NeStatusChangeNotificationService;
import net.flex.dci.otn.controller.db.monitor.core.service.NotificationService;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeV2DiffUpdateDto;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.NetConfEventChangeDto;
import net.flex.dci.otn.controller.db.monitor.transforms.V2ToV1ChangeEventTransforms;
import org.apache.kafka.connect.data.Struct;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.notification.rev180718.EventType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/11/4 10:26
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataUpdateHandler implements IChangeHandler {


    private final NotificationService notificationService;

    private final NeStatusChangeNotificationService neStatusChangeNotificationService;

    private final SimpleMongoDao simpleMongoDao;

    private final NetConfEventChangeHandlerDispatcher netConfEventChangeHandlerDispatcher;

    @Override
    public void handle(Map<String, Object> payload) {
        log.debug("start to handle the data change for update payload is :{}", payload);
        Struct source = (Struct) payload.get(FieldName.SOURCE);
        Struct updateDescription = (Struct) payload.get(MongoDbFieldName.UPDATE_DESCRIPTION);
        if (updateDescription == null) {
            log.warn("updateDescription is null, skip try to handle as full document replacement");
            handleFullReplacement(source, payload);
            return;
        }
        String updatedFields = (String) updateDescription.get(
                MongoDbFieldName.UPDATED_FIELDS);
        List<String> removedFields = updateDescription.getArray(
                MongoDbFieldName.REMOVED_FIELDS);
//        List<Map<String, Object>> truncatedArrays = (List<Map<String, Object>>) updateDescription.get(
//                "truncatedArrays");
        Document updateDetail = getUpdateDetail(updatedFields, removedFields);
//        updateDetail.put(DIFF_VERSION_KEY, 1);
//        String updateFields = (String) updateDescription.get(MongoDbFieldName.UPDATED_FIELDS);
//        Object removeFields = updateDescription.get(MongoDbFieldName.REMOVED_FIELDS);
//        String removeField = removeFields == null ? "" : (String) removeFields;
        Document afterDoc = null;
        if (payload.containsKey(FieldName.AFTER)) {
            String afterDocJson = (String) payload.get(FieldName.AFTER);
            afterDoc = Document.parse(afterDocJson);
        }
//        Document updateDetail = Document.parse(updateFields);
//        String filter = (String) payload.get(MongoDbFieldName.FILTER);
//        String patch = (String) payload.get(MongoDbFieldName.PATCH);
//        Document updateDetail = Document.parse(patch);
        handleUpdateEvent(source, updateDetail, afterDoc);

    }

    /**
     * handle full replacement
     *
     * @param source
     * @param payload
     */
    private void handleFullReplacement(Struct source, Map<String, Object> payload) {
        String afterDocJson = (String) payload.get(FieldName.AFTER);
        if (afterDocJson == null || afterDocJson.isEmpty()) {
            log.warn("AFTER is also null/empty, cannot handle full replacement event");
            return;
        }
        Document afterDoc = Document.parse(afterDocJson);
        Document setDoc = new Document();
        Document dataDoc = (Document) afterDoc.get(DATA);
        if (dataDoc != null) {
            for (String topKey : dataDoc.keySet()) {
                Object value = dataDoc.get(topKey);
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    if (!list.isEmpty()) {
                        setDoc.put("data." + topKey + ".0", list.get(0));
                    }
                } else {
                    setDoc.put("data." + topKey, value);
                }
            }
        }

        if (setDoc.isEmpty()) {
            log.warn("cannot extract data.node.0 from AFTER document, skip");
            return;
        }

        Document updateDetail = new Document();
        updateDetail.put(DIFF_VERSION_KEY, 1);
        updateDetail.put(OP_SET, setDoc);
        handleUpdateEvent(source, updateDetail, afterDoc);
    }

    private Document getUpdateDetail(String updatedFieldsJson,
            List<String> removedFieldsJson) {
        Document updateDetail = new Document();
        updateDetail.put(DIFF_VERSION_KEY, 1);
        Document setDoc = new Document();
        if (updatedFieldsJson != null && !updatedFieldsJson.isEmpty()) {
            Document updatedFields = Document.parse(updatedFieldsJson);
            setDoc.putAll(updatedFields);
        }
        if (!setDoc.isEmpty()) {
            updateDetail.put(OP_SET, setDoc);
        }
        //handle the unset operation
        if (removedFieldsJson != null && !removedFieldsJson.isEmpty()) {
            Document unsetDoc = new Document();
            for (String field : removedFieldsJson) {
                unsetDoc.put(field, true);
            }
            updateDetail.put(OP_UNSET, unsetDoc);
        }
        return updateDetail;
    }

    /**
     * handle the update event
     *
     * @param source //     * @param filter
     * @param updateDetail
     */
    private void handleUpdateEvent(Struct source, Document updateDetail,
            Document sourceDoc) {
        log.debug("start to handle the data update from payload:{},source:{}", updateDetail,
                source);
        String collectionName = (String) source.get(SourceInfo.COLLECTION);

        // region debug-point mongo-query-start
//        Document sourceDoc = simpleMongoDao.findDocument(collectionName,
//                Document.parse(filter));
        // region debug-point mongo-query-done
        // endregion

        Integer diffVersion = updateDetail.getInteger(DIFF_VERSION_KEY);
        if (Objects.nonNull(diffVersion)) {
            handleOPSetOrUnSetUpdateEventByVersion(collectionName, sourceDoc, updateDetail,
                    diffVersion);
        } else {
            Document updateDoc = (Document) updateDetail.get(DATA);
            //handle update operation
            List<NetConfEventChangeDto> updateDtos = netConfEventChangeHandlerDispatcher.extractUpdateInfo(
                    collectionName, sourceDoc, updateDoc);
            notificationUpdate(updateDtos);
        }

//        if (updateDetail.containsKey(OP_SET) || updateDetail.containsKey(OP_UNSET)) {
//
//
//
//        } else {
//            Document updateDoc = (Document) updateDetail.get(DATA);
//            //handle update operation
//            List<NetConfEventChangeDto> updateDtos = netConfEventChangeHandlerDispatcher.extractUpdateInfo(
//                    collectionName, sourceDoc, updateDoc);
//            notificationUpdate(updateDtos);
//        }
    }

    private void handleOPSetOrUnSetUpdateEventByVersion(String collectionName, Document sourceDoc,
            Document updateDetails, Integer diffVersion) {
        log.debug("update change event patch diff version is:{}", diffVersion);
        if (diffVersion.equals(1)) {
            handleDiffVersionOneEventUpdate(collectionName, sourceDoc, updateDetails);
        } else if (diffVersion.equals(2)) {
            handleDiffVersionTwoEventUpdate(collectionName, sourceDoc, updateDetails);
        }
    }

    private void handleDiffVersionTwoEventUpdate(String collectionName, Document sourceDoc,
            Document updateDetail) {
        log.debug("start to handle the diff version two data update from payload:{},source:{}",
                updateDetail,
                sourceDoc);
        Document diffDocument = (Document) updateDetail.get(DIFF_KEY);
        ChangeV2DiffUpdateDto changeV2DiffUpdateDto = V2ToV1ChangeEventTransforms.extractChangeV2DiffUpdateDto(
                diffDocument);
        if (changeV2DiffUpdateDto == null) {
            log.warn("there is nothing to handle,discard it");
            return;
        }
        OperationMethod operationMethod = changeV2DiffUpdateDto.getOperationMethod();
        Document updateDoc = changeV2DiffUpdateDto.getChangeBody();
        log.debug("");
        if (operationMethod.equals(OperationMethod.d)) {
            List<NetConfEventChangeDto> changeDtos = netConfEventChangeHandlerDispatcher.extractRemoveAttributeInfo(
                    collectionName,
                    sourceDoc,
                    updateDoc);
            notificationUpdate(changeDtos);
        } else {
            if (updateDoc.containsKey(DELETE_OP)) {
                List<NetConfEventChangeDto> deleteDtos = netConfEventChangeHandlerDispatcher.extractDeleteInfo(
                        collectionName, sourceDoc, updateDoc);
                notificationDelete(deleteDtos);
            } else {
                List<NetConfEventChangeDto> updateDtos = netConfEventChangeHandlerDispatcher.extractUpdateInfo(
                        collectionName, sourceDoc, updateDoc);
                notificationUpdate(updateDtos);
            }
        }

    }


    private void handleDiffVersionOneEventUpdate(String collectionName, Document sourceDoc,
            Document updateDetail) {
        log.debug("start to handle the diff version one data update from payload:{},source:{}",
                updateDetail,
                sourceDoc);
        if (updateDetail.containsKey(OP_UNSET)) {
            Document unsetDoc = (Document) updateDetail.get(OP_UNSET);
            List<NetConfEventChangeDto> unsetDtos = netConfEventChangeHandlerDispatcher.extractRemoveAttributeInfo(
                    collectionName,
                    sourceDoc,
                    unsetDoc);
            notificationUpdate(unsetDtos);
        } else if (updateDetail.containsKey(OP_SET)) {
            Document updateDoc = (Document) updateDetail.get(OP_SET);
            if (updateDoc.containsKey(DELETE_OP)) {
                //handle delete operation
                List<NetConfEventChangeDto> deleteDtos = netConfEventChangeHandlerDispatcher.extractDeleteInfo(
                        collectionName, sourceDoc, updateDoc);
                notificationDelete(deleteDtos);
            } else {
                List<NetConfEventChangeDto> updateDtos = netConfEventChangeHandlerDispatcher.extractUpdateInfo(
                        collectionName, sourceDoc, updateDoc);
                notificationUpdate(updateDtos);
            }
        }
    }

    private void notificationDelete(List<NetConfEventChangeDto> deleteDtos) {
        deleteDtos.forEach(
                deleteDto -> {
                    if (deleteDto.getEventType() == null) {
                        notificationService.publishNotification(EventType.Remove, deleteDto);
                        neStatusChangeNotificationService.publishNotification(EventType.Remove,
                                deleteDto);
                    } else {
                        notificationService.publishNotification(deleteDto.getEventType(),
                                deleteDto);
                        neStatusChangeNotificationService.publishNotification(
                                deleteDto.getEventType(), deleteDto);
                    }
                });
    }

    private void notificationUpdate(List<NetConfEventChangeDto> updateDtos) {
        updateDtos.forEach(
                updateDto -> {
                    if (updateDto.getEventType() == null) {
                        notificationService.publishNotification(EventType.Update, updateDto);
                    } else {
                        notificationService.publishNotification(updateDto.getEventType(),
                                updateDto);
                    }
                });
    }


}
