package net.flex.dci.otn.controller.subnet.manager.dto.view;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/2/13
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ViewLinkRefConnectionDto implements Serializable {

    private String viewLinkId;

    private String sourceSite;

    private String destSite;

    private List<String> refConnectionIds;

}
