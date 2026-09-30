package net.flex.dci.otn.controller.allocate.network.bytedance.ase;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.common.util.CommonUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class InjectAseImpl {

    private static final String DEFAULT_BAND = "C";

    private Link siteLink;
    private List<String> otmNodeList;

    private ZkResourceLock locker;
    private RouteInfo rInfo;
    private NeYangModel yangModel;

    private ChangedObject changedObject;
    private MultipleTransaction mongoTransaction;


    public InjectAseImpl(String siteLinkId, ChangedObject changedObject) {
        this.changedObject = changedObject;
        this.siteLink = changedObject.getChangedSiteLink(siteLinkId);
        this.otmNodeList = new ArrayList<>();
    }

    public void start() {
        log.debug("start inject ASE action on sitelink");

        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        if (!siteLinkAttr.getGrid().equals(GridType._0)) {
            log.error("not a flex grid, not supported on ASE");
            return;
        }
        yangModel = CommonUtils.getYangModelInProperties(siteLinkAttr.getProperties());

        rInfo = new RouteInfo();
        rInfo.parse(siteLinkAttr.getExplictRoute().getRoute());

        log.info("start to inject ASE on sitelink: {} {}", siteLink.getLinkId(), siteLinkAttr.getFriendlyName());
        //create fakeWaveOch
        createAseOchLink();


        log.debug("inject ASE related data ready.");
     }

    private void createAseOchLink() {
        WDM_Band band = getWDMBand(siteLink);

        List<Available> initialAvaList = FrequencyAvailable.getInitializedAvailableList(yangModel, band, GridType._100);

        findoutOTM();

        String srcTp = siteLink.getSource().getSourceTp().getValue();
        String dstTp = siteLink.getDestination().getDestTp().getValue();
        String ochSrcTp = getExp33(srcTp);
        String ochDstTp = getExp33(dstTp);

        initialAvaList.forEach(ava->createAseOchLink(ochSrcTp, ochDstTp, ava));
    }

    private WDM_Band getWDMBand(Link siteLink) {
        String bandStr = siteLink.getAugmentation(Link1.class).getSite().getLinkGroup();
        // 如果对应的 band 为空，则设置默认值
        if (bandStr == null) {
            bandStr = DEFAULT_BAND;
        }
        return WDM_Band.fromString(bandStr);
    }

    private void createAseOchLink(String ochSrcTp, String ochDstTp, Available ava) {
        Link ochLink = new DummyOchLinkConstructor(changedObject, siteLink.getLinkId()).create(ochSrcTp, ochDstTp, ava, rInfo.getXcIdList(), otmNodeList);
        insertIntoSiteLink(ochLink);
    }

    private void findoutOTM() {
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        siteLinkExternalChecking(siteLinkAttr.getAExternal().getAddDropLink());
        siteLinkExternalChecking(siteLinkAttr.getZExternal().getAddDropLink());
    }

    private void siteLinkExternalChecking(List<AddDropLink> addDropLink) {
        if (addDropLink.get(0).getConnnectorType().name().contains(EquipType.MUX.name())) {
            String aTpId = PhysicalLinkIdNamingRule.getTpAId(addDropLink.get(0).getLinkRef());
            String zTpId = PhysicalLinkIdNamingRule.getTpZId(addDropLink.get(0).getLinkRef());

            String muxTpId;
            if (aTpId.contains(EquipType.MUX.name())) {
                muxTpId = aTpId;
            } else {
                muxTpId = zTpId;
            }
            String nodeId = PhysicalTpIdNamingRule.getNodeId(muxTpId);
            otmNodeList.add(nodeId);
        }
    }

    private void insertIntoSiteLink(Link ochLink) {
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        List<String> ochLinkIdList = siteLinkAttr.getDummyLink();
        if (ochLinkIdList == null) {
            ochLinkIdList = new ArrayList<>();
        }
        ochLinkIdList.add(ochLink.getLinkId().getValue());

        Link newSiteLink = new LinkBuilder(siteLink).addAugmentation(Link1.class, new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                .setSite(new SiteBuilder(siteLinkAttr).setDummyLink(ochLinkIdList).build())
                .build()).build();
        siteLink = newSiteLink;

        changedObject.addChangedSiteLink(siteLink);
    }

    private String getExp33(String tpId) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
        Node node = changedObject.getChangedPhyNode(nodeId);
        Optional<TerminationPoint> tpOp = node.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().contains("EXP33") && tp.getTpId().getValue().contains(eqId)).findAny();
        if (!tpOp.isPresent()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, String.format("cannot find EXP 33 on eq %s", eqId));
        }
        return tpOp.get().getTpId().getValue();
    }


}
