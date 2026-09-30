/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.ne.Grid;
import net.flex.dci.otn.controller.allocate.ne.OcmGridGroup;
import net.flex.dci.otn.controller.allocate.ne.Protected;
import net.flex.dci.otn.controller.allocate.ne.SiteModel;
import net.flex.dci.otn.controller.allocate.ne.Type;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SiteModelConfigCL implements SiteModelConfigInterface{

    private static final String CONFIG_FILE = "siteModel.json";
    public static final String DEFAULT_KEY = "default";
    private SiteModel model;

    private Map<Integer, Grid> gridMap = new HashMap<>();
    private Map<String, Type> nodeTypeMap = new HashMap<>();
    private Map<String, List<String>> protectedMap = new HashMap<>();


    @PostConstruct
    public void loadFile() {
        log.info("Begin to load siteNode config info.");
        try {
            InputStream configFile = new ClassPathResource(CONFIG_FILE).getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            model = mapper.readValue(configFile, SiteModel.class);
            init();

        } catch (Exception e) {
            log.error("default setting hasn't found or content is an invalid JSON {}", e);
        }
    }

    private void init() {
        for (Grid grid : model.getGrid()) {
            gridMap.put(grid.getValue(), grid);
        }

        for (Type type : model.getType()) {
            nodeTypeMap.put(type.getName(), type);
        }
        for (Protected protectedItem : model.getProtected()) {
            protectedMap.put(protectedItem.getModel(), protectedItem.getCards());
        }

    }

    /**
     * The card must be in route order
     *
     * @param nodeType
     * @param grid
     * @param isProtected
     * @param linkModel
     * @return
     */
    public List<String> getMainCardTypes(String nodeType, Integer grid, @NonNull Boolean isProtected, @NonNull String linkModel) throws NeDesignerException {
        List<String> cardTypes = new ArrayList<>();

        Type typeInfo = nodeTypeMap.get(nodeType);
        if (typeInfo == null) {
            String msg = String.format("Failed to get type info by the nodeType: %s, from config file: %s", nodeType, CONFIG_FILE);
            log.error("msg");
            throw new NeDesignerException(msg);
        }

        //get mux card
        if (typeInfo.getHasMux()) {
            List<String> gridCards = getCardsByGrid(grid);
            cardTypes.addAll(gridCards);
        }

        //get protected Card
        if (isProtected && typeInfo.getHasProtected()) {

            //get wss card, hard corded, later can refactor
            if (nodeType.equals("R")) {
                cardTypes.add(typeInfo.getCards().get(0));
            }

            List<String> protectedCards;
            if (grid == 0) {
                protectedCards = protectedMap.get(String.valueOf(grid));
            } else {
                protectedCards = protectedMap.get(linkModel);
            }
            if (protectedCards == null || protectedCards.isEmpty()) {
                String msg = String.format("Failed to get protected card by grid : %d, linkModel:%s, from config file: %s", grid, linkModel, CONFIG_FILE);
                log.error(msg);
                throw new NeDesignerException(msg);
            }
            cardTypes.addAll(protectedCards);
        }
        if (!isProtected || !typeInfo.getHasProtected()) {
            //get node cards
            cardTypes.addAll(typeInfo.getCards());
        }

        return cardTypes;

    }

    private List<String> getCardsByGrid(Integer gridInput) throws NeDesignerException {
        List<String> gridCards = getGrid(gridInput).getCards();
        if (gridCards == null || gridCards.isEmpty()) {
            String msg = String.format("Failed to get grid card by grid : %d, from config file: %s", gridInput, CONFIG_FILE);
            log.error("msg");
            throw new NeDesignerException(msg);
        }
        return gridCards;
    }

    private Grid getGrid(Integer gridInput) throws NeDesignerException {
        Grid grid = gridMap.get(gridInput);
        if (grid == null) {
            String msg = String.format("Unsupported grid: %d, because failed to get grid from config file: %s", gridInput, CONFIG_FILE);
            log.error(msg);
            throw new NeDesignerException(msg);
        }
        return grid;
    }

    public List<String> getSlaveCardTypes(String nodeType, Integer grid, Boolean isProtected, @NonNull String linkModel) {
        Type typeInfo = nodeTypeMap.get(nodeType);
        //get protected card
        if (isProtected && typeInfo.getHasProtected() && typeInfo.getModels().contains(linkModel)) {
            if (typeInfo.getCards() != null && !typeInfo.getCards().isEmpty()) {
                return Arrays.asList(typeInfo.getCards().get(typeInfo.getCards().size() - 1));//目前所有slave card都只有一张OA卡，所以暂时hard coded，以后可以重新定义json，重构代码。
            }
        }
        return Collections.emptyList();
    }

    public OcmGridGroup getOcmGridGroup(Integer gridInput) throws NeDesignerException {
        if (gridInput != 0) {
            //需求变化，固定频率去掉ocm，免得数据过大
            return null;
        }
        OcmGridGroup ocmGridGroup = getGrid(gridInput).getOcmGridGroup();
        //ocm updater will do this workd
//        if (ocmGridGroup == null) {
//            String msg = String.format("Unsupported grid: %d, because failed to get UsedGripGroup from config file: %s", gridInput, CONFIG_FILE);
//            log.error(msg);
//            throw new NeDesignerException(msg);
//        }
        return ocmGridGroup;

    }
}
