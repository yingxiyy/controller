package net.flex.dci.otc.controller.status.alarm.service.extractor;

import static net.flex.dci.otc.controller.status.util.NmlKeyHelper.getNmlKeysForTp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.RackDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 * @date 2022/1/12 14:43
 */
@Slf4j
public abstract class AbstractExtractor implements IExtractor {

    @Autowired
    protected PhyLinkDao phyLinkDao;

    @Autowired
    protected SiteLinkDao siteLinkDao;

    @Autowired
    protected TunnelDao tunnelDao;

    @Autowired
    protected OchLinkDao ochLinkDao;

    @Autowired
    protected SiteNodeDao siteNodeDao;

    @Autowired
    protected RackDao rackDao;


    public AbstractExtractor() {
//        this.phyLinkDao = SpringBeanFinder.getBean(PhyLinkDao.class);
//        this.siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
//        this.tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);
//        this.ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
//        this.rackDao = SpringBeanFinder.getBean(RackDao.class);
//        this.siteNodeDao = SpringBeanFinder.getBean(SiteNodeDao.class);
    }


    protected Set<String> getPhyLinkNmlKeys(String phyLinkId) {
        Link phyLink = phyLinkDao.getPhyLinkById(phyLinkId);
        if (phyLink == null) {
            return null;
        }
        String srcTpId = phyLink.getSource().getSourceTp().getValue();
        String dstTpId = phyLink.getDestination().getDestTp().getValue();
        Set<String> set = new HashSet<>();
        set.addAll(getNmlKeysForTp(srcTpId));
        set.addAll(getNmlKeysForTp(dstTpId));
        set.add(phyLinkId);
        return set;
    }

//    protected Set<String> getNmlKeysForTp(String tpId) {
////        String arr[] = tpId.split("#");
////        String neId = arr[0] + "#" + arr[1];
////        String equipId = neId + "#" + arr[2];
//        log.debug("get tp relative nml key :{}", tpId);
//        String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);
//        String transceiverId = PhysicalTpIdNamingRule.getTransceiverIdWithNamingRule(tpId);
//        Set<String> set = new HashSet<String>();
////		set.add(neId);
//        set.add(transceiverId);
//        set.add(equipId);
//        set.add(tpId);
//        if (isLinePortNeedsSigMapping(tpId)) {
//            String sigTpId = convertLineToSig(tpId);
//            set.add(sigTpId);
//            log.debug("Mapped LINE port {} to SIG logical port {}", tpId, sigTpId);
//        }
//        return set;
//    }
//
//    private String convertLineToSig(String tpId) {
//        return tpId.replaceFirst(LINE_PORT_REGEX, SIG_PORT_SUFFIX);
//    }
//
//    private boolean isLinePortNeedsSigMapping(String tpId) {
//        return tpId.endsWith(LINE_PORT_SUFFIX);
//    }

    protected Set<String> getSiteLinkNmlKeys(String siteLinkId) {
        Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
        Set<String> set = new HashSet<>();
        List<SupportingLink> supportingLinkList = siteLink.getSupportingLink();
        if (supportingLinkList != null) {
            for (SupportingLink supportingLink : supportingLinkList) {
                String phyLinkId = supportingLink.getLinkRef().getValue();
                set.addAll(getPhyLinkNmlKeys(phyLinkId));
            }
        }
        set.add(siteLinkId);
        return set;
    }

    protected Set<String> getOchNmlKeys(String ochLinkId) {
        Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink> supportingLinkList = ochLink.getSupportingLink();
        Set<String> ochSet = new HashSet<>();
        if (supportingLinkList != null) {
            for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink supportingLink : supportingLinkList) {
                String linkId = supportingLink.getLinkRef().getValue();
                Set<String> set = getPhyLinkNmlKeys(linkId);
                if (set == null) {
                    set = getSiteLinkNmlKeys(linkId);
                }
                ochSet.addAll(set);
            }
        }
        return ochSet;
    }


}
