package net.flex.dci.otn.controller.allocate.designer.config;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.bom.BomMetaInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BomMetaConfig {

    private static final String BYTEDANCE_RESOURCE_MODEL = "ByteDance";
    private static final String BYTEDANCE_2_YANG_MODEL = "ByteDance2.0";

    private static final String CSV_SUFFIX = "BOM.csv";
    private static final String PATTERN = "classpath*:*" + CSV_SUFFIX;


    private Map<String, BomMetaInfo> bomMetaMap = new HashMap<>();//vendorName-vendorType-equipTypeConfiged 或者vendorName-vendorType-equipTypeConfiged-CardClass is the key

    @Autowired
    private ResourceLoader resourceLoader;
    @Value("${server.yangModel}")
    private String yangModel;

    @PostConstruct
    public void load() {
        log.info("Begin to load BOM Meta info.");
        try {
            org.springframework.core.io.Resource[] resources = ResourcePatternUtils
                    .getResourcePatternResolver(resourceLoader).getResources(PATTERN);
            for (org.springframework.core.io.Resource jsonResource : resources) {
                CsvMapper mapper = new CsvMapper();
                CsvSchema bootstrapSchema = CsvSchema.emptySchema().withHeader();
                InputStream csvFile;
                try {
                    csvFile = jsonResource.getInputStream();
                    MappingIterator<BomMetaInfo> it = mapper
                            .readerFor(BomMetaInfo.class)
                            .with(bootstrapSchema)
                            .readValues(csvFile);
                    String fileName = jsonResource.getFilename();
                    if (!isLoadableYangModel(fileName)) {
                        log.debug("Not load the csv file: {} not contains:{}", fileName,yangModel);
                        continue;
                    }
                    log.info("Loading the csv file: {}", fileName);
                    while (it.hasNextValue()) {
                        BomMetaInfo bomMetaInfo = it.nextValue();
                        String key = createKeyByFileName(fileName, bomMetaInfo.getEquipTypeConfiged(), bomMetaInfo.getCardClass());
                        bomMetaMap.put(key, bomMetaInfo);
                        log.debug("Stored bomMeta by the key: {}", key);
                    }
                } catch (Exception e) {
                    log.error("Failed to load BOM Meta info json file by resource: {}", jsonResource, e);
                    continue;

                }
            }
        } catch (IOException e) {
            log.error("Failed to load BOM Meta Info from resource.", e);
        }
    }

    /**
     * vendorName-vendorType-equipTypeConfiged is the key ; For TRANSCEIVER-C, vendorName-vendorType-equipTypeConfiged-CardClass is the key
     *
     * @param fileName
     * @param equipTypeConfiged
     * @param cardClass
     * @return
     */
    private String createKeyByFileName(@NonNull String fileName, @NonNull String equipTypeConfiged, String cardClass) {
        String prefix = fileName.substring(0, fileName.lastIndexOf("-")).toUpperCase();

        return createKey(prefix, equipTypeConfiged, cardClass);
    }

    private boolean isLoadableYangModel(String fileName) {
        if (containsYangModelSegment(fileName, yangModel)) {
            return true;
        }
        return BYTEDANCE_RESOURCE_MODEL.equals(yangModel)
                && containsYangModelSegment(fileName, BYTEDANCE_2_YANG_MODEL);
    }

    private boolean containsYangModelSegment(String fileName, String model) {
        // Match the model as a filename segment so ByteDance does not accidentally load ByteDance2.0 files.
        return fileName.contains("-" + model + "-");
    }

    public String getKey(@NonNull String vendorName, @NonNull String vendorType,
            @NonNull String equipTypeConfiged, String cardClass) {
        String model = yangModel;
        if (ProductTypeResolver.isBone20ProductType(vendorName, vendorType)) {
            // Only COHERENT CHASSIS2.0 selects Bone2.0 resources; other product-types keep
            // the 2606-release BOM key rule: vendorName-productType-server.yangModel.
            model = BYTEDANCE_RESOURCE_MODEL;
        }
        String prefix = String.format("%s-%s-%s", vendorName, vendorType, model).toUpperCase();

        return createKey(prefix, equipTypeConfiged, cardClass);
    }

    private String createKey(@NonNull String prefix, @NonNull String equipTypeConfiged, String cardClass) {
        StringBuilder key = new StringBuilder(prefix).append("-").append(equipTypeConfiged);
        if (equipTypeConfiged.equals("TRANSCEIVER-C")) {
            key.append("-").append(cardClass);
        }
        return key.toString();
    }


    public BomMetaInfo getBomMetaInfo(String key) {
        BomMetaInfo bomMetaInfo = bomMetaMap.get(key);
        //todo: for those have no config in BOM config, generate fake bomInfo automatically for LINECARD, as the testing use.
        String[] keyArrays = key.split("-");
        if (bomMetaInfo == null) {
            log.warn("Failed to get bom info by key:{}, create a default onw.", key);
            bomMetaInfo = new BomMetaInfo();
            bomMetaInfo.setCardClass(keyArrays[keyArrays.length - 1]);
            bomMetaInfo.setComponentType("LINECARD");
//            bomMetaInfo.setName("DWDM板卡");
            bomMetaInfo.setSerialNo(key);
            bomMetaInfo.setEquipTypeConfiged(keyArrays[keyArrays.length - 1]);
//            bomMetaInfo.setEquipType("DWDM");

        }
        return bomMetaInfo;
    }
}
