package net.flex.dci.otn.controller.resource.statistic.rest.equipment;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import net.flex.dci.otn.controller.resource.statistic.rest.BaseInfo;

/**
 * 2026/1/29
 *
 * @author musa
 * @version 1.0
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
public class CardInfo extends BaseInfo implements Serializable {


    private String cardId;

    private String cardName;

    private String cardType;

    private String vendorName;

    private String partName;

    private String shelf;

    private String slot;

    private String pn;

    private String sn;

    private String hwVersion;

    private String swVersion;

    private String fwVersion;

    private String mfgDate;

    private String description;

    private String asset_management_code;


}
