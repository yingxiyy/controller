package net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

/**
 * @version 1.0
 * @date 2022/12/6 16:42
 */
@Data
@Builder
public class ThumbnailSequenceDto implements Serializable {

    private ThumbnailSequenceDto primary;
    private ThumbnailSequenceDto secondary;
    private ThumbnailSequenceDto tertiary;
    private String nodeId;
    private String tpId;
    private String linkId;
    private TopologyId topologyRef;
    private Class<?> refClazz;

    @Tolerate
    public ThumbnailSequenceDto() {
    }


}
