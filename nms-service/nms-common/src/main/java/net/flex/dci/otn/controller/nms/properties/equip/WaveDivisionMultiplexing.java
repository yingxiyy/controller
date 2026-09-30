package net.flex.dci.otn.controller.nms.properties.equip;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 2025/7/4
 *
 * @author musa
 * @version 1.0
 **/
@Data
public class WaveDivisionMultiplexing implements Serializable {

    private String description;

    private List<String> equipmentTypes;
}
