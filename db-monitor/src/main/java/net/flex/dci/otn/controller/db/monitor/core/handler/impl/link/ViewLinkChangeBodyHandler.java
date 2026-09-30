/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core.handler.impl.link;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.COLON;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.VIEW_LINK_COLLECTION;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.REMOVE_ATTRIBUTES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.ViewNodeNamingRule;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.ViewNodeDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.db.monitor.core.handler.AbstractBodyChangeHandler;
import net.flex.dci.otn.controller.db.monitor.core.mapper.GeneralCollectionRefObjectTypeMapper;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.controller.db.monitor.utils.ChangeObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/11/16 13:19
 */
@Slf4j
@Component
public class ViewLinkChangeBodyHandler extends AbstractBodyChangeHandler {

    private static final String SUBNET_ID_FIELD = "subnet-id";

    private static final String VIEW_FIELD = "view";

    private static final String DESTINATION_FIELD = "destination";

    private static final String SOURCE_FIELD = "source";

    private static final String SOURCE_NODE_FIELD = "source-node";

    private static final String DEST_NODE_FIELD = "dest-node";

    @Autowired
    private SubNetTreeNodeDao subNetTreeNodeDao;

    @Autowired
    private ViewNodeDao viewNodeDao;

    @Override
    public String getCollection() {
        return VIEW_LINK_COLLECTION;
    }

    @Override
    protected ChangeObject extractChangeObject(String path, Document sourceDocument,
            Document updateDocument) {
        log.debug("start to extract change object for the view link,change body is:{}",
                updateDocument.toJson());
        ChangeObject changeObject = new ChangeObject();
        if (path.equals("data.link.0")) {
            changeObject = getNodeChangeObject((Document) updateDocument.get(path));
            changeObject.setObjectType(ObjectType.Link.name());
        } else if (path.contains("view-topology:view")) {
            changeObject = extractDetailChangeObject("view-topology:view", path, true,
                    sourceDocument,
                    updateDocument);
            changeObject.setObjectType(ObjectType.Link.name());
        }
        if (changeObject.getChangeBody() != null && !changeObject.getChangeBody().isEmpty()) {
            return changeObject;
        }

        return null;
    }

    @Override
    public List<ChangeObject> extractCreateObject(String collectionName, Document createDoc) {
        log.info("start to extract create body :{}", createDoc.toJson());

        Document changeDoc = new Document();
        for (Entry<String, Object> entry : createDoc.entrySet()) {
            String key = entry.getKey();
            if (key.contains(COLON)) {
                changeDoc.put(key.split(COLON)[1], entry.getValue());
            } else {
                changeDoc.put(key, entry.getValue());
            }
        }
        //add basic change object
        List<ChangeObject> changeObjects = new ArrayList<>();
        ChangeObject changeObject = new ChangeObject();
        changeObject.setChangeBody(changeDoc);
        changeObject.setObjectType(
                GeneralCollectionRefObjectTypeMapper.getGeneralObjectType(collectionName));
        changeObjects.add(changeObject);
        try {
            String currentSubnetId = getSubnetIdFromView(changeDoc);
            if (StringUtils.isBlank(currentSubnetId)) {
                log.warn("current subnetId is empty,skip multi-level rebuild");
                return changeObjects;
            }
            SubNetTreeNode subNetTreeNode = subNetTreeNodeDao.findBySubNetId(currentSubnetId)
                    .orElseThrow(() -> new CommonException(
                            CommonExceptionType.NOT_FOUND_ERROR,
                            "subnet:" + currentSubnetId + " is not found"));
            List<String> pathIds = subNetTreeNode.getPathIds();
            Collections.reverse(pathIds);
            pathIds.remove(currentSubnetId);
            log.info("subnet pathIds(reversed): {}, subnetId:{}", pathIds, currentSubnetId);
            String srcSiteId = ViewNodeNamingRule.extractSiteId(extractLinkSrcSiteId(changeDoc));
            String destSiteId = ViewNodeNamingRule.extractSiteId(extractLinkDestSiteId(changeDoc));

            for (String subnetId : pathIds) {
                log.info("current subnetId is:{}", subnetId);
                String srcViewNodeId = ViewNodeNamingRule.generatedIdWithPlaneId(srcSiteId,
                        subnetId);
                String destViewNodeId = ViewNodeNamingRule.generatedIdWithPlaneId(destSiteId,
                        subnetId);
                boolean srcViewNodeExisted = viewNodeDao.existedViewNodeById(srcViewNodeId);
                boolean destViewNodeExisted = viewNodeDao.existedViewNodeById(destViewNodeId);
                if (!(srcViewNodeExisted && destViewNodeExisted)) {
                    log.warn("no viewNode found for levelSubnetId: {}, srcSiteId:{}, destSiteId:{}",
                            subnetId, srcSiteId, destSiteId);
                    continue;
                }
                Document levelChangeDoc = deepCopyDocument(changeDoc);
                updateLinkSrcDest(levelChangeDoc, srcViewNodeId, destViewNodeId);
                ChangeObject levelChangeObject = new ChangeObject();
                levelChangeObject.setChangeBody(levelChangeDoc);
                levelChangeObject.setObjectType(
                        GeneralCollectionRefObjectTypeMapper.getGeneralObjectType(collectionName));
                changeObjects.add(levelChangeObject);
                log.info("generate level ViewLink success:subnetId={} srcSiteId={} destSiteId={}",
                        subnetId, srcViewNodeId, destViewNodeId);
            }

        } catch (Exception ex) {
            log.error("multi-level viewLink rebuild failed", ex);
        }
        return changeObjects;
    }

    private void updateLinkSrcDest(Document changeDoc, String srcViewNodeId,
            String destViewNodeId) {
        if (changeDoc.containsKey(SOURCE_FIELD)) {
            Document sourceDoc = (Document) changeDoc.get(SOURCE_FIELD);
            sourceDoc.put(SOURCE_NODE_FIELD, srcViewNodeId);
            changeDoc.put(SOURCE_FIELD, sourceDoc);
        }
        // 更新destination
        if (changeDoc.containsKey(DESTINATION_FIELD)) {
            Document destDoc = (Document) changeDoc.get(DESTINATION_FIELD);
            destDoc.put(DEST_NODE_FIELD, destViewNodeId);
            changeDoc.put(DESTINATION_FIELD, destDoc);
        }
    }

    private Document deepCopyDocument(Document changeDoc) {
        return Document.parse(changeDoc.toJson());
    }

    private String extractLinkDestSiteId(Document changeDoc) {
        Document destination = changeDoc.get(DESTINATION_FIELD, Document.class);
        return destination.getString(DEST_NODE_FIELD);
    }

    private String extractLinkSrcSiteId(Document changeDoc) {
        Document source = changeDoc.get(SOURCE_FIELD, Document.class);
        return source.getString(SOURCE_NODE_FIELD);
    }


    /**
     * get subnetId From View
     *
     * @param changeDoc
     * @return
     */
    private String getSubnetIdFromView(Document changeDoc) {
        Document viewLinkPhysicalDoc = changeDoc.get(VIEW_FIELD, Document.class);
        return viewLinkPhysicalDoc.getString(SUBNET_ID_FIELD);
    }

    @Override
    public ChangeObject extractUNSETChangeObject(Document sourceDocument, Document unsetDocument) {
        log.info("start to extract the unset attribute {}", unsetDocument.toJson());
        ChangeObject changeObject = new ChangeObject();
        for (Map.Entry<String, Object> entry : unsetDocument.entrySet()) {
            String key = entry.getKey();
            Boolean value = (Boolean) entry.getValue();
            if (value) {
                String[] subPaths = key.split("\\.");
                String unsetAttribute = subPaths[subPaths.length - 1];
                Document unsetDoc = new Document(REMOVE_ATTRIBUTES, unsetAttribute);
                changeObject = ChangeObjectUtils.buildUnsetObject(sourceDocument, subPaths,
                        unsetDoc);
                if (changeObject != null) {
                    changeObject.setObjectType(ObjectType.Link.name());
                }

            }
        }
        return changeObject;
    }


}
