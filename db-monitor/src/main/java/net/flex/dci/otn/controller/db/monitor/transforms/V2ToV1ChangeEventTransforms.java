package net.flex.dci.otn.controller.db.monitor.transforms;

import static net.flex.dci.otn.controller.db.monitor.utils.Constants.DELETE_OP;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.enums.OperationMethod;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeV2DiffUpdateDto;
import org.bson.Document;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 7/10/2025 3:15 PM
 */
@Slf4j
public class V2ToV1ChangeEventTransforms {

    /**
     * transform the v2 diff change to update document
     *
     * @param diffDocument
     * @return
     */
    public static ChangeV2DiffUpdateDto extractChangeV2DiffUpdateDto(Document diffDocument) {
        Document result = new Document();
        String path = "";
        ChangeV2DiffUpdateDto changeV2DiffUpdateDto = ChangeV2DiffUpdateDto.builder()
                .changeBody(result).build();
        recurse(diffDocument, path, changeV2DiffUpdateDto);
        Document changeDocument = changeV2DiffUpdateDto.getChangeBody();
        if (changeDocument.containsKey(DELETE_OP)) {
            return changeV2DiffUpdateDto;
        }
        Document directChangeDocument = new Document();
        OperationMethod operationMethod = changeV2DiffUpdateDto.getOperationMethod();
        for (String key : changeDocument.keySet()) {
            Object value = changeDocument.get(key);
            if (key.endsWith("." + OperationMethod.a.name())) {
                log.debug("skip mark field: {}", key);
                continue;
            }
            if (operationMethod.equals(OperationMethod.d)) {
                if (value instanceof Boolean && !key.endsWith(OperationMethod.a.name())) {
                    directChangeDocument.put(key, value);
                }
            } else {
//                if (value instanceof Document) {
                directChangeDocument.put(key, value);
//                }
            }
        }

        return ChangeV2DiffUpdateDto.builder().operationMethod(operationMethod)
                .changeBody(directChangeDocument).build();
    }

    private static void recurse(Document node, String path, ChangeV2DiffUpdateDto context) {
        if (node == null || node.isEmpty()) {
            return;
        }
        Map<String, OperationDetail> operations = new LinkedHashMap<>();
        for (String key : node.keySet()) {
            if (isOperationKey(key)) {
                OperationDetail op = null;
                Pattern pattern = Pattern.compile("^u(\\d+)$");
                Matcher matcher = pattern.matcher(key);
                if (matcher.matches()) {
                    String numStr = matcher.group(1);
                    op = processOperationKey(key, node.get(key), buildPath(path, numStr));
                } else {
                    op = processOperationKey(key, node.get(key), path);
                }
                operations.put(key, op);
            }
        }
        for (OperationDetail op : operations.values()) {
            applyOperationToContext(op, context);
        }

        for (String rawKey : node.keySet()) {

            if (operations.containsKey(rawKey)) {
                continue;
            }
            Object val = node.get(rawKey);
            String key = normalize(rawKey);
            String curPath = buildPath(path, key);

            if (val instanceof Document) {
                Document childDoc = (Document) val;
                // 3. 检查是否是带数字后缀的更新操作 (如 u0, u1)
                Pattern pattern = Pattern.compile("^u\\d+$");
                if (pattern.matcher(key).matches()) {
                    // 处理带数字的更新操作
                    context.setOperationMethod(OperationMethod.u);
                    context.getChangeBody().put(path, childDoc);
                } else {
                    // 递归处理子文档
                    recurse(childDoc, curPath, context);
                }
            } else {
                context.getChangeBody().append(curPath, val);
            }

        }
    }

    private static void applyOperationToContext(OperationDetail op, ChangeV2DiffUpdateDto context) {
        switch (op.type) {
            case INSERT:

            case UPDATE:
                context.setOperationMethod(
                        op.type == OperationType.UPDATE ? OperationMethod.u : OperationMethod.i);
                if (op.value instanceof Document) {
                    Document opDoc = (Document) op.value;
//                    for (String field : opDoc.keySet()) {
//                        String fieldPath = buildPath(op.path, field);
//                        Object fieldValue = opDoc.get(field);
//                        context.getChangeBody().put(fieldPath, fieldValue);
//                    }
                    if (!StringUtils.hasText(op.path)) {
                        context.getChangeBody().putAll(opDoc);
                    } else {
                        if (context.getChangeBody().containsKey(op.path)) {
                            Document currentOp = (Document) context.getChangeBody().get(op.path);
                            if (currentOp != null) {
                                currentOp.putAll((Document) op.value);
                            } else {
                                context.getChangeBody()
                                        .put(op.path, new Document((Map<String, Object>) op.value));
                            }
//                            Map<String, Object> currentOp = (Map<String, Object>) context.getChangeBody()
//                                    .get(op.path);
//                            Map<String, Object> currentValue = opDoc;
//                            currentValue.putAll(currentOp);
//                            context.getChangeBody().put(op.path, new Document(currentValue));
                        } else {
                            context.getChangeBody().put(op.path, op.value);
                        }
                    }
                } else {
                    context.getChangeBody().put(op.path, op.value);
                }
                break;
            case DELETE:
                if (context.getOperationMethod() == null) {
                    context.setOperationMethod(OperationMethod.d);
                }
                if (op.value instanceof Document) {
                    processNestedDelete((Document) op.value, op.path, context);
                } else {
                    context.getChangeBody().put(op.path, true);
                }
                break;
        }
    }

    private static OperationDetail processOperationKey(String key, Object value, String path) {
        OperationDetail op = new OperationDetail();
        op.key = key;
        op.path = path;
        op.value = value;
//        if("a".equals(key)) {
//            op.type = OperationType.A
//        }
        if ("d".equals(key)) {
            op.type = OperationType.DELETE;
        } else if ("i".equals(key)) {
            op.type = OperationType.INSERT;
        } else if (key.startsWith("u")) {
            op.type = OperationType.UPDATE;
        }
        return op;
    }

    private static boolean processOperationNode(Document node, String path,
            ChangeV2DiffUpdateDto context) {
        if (node.containsKey(OperationMethod.i.name())) {
            context.setOperationMethod(OperationMethod.i);
            Document insertDoc = node.get("i", Document.class);
            if (StringUtils.hasText(path)) {
                context.getChangeBody().put(path, insertDoc);
            } else {
                context.getChangeBody().putAll(insertDoc);
            }
            return true;
        }
        if (node.containsKey(OperationMethod.u.name())) {
            context.setOperationMethod(OperationMethod.u);
            Document updateDoc = node.get("u", Document.class);
            context.getChangeBody().put(path, updateDoc);
            return true;
        }
        if (node.containsKey(OperationMethod.d.name())) {
            context.setOperationMethod(OperationMethod.d);
            Object deleteValue = node.get("d");
            if (deleteValue instanceof Document) {
                processNestedDelete((Document) deleteValue, path, context);
            } else {
                context.getChangeBody().put(path, true);
            }
            return true;
        }

//        if (node.containsKey(OperationMethod.a.name())) {
//            if (context.getOperationMethod() == null) {
//                context.setOperationMethod(OperationMethod.a);
//            }
//            // 数组操作需要特殊处理...
//            return true;
//        }

        return false;
    }

    private static void processNestedDelete(Document deleteValue, String basePath,
            ChangeV2DiffUpdateDto context) {
        for (String key : deleteValue.keySet()) {
            Object value = deleteValue.get(key);
            String fullPath = buildPath(basePath, key);
            if (value instanceof Boolean) {
                context.getChangeBody().put(fullPath, true);
            } else if (value instanceof Document) {
                processNestedDelete((Document) value, fullPath, context);
            }
        }
    }

    private static String buildPath(String basePath, String key) {
        if (basePath.isEmpty()) {
            return key;
        }
        return basePath + "." + key;
    }

    private static String normalize(String rawKey) {
        if (!rawKey.startsWith("s")) {
            return rawKey;
        }
        String rest = rawKey.substring(1);
        if (rest.matches("\\d+")) {
            return rest;
        }
        return rest;
    }

    private static boolean isOperationKey(String key) {
        return
                key.equals("d") ||
                        key.equals("i") ||
                        key.equals("u") ||
                        key.matches("^u\\d+$");
    }

    public static void main(String[] args) {
//        String json = "{\"sdata\": {\"snode\": {\"a\": true,\"s0\": {\"sotn-phy-topology:physical\": {\"u\": {\"admin-state\": \"up\"},\"i\": {\"ip\": \"192.168.3.226\"}}}}}}}";
//        String json = "{\"sdata\": {\"snode\": {\"a\": true,\"s0\": {\"stermination-point\": {\"a\": true,\"s91\": {\"sotn-phy-topology:physical\": {\"u\": {\"admin-state\": \"up\",\"implement-state\": \"implement\",\"operational-state\": \"up\"}}}}}}}}}";
//        String json = "{\"sdata\": {\"snode\": {\"a\": true,\"s0\": {\"u\": {\"otn-phy-topology:physical\": {\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"port\": 830,\"login-name\": \"admin\",\"login-passwd\": \"Changeit@123\",\"stuffed\": false,\"internal-links\": [{\"link-name\": \"PORT-1-2-L1#PORT-1-1-1SIG\",\"link-ref\": \"OsLink-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-1SIG-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-2#PORT-1-2-L1\",\"implement-state\": \"allocate\",\"src-tp\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-2#PORT-1-2-L1\",\"link-type\": \"os-link\",\"direction\": \"bidirection\",\"dst-tp\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-1SIG\"},{\"link-name\": \"PORT-1-1-1A#EXT:PORT-1-50-M1D1\",\"link-ref\": \"OsLink-Site-1942390716638564352#Ne-1943311953296494592#MUX-1-50#PORT-1-50-M1D1-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-1A\",\"implement-state\": \"allocate\",\"src-tp\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-1A\",\"link-type\": \"os-link\",\"direction\": \"bidirection\",\"dst-tp\": \"Site-1942390716638564352#Ne-1943311953296494592#MUX-1-50#PORT-1-50-M1D1\"},{\"link-name\": \"PORT-1-3-L1#PORT-1-1-2SIG\",\"link-ref\": \"OsLink-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-2SIG-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-3#PORT-1-3-L1\",\"implement-state\": \"allocate\",\"src-tp\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-3#PORT-1-3-L1\",\"link-type\": \"os-link\",\"direction\": \"bidirection\",\"dst-tp\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-2SIG\"},{\"link-name\": \"PORT-1-1-2A#EXT:PORT-1-50-M2D2\",\"link-ref\": \"OsLink-Site-1942390716638564352#Ne-1943311953296494592#MUX-1-50#PORT-1-50-M2D2-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-2A\",\"implement-state\": \"allocate\",\"src-tp\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-2A\",\"link-type\": \"os-link\",\"direction\": \"bidirection\",\"dst-tp\": \"Site-1942390716638564352#Ne-1943311953296494592#MUX-1-50#PORT-1-50-M2D2\"},{\"link-name\": \"PORT-1-4-L1#PORT-1-1-3SIG\",\"link-ref\": \"OsLink-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-3SIG-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-4#PORT-1-4-L1\",\"implement-state\": \"allocate\",\"src-tp\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-4#PORT-1-4-L1\",\"link-type\": \"os-link\",\"direction\": \"bidirection\",\"dst-tp\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-3SIG\"},{\"link-name\": \"PORT-1-1-3A#EXT:PORT-1-50-M3D3\",\"link-ref\": \"OsLink-Site-1942390716638564352#Ne-1943311953296494592#MUX-1-50#PORT-1-50-M3D3-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-3A\",\"implement-state\": \"allocate\",\"src-tp\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-3A\",\"link-type\": \"os-link\",\"direction\": \"bidirection\",\"dst-tp\": \"Site-1942390716638564352#Ne-1943311953296494592#MUX-1-50#PORT-1-50-M3D3\"}],\"risk-group-name\": \"DefaultRiskPlane\",\"vendor-type\": \"CHASSIS\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"properties\": {\"property\": [{\"name\": \"hostName\",\"value\": \"XXD-II-VI-133\"},{\"name\": \"yang-model\",\"value\": \"ByteDance\"}]},\"node-type\": \"TD\",\"alarm-state\": \"unknown\",\"plane-name\": \"上海\",\"communication-status\": \"loginFail\",\"vendor-name\": \"II-VI\",\"alignment-status\": \"unknown\",\"equipments\": [{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#CHASSIS-1\",\"equip-type\": \"Other\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"equip-type-configed\": \"CHASSIS\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"CHASSIS\",\"alignment-status\": \"unknown\",\"friendly-name\": \"CHASSIS-1\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1\",\"equip-type\": \"OP\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"slot\": \"1\",\"equip-type-configed\": \"OLP3_3\",\"operational-state\": \"unknown\",\"admin-state\": \"up\",\"properties\": {\"property\": [{\"name\": \"slots\",\"value\": \"1\"}]},\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"OLP3_3\",\"alignment-status\": \"unknown\",\"friendly-name\": \"OLP3_3-1-1\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-2\",\"equip-type\": \"OT\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"slot\": \"2\",\"equip-type-configed\": \"L1X12C8\",\"service-type\": \"MUX_4x100G\",\"operational-state\": \"unknown\",\"admin-state\": \"up\",\"properties\": {\"property\": [{\"name\": \"slots\",\"value\": \"2\"}]},\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"L1X12C8\",\"alignment-status\": \"unknown\",\"friendly-name\": \"L1X12C8-1-2\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-3\",\"equip-type\": \"OT\",\"creation-time\": \"2025-07-11T10:44:02+08:00\",\"implement-state\": \"allocate\",\"slot\": \"3\",\"equip-type-configed\": \"L1X12C8\",\"service-type\": \"MUX_4x100G\",\"operational-state\": \"unknown\",\"admin-state\": \"up\",\"properties\": {\"property\": [{\"name\": \"slots\",\"value\": \"3\"}]},\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"L1X12C8\",\"alignment-status\": \"unknown\",\"friendly-name\": \"L1X12C8-1-3\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-4\",\"equip-type\": \"OT\",\"creation-time\": \"2025-07-12T08:57:45+08:00\",\"implement-state\": \"allocate\",\"slot\": \"4\",\"equip-type-configed\": \"L1X12C8\",\"service-type\": \"MUX_4x100G\",\"operational-state\": \"unknown\",\"admin-state\": \"up\",\"properties\": {\"property\": [{\"name\": \"slots\",\"value\": \"4\"}]},\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"L1X12C8\",\"alignment-status\": \"unknown\",\"friendly-name\": \"L1X12C8-1-4\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-5\",\"equip-type\": \"EMPTY\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"slot\": \"5\",\"equip-type-configed\": \"BLANK\",\"operational-state\": \"unknown\",\"admin-state\": \"up\",\"properties\": {\"property\": []},\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"BLANK\",\"alignment-status\": \"unknown\",\"friendly-name\": \"SLOT-1-5\",\"empty\": true},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-6\",\"equip-type\": \"EMPTY\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"slot\": \"6\",\"equip-type-configed\": \"BLANK\",\"operational-state\": \"unknown\",\"admin-state\": \"up\",\"properties\": {\"property\": []},\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"BLANK\",\"alignment-status\": \"unknown\",\"friendly-name\": \"SLOT-1-6\",\"empty\": true},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-7\",\"equip-type\": \"EMPTY\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"slot\": \"7\",\"equip-type-configed\": \"BLANK\",\"operational-state\": \"unknown\",\"admin-state\": \"up\",\"properties\": {\"property\": []},\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"BLANK\",\"alignment-status\": \"unknown\",\"friendly-name\": \"SLOT-1-7\",\"empty\": true},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-8\",\"equip-type\": \"EMPTY\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"slot\": \"8\",\"equip-type-configed\": \"BLANK\",\"operational-state\": \"unknown\",\"admin-state\": \"up\",\"properties\": {\"property\": []},\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"BLANK\",\"alignment-status\": \"unknown\",\"friendly-name\": \"SLOT-1-8\",\"empty\": true},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#PANEL-1-40\",\"equip-type\": \"Other\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"equip-type-configed\": \"PANEL\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"PANEL\",\"alignment-status\": \"unknown\",\"friendly-name\": \"PANEL-1-40\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#PSU-1-21\",\"equip-type\": \"Other\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"equip-type-configed\": \"PSU\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"PSU\",\"alignment-status\": \"unknown\",\"friendly-name\": \"PSU-1-21\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#PSU-1-22\",\"equip-type\": \"Other\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"equip-type-configed\": \"PSU\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"PSU\",\"alignment-status\": \"unknown\",\"friendly-name\": \"PSU-1-22\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#FAN-1-31\",\"equip-type\": \"Other\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"equip-type-configed\": \"FAN\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"FAN\",\"alignment-status\": \"unknown\",\"friendly-name\": \"FAN-1-31\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#FAN-1-32\",\"equip-type\": \"Other\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"equip-type-configed\": \"FAN\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"FAN\",\"alignment-status\": \"unknown\",\"friendly-name\": \"FAN-1-32\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#FAN-1-33\",\"equip-type\": \"Other\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"equip-type-configed\": \"FAN\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"FAN\",\"alignment-status\": \"unknown\",\"friendly-name\": \"FAN-1-33\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#CU-1-41\",\"equip-type\": \"Other\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"equip-type-configed\": \"CU\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"CU\",\"alignment-status\": \"unknown\",\"friendly-name\": \"CU-1-41\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#CU-1-42\",\"equip-type\": \"Other\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"equip-type-configed\": \"CU\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"equip-type-vendor-specific\": \"CU\",\"alignment-status\": \"unknown\",\"friendly-name\": \"CU-1-42\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#TRANSCEIVER-1-2-C1\",\"equip-type\": \"TRANSCEIVER\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"slot\": \"2\",\"equip-type-configed\": \"TRANSCEIVER\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"properties\": {\"property\": [{\"name\": \"ethernet-pmd\",\"value\": \"ETH_100GBASE_LR4\"}]},\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"alignment-status\": \"unknown\",\"friendly-name\": \"TRANSCEIVER-1-2-C1\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#TRANSCEIVER-1-2-L1\",\"equip-type\": \"TRANSCEIVER\",\"creation-time\": \"2025-07-11T10:10:18+08:00\",\"implement-state\": \"allocate\",\"slot\": \"2\",\"equip-type-configed\": \"TRANSCEIVER\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"alignment-status\": \"unknown\",\"friendly-name\": \"TRANSCEIVER-1-2-L1\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#TRANSCEIVER-1-3-C1\",\"equip-type\": \"TRANSCEIVER\",\"creation-time\": \"2025-07-11T10:44:02+08:00\",\"implement-state\": \"allocate\",\"slot\": \"3\",\"equip-type-configed\": \"TRANSCEIVER\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"properties\": {\"property\": [{\"name\": \"ethernet-pmd\",\"value\": \"ETH_100GBASE_CWDM4\"}]},\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"alignment-status\": \"unknown\",\"friendly-name\": \"TRANSCEIVER-1-3-C1\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#TRANSCEIVER-1-3-L1\",\"equip-type\": \"TRANSCEIVER\",\"creation-time\": \"2025-07-11T10:44:02+08:00\",\"implement-state\": \"allocate\",\"slot\": \"3\",\"equip-type-configed\": \"TRANSCEIVER\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"alignment-status\": \"unknown\",\"friendly-name\": \"TRANSCEIVER-1-3-L1\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#TRANSCEIVER-1-4-C1\",\"equip-type\": \"TRANSCEIVER\",\"creation-time\": \"2025-07-12T08:57:45+08:00\",\"implement-state\": \"allocate\",\"slot\": \"4\",\"equip-type-configed\": \"TRANSCEIVER\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"properties\": {\"property\": [{\"name\": \"ethernet-pmd\",\"value\": \"ETH_100GBASE_LR4\"}]},\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"alignment-status\": \"unknown\",\"friendly-name\": \"TRANSCEIVER-1-4-C1\",\"empty\": false},{\"equipment-id\": \"Site-1942390716638564352#Ne-1943493018971672576#TRANSCEIVER-1-4-L1\",\"equip-type\": \"TRANSCEIVER\",\"creation-time\": \"2025-07-12T08:57:45+08:00\",\"implement-state\": \"allocate\",\"slot\": \"4\",\"equip-type-configed\": \"TRANSCEIVER\",\"operational-state\": \"unknown\",\"admin-state\": \"unknown\",\"shelf\": \"1\",\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"alarm-state\": \"unknown\",\"alignment-status\": \"unknown\",\"friendly-name\": \"TRANSCEIVER-1-4-L1\",\"empty\": false}],\"system\": {\"ntp\": [{\"ip\": \"127.0.0.1\"}],\"syslog\": [{\"ip\": \"127.0.0.1\"}],\"radius\": [{\"ip\": \"127.0.0.1\"}],\"properties\": {\"property\": [{\"name\": \"timezone\",\"value\": \"Asia/Shanghai\"}]}},\"friendly-name\": \"XXD-II-VI-133\",\"cross-connections\": [{\"cross-connection-id\": \"XC-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-2#PORT-1-2-C1/odu4=1-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-2#PORT-1-2-L1/odu4x4=1/odu4=1\",\"implement-state\": \"allocate\",\"destination-tp\": [{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-2#PORT-1-2-L1\",\"slot\": \"/odu4x4=1/odu4=1\"}],\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"fixed\": true,\"description\": \"PORT-1-2-C1/L1\",\"operational-state\": \"unknown\",\"direction\": \"bidirection\",\"source-tp\": [{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-2#PORT-1-2-C1\",\"slot\": \"/odu4=1\"}],\"admin-state\": \"unknown\"},{\"cross-connection-id\": \"XC-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-1A-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-1B-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-1C-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-1SIG\",\"implement-state\": \"allocate\",\"destination-tp\": [{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-1A\"},{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-1B\"},{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-1C\"}],\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"fixed\": true,\"aps\": {\"revertive\": false,\"force-to-port\": \"NONE\",\"hold-off-time\": 0,\"aps-mode\": \"ABSOLUTE\",\"name\": \"APS-1-1-1\",\"properties\": {\"property\": [{\"name\": \"secondary-switch-threshold\",\"value\": \"-10.0\"},{\"name\": \"protected\",\"value\": \"true\"},{\"name\": \"primary-switch-threshold\",\"value\": \"-10.0\"},{\"name\": \"primary-switch-hysteresis\",\"value\": \"1.0\"}]}},\"description\": \"APS-1-1-1\",\"operational-state\": \"unknown\",\"direction\": \"bidirection\",\"source-tp\": [{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-1SIG\"}],\"admin-state\": \"unknown\"},{\"cross-connection-id\": \"XC-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-3#PORT-1-3-C1/odu4=1-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-3#PORT-1-3-L1/odu4x4=1/odu4=1\",\"implement-state\": \"allocate\",\"destination-tp\": [{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-3#PORT-1-3-L1\",\"slot\": \"/odu4x4=1/odu4=1\"}],\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"fixed\": true,\"description\": \"PORT-1-3-C1/L1\",\"operational-state\": \"unknown\",\"direction\": \"bidirection\",\"source-tp\": [{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-3#PORT-1-3-C1\",\"slot\": \"/odu4=1\"}],\"admin-state\": \"unknown\"},{\"cross-connection-id\": \"XC-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-2A-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-2B-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-2C-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-2SIG\",\"implement-state\": \"allocate\",\"destination-tp\": [{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-2A\"},{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-2B\"},{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-2C\"}],\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"fixed\": true,\"aps\": {\"revertive\": false,\"force-to-port\": \"NONE\",\"hold-off-time\": 0,\"aps-mode\": \"ABSOLUTE\",\"name\": \"APS-1-1-2\",\"properties\": {\"property\": [{\"name\": \"secondary-switch-threshold\",\"value\": \"-10.0\"},{\"name\": \"protected\",\"value\": \"true\"},{\"name\": \"primary-switch-threshold\",\"value\": \"-10.0\"},{\"name\": \"primary-switch-hysteresis\",\"value\": \"1.0\"}]}},\"description\": \"APS-1-1-2\",\"operational-state\": \"unknown\",\"direction\": \"bidirection\",\"source-tp\": [{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-2SIG\"}],\"admin-state\": \"unknown\"},{\"cross-connection-id\": \"XC-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-4#PORT-1-4-C1/odu4=1-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-4#PORT-1-4-L1/odu4x4=1/odu4=1\",\"implement-state\": \"allocate\",\"destination-tp\": [{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-4#PORT-1-4-L1\",\"slot\": \"/odu4x4=1/odu4=1\"}],\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"fixed\": true,\"description\": \"PORT-1-4-C1/L1\",\"operational-state\": \"unknown\",\"direction\": \"bidirection\",\"source-tp\": [{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-4#PORT-1-4-C1\",\"slot\": \"/odu4=1\"}],\"admin-state\": \"unknown\"},{\"cross-connection-id\": \"XC-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-3A-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-3B-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-3C-Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-3SIG\",\"implement-state\": \"allocate\",\"destination-tp\": [{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-3A\"},{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-3B\"},{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-3C\"}],\"node-ref\": \"Site-1942390716638564352#Ne-1943493018971672576\",\"fixed\": true,\"aps\": {\"revertive\": false,\"force-to-port\": \"NONE\",\"hold-off-time\": 0,\"aps-mode\": \"ABSOLUTE\",\"name\": \"APS-1-1-3\",\"properties\": {\"property\": [{\"name\": \"secondary-switch-threshold\",\"value\": \"-10.0\"},{\"name\": \"protected\",\"value\": \"true\"},{\"name\": \"primary-switch-threshold\",\"value\": \"-10.0\"},{\"name\": \"primary-switch-hysteresis\",\"value\": \"1.0\"}]}},\"description\": \"APS-1-1-3\",\"operational-state\": \"unknown\",\"direction\": \"bidirection\",\"source-tp\": [{\"tp-ref\": \"Site-1942390716638564352#Ne-1943493018971672576#LINECARD-1-1#PORT-1-1-3SIG\"}],\"admin-state\": \"unknown\"}],\"supervision-status\": \"unmonitored\"}}}}}}";
//        String json = " {\"i\": {\"delete\": \"Site-1942390716638564352#Ne-1943311953296494592\"}}";
        String json = "{\"sdata\": {\"snode\": {\"a\": true,\"s0\": {\"sotn-phy-topology:physical\": {\"sequipments\": {\"a\": true,\"s5\": {\"sproperties\": {\"sproperty\": {\"a\": true,\"s8\": {\"u\": {\"value\": \"OLP3_3\"}}}}}}}}}}}";
        Document diff = Document.parse(json);
        ChangeV2DiffUpdateDto changeObject = extractChangeV2DiffUpdateDto(diff);
        System.out.println(changeObject.getChangeBody().toJson());
    }

    private enum OperationType {
        UPDATE, DELETE, INSERT
    }

    private static class OperationDetail {

        OperationType type;
        String key;
        String path;
        Object value;
    }
}
