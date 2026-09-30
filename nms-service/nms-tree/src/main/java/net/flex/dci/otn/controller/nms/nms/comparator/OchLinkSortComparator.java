package net.flex.dci.otn.controller.nms.nms.comparator;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.flex.dci.otc.common.util.NeYangModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

/**
 * @version 1.0
 * @date 10/17/2023 4:38 PM
 */
public class OchLinkSortComparator implements Comparator<Link> {

    public NeYangModel neYangModel;

    public OchLinkSortComparator(NeYangModel neYangModel) {
        this.neYangModel = neYangModel;
    }

    @Override
    public int compare(Link link1, Link link2) {
        int aId = getIndex(link1);
        int bId = getIndex(link2);
        return aId - bId;
    }

    private int getIndex(Link ochLink) {
        if (ochLink.getSupportingLink() != null) {
            for (SupportingLink sLink : ochLink.getSupportingLink()) {
                Pattern pattern = Pattern.compile(neYangModel.muxPortMatchingRegex());
                Matcher matcher = pattern.matcher(sLink.getLinkRef().getValue());
                if (matcher.find()) {
                    String name = matcher.group(0);
                    String[] ids = name.split(neYangModel.muxChannelIdKeyword());
                    return Integer.parseInt(ids[1]);
                }
            }
        }
        return 0;
    }
}
