package net.flex.dci.otn.controller.nms.nms.dto.omslink;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 8/11/2025 5:02 PM
 */
@Data
@Builder
public class OtsLinkAmplifierInfo implements Serializable {

    private String otsLinkId;

    private String otsLinkName;

    private OtsTerminalInfo source;

    private OtsTerminalInfo destination;

    private DirectionAmplifierInfo azC;

    private DirectionAmplifierInfo azL;

    private DirectionAmplifierInfo zaC;

    private DirectionAmplifierInfo zaL;

    private DirectionAmplifierInfo azPaC;

    private DirectionAmplifierInfo azPaL;

    private DirectionAmplifierInfo zaPaC;

    private DirectionAmplifierInfo zaPaL;

    private OtsLinkRamanInfo azRaman;

    private OtsLinkRamanInfo zaRaman;


    private boolean isAlign;
}
