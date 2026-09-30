package net.flex.dci.otn.controller.nms.nms.dto.dimension;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/4/7
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class SiteDetails implements Serializable {

    private String siteId;

    private String siteName;

    private List<NeDetails> nes;

}
