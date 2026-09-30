package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MUX;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MUX.FrequencyRange;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.Spectrum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;

/**
 * @version 1.0
 * @date 2022/6/23 13:53
 */
@Data
@Builder
public class MuxSpectrumDto implements Serializable {

    private GridType gridType;

    private List<Spectrum> spectrumList;

    private FrequencyRange frequencyRange;

    private MUX mux;
}
