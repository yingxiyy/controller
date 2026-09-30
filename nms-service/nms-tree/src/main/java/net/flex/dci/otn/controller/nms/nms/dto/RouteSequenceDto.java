package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import net.flex.dci.otn.controller.nms.nms.enums.RouteHopType;

/**
 * @version 1.0
 * @date 2022/6/29 11:15
 */
@Data
public class RouteSequenceDto implements Serializable {

    //    private RouteSequenceDto next;
//
    private String topologyRef;


    private Boolean isVirtual = false;

//    private List<CrossConnections> xcs = new ArrayList<>();

    private List<String> xcIds = new ArrayList<>();

    private String linkId;

    private String tpId;

//    private Class<?> clazz;

    private RouteHopType routeHopType;

    private RouteSequenceDto prev;

    private RouteSequenceDto primary;

    private RouteSequenceDto secondary;

    private List<RouteSequenceDto> tertiary;

    private RouteSequenceDto subSequenceDto; // subsequenceDto

    @Override
    public String toString() {
        return "linkid is:" + linkId + " tp id is:" + tpId;
    }
}
