//package net.flex.dci.otn.controller.nms.filter;
//
//import static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType._0;
//import static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType._100;
//import static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType._50;
//import static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType._75;
//
//import java.util.LinkedList;
//import java.util.List;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.Scope;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.ScopeBuilder;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.ScopeKey;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
//
///**
// * @version 1.0
// * @date 2022/9/5 15:30
// */
//public class SpectrumUtils {
//
//    public List<Scope> getScopes(Integer start, Integer end, int width, short ochIndex) {
//        List<Scope> scopes = new LinkedList<>();
//        Integer step = end;
//        end = end + width;
//        switch (this.grid) {
//            case _50:
//                step = 50000;
//                break;
//            case _75:
//                step = 75000;
//                break;
//            case _100:
//                step = 100000;
//                break;
//            case _0:
//                step = 6250;
//                break;
//        }
//        for (int pos = start; pos >= end; pos -= step) {
//            ScopeBuilder sb = new ScopeBuilder();
//            sb.setLower(pos - width);
//            sb.setUpper(pos);
//            sb.setCentre(pos - width / 2);
//            if (getGrid().equals(_0)) {
//                sb.setIndex(ochIndex);
//            } else {
//                sb.setIndex(getIndex(sb.getCentre()));
//            }
//            sb.setImplementState(ImplementState.Plan);
//            sb.setKey(new ScopeKey(sb.getCentre()));
//
//            scopes.add(sb.build());
//        }
//
//        return scopes;
//    }
//
//    private Short getIndex(Integer centre) {
//        Long key = Long.valueOf(centre);
//        if (centreFreq.containsKey(key)) {
//            return centreFreq.get(key).shortValue();
//        } else {
//            return 222;
//        }
//    }
//
//}
