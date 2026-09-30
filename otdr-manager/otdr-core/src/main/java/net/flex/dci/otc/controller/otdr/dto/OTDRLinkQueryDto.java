package net.flex.dci.otc.controller.otdr.dto;

import java.io.Serializable;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.db.jpa.service.dao.dto.otdr.OTDRPagedQueryDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.Provider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * 2026/9/24
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OTDRLinkQueryDto implements Serializable {

    private OTDRPagedQueryDto otdrPagedQueryDto;

    private Provider provider;

    private Map<String, Node> nodeMap;

    private String phyLinkId;

}
