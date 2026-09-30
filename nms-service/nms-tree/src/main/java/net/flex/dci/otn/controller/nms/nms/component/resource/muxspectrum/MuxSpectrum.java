package net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum;

import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.nms.nms.dto.MuxSpectrumDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;

import java.util.List;

/**
 * @version 1.0
 * @date 2022/6/23 13:47
 */
public interface MuxSpectrum {

    MuxSpectrumDto getOpcNeSpectrum(String neId);


    MuxSpectrumDto getSiteLinkSpectrum(Link siteLink);

    MuxSpectrumDto getSpectrumWithSiteLinks(List<String> siteLinkIds, GridType grid, WDM_Band band);

    MuxSpectrumDto getEquipRefSpectrum(String neId, String equipId);
}
