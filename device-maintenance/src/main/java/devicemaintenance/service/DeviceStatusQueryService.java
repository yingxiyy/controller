package devicemaintenance.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 设备状态查询服务
 * 专门处理从MongoDB op-phy-node集合查询设备维护状态
 */
@Slf4j
@Service
public class DeviceStatusQueryService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据设备ID或设备名称查询设备物理节点信息
     *
     * @param deviceIdentifier 设备ID（Site-xxx#Ne-xxx）或设备名称
     * @return 设备物理节点文档，如果未找到返回empty
     */
    public Optional<Document> findDevicePhysicalNode(String deviceIdentifier) {
        try {
            log.info("查询设备物理节点: deviceIdentifier={}", deviceIdentifier);
            
            Document result;
            
            // 判断是设备ID还是设备名称
            if (deviceIdentifier.contains("#")) {
                // 完整设备ID查询
                result = queryByDeviceId(deviceIdentifier);
            } else {
                // 设备名称查询
                result = queryByDeviceName(deviceIdentifier);
            }
            
            if (result != null) {
                log.info("找到设备: deviceIdentifier={}", deviceIdentifier);
                return Optional.of(result);
            } else {
                log.warn("未找到设备: deviceIdentifier={}", deviceIdentifier);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("查询设备物理节点失败: deviceIdentifier={}, error={}", deviceIdentifier, e.getMessage());
            throw new RuntimeException("Failed to query device status: " + e.getMessage(), e);
        }
    }

    /**
     * 查询所有主设备的物理节点信息
     * 主设备定义：neId格式为 Site-xxx#Ne-xxx（包含1个#）
     * 排除子设备：equipment-id格式为 Site-xxx#Ne-xxx#EQUIPMENT-xxx（包含2个或更多#）
     *
     * @return 所有主设备的物理节点文档列表
     */
    public List<Document> findAllMainDevicePhysicalNodes() {
        try {
            log.info("查询所有主设备物理节点");
            
            // 查询所有文档
            Query query = new Query();
            query.fields()
                .include("neId")
                .include("data.node.node-id")
                .include("data.node.otn-phy-topology:physical.friendly-name")
                .include("data.node.otn-phy-topology:physical.communication-status")
                .include("data.node.otn-phy-topology:physical.operational-state")
                .include("data.node.otn-phy-topology:physical.system.properties");
            
            List<Document> allDocs = mongoTemplate.find(query, Document.class, "op-phy-node");
            
            // 过滤出主设备（neId中只有2个#）
            List<Document> mainDevices = new java.util.ArrayList<>();
            for (Document doc : allDocs) {
                String neId = doc.getString("neId");
                if (neId != null && isMainDevice(neId)) {
                    mainDevices.add(doc);
                }
            }
            
            log.info("从 {} 个文档中过滤出 {} 个主设备", allDocs.size(), mainDevices.size());
            return mainDevices;
            
        } catch (Exception e) {
            log.error("查询所有主设备失败: error={}", e.getMessage());
            throw new RuntimeException("Failed to query all master devices: " + e.getMessage(), e);
        }
    }

    /**
     * 判断是否为主设备
     * 主设备的neId格式：Site-xxx#Ne-xxx（恰好1个#）
     * 子设备的equipment-id格式：Site-xxx#Ne-xxx#CHASSIS-1（2个或更多#）
     */
    private boolean isMainDevice(String neId) {
        if (neId == null || neId.isEmpty()) {
            return false;
        }
        
        // 统计#的数量
        int hashCount = 0;
        for (int i = 0; i < neId.length(); i++) {
            if (neId.charAt(i) == '#') {
                hashCount++;
            }
        }
        
        // 主设备恰好有1个#
        return hashCount == 1;
    }

    /**
     * 通过设备ID查询
     */
    private Document queryByDeviceId(String deviceId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("data.node.node-id").is(deviceId));
        query.fields()
            .include("data.node.node-id")
            .include("data.node.otn-phy-topology:physical.friendly-name")
            .include("data.node.otn-phy-topology:physical.system.properties");
        
        return mongoTemplate.findOne(query, Document.class, "op-phy-node");
    }

    /**
     * 通过设备名称查询（friendly-name）
     */
    private Document queryByDeviceName(String deviceName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("data.node.otn-phy-topology:physical.friendly-name").is(deviceName));
        query.fields()
            .include("data.node.node-id")
            .include("data.node.otn-phy-topology:physical.friendly-name")
            .include("data.node.otn-phy-topology:physical.system.properties");
        
        return mongoTemplate.findOne(query, Document.class, "op-phy-node");
    }

    /**
     * 提取设备节点信息
     */
    public Optional<Document> extractDeviceNode(Document physicalNodeDoc) {
        if (physicalNodeDoc == null) {
            return Optional.empty();
        }

        try {
            Document data = physicalNodeDoc.get("data", Document.class);
            if (data == null) {
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            List<Document> nodes = (List<Document>) data.get("node");
            if (nodes == null || nodes.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(nodes.get(0));
        } catch (Exception e) {
            log.error("提取设备节点信息失败: error={}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 提取系统属性
     */
    public Optional<List<Document>> extractSystemProperties(Document deviceNode) {
        if (deviceNode == null) {
            return Optional.empty();
        }

        try {
            Document physical = deviceNode.get("otn-phy-topology:physical", Document.class);
            if (physical == null) {
                return Optional.empty();
            }

            Document system = physical.get("system", Document.class);
            if (system == null) {
                return Optional.empty();
            }

            Document properties = system.get("properties", Document.class);
            if (properties == null) {
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            List<Document> propertyList = (List<Document>) properties.get("property");
            
            return Optional.ofNullable(propertyList);
        } catch (Exception e) {
            log.error("提取系统属性失败: error={}", e.getMessage());
            return Optional.empty();
        }
    }
}
