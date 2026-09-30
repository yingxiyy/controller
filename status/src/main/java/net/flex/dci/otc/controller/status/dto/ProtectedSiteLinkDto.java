package net.flex.dci.otc.controller.status.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.controller.status.common.ProtectedSiteLinkWorkModel;

/**
 * @version 1.0
 * @date 2022/4/10 21:06
 */
@Data
@Builder
@AllArgsConstructor
public class ProtectedSiteLinkDto implements Serializable {

    private String siteLinkId;

    private List<String> switchPortId;

    private List<String> selectedPortId;

    private ProtectedSiteLinkWorkModel workModel;

}
