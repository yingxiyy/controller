package net.flex.dci.otn.controller.nms.nms.enums;

import lombok.Getter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;

/**
 * 2026/7/6
 *
 * @author musa
 * @version 1.0
 **/
@Getter
public enum RouteHopType {
    LINK("LINK", Link.class),
    TP("TP", Tp.class);

    private String value;

    private Class<?> clazz;

    RouteHopType(String value, Class<?> clazz) {
        this.value = value;
        this.clazz = clazz;
    }

    public static Class<?> getRouteHopTypeClass(String value) {
        for (RouteHopType routeHopType : RouteHopType.values()) {
            if (routeHopType.getValue().equals(value)) {
                return routeHopType.getClazz();
            }
        }
        return null;
    }

    public static RouteHopType fromClazz(Class<?> clazz) {
        for (RouteHopType routeHopType : RouteHopType.values()) {
            if (routeHopType.getClazz().equals(clazz)) {
                return routeHopType;
            }
        }
        return null;
    }
}
