package net.flex.dci.otn.controller.subnet.manager.dto.view;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/2/11
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ViewTopologyMigrationDto implements Serializable {


    private List<ViewLinkMigrationDto> viewLinkMigrationDtoList;

    private List<ViewNodeMigrationDto> viewNodeMigrationDtoList;

}
