package net.flex.dci.otc.controller.otdr.model;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.ScanWavelength;

/**
 * 2025/8/3
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@Slf4j
public class OTDRScanParameters implements Serializable {

    //reflection-threshold
    private String reflectionThreshold;
    //splice-loss-threshold
    private String spliceLossThreshold;
    //end-of-fiber-threshold
    private String endOfFiberThreshold;
    //pulse-width
    private Long pulseWidth;

    //distance-range
    private Long distanceRange;

    //sampling-resolution
    private String samplingResolution;
    //scan-time
    private Long scanTime;

    //scan-wavelength
    private String scanWaveLength;

    //refractive-index
    private String refractiveIndex;


    public static OTDRScanParameters parseOTDRScanParameterByInput(StartOtdrInput startOtdrInput) {
        log.debug("parse OTDR Scan paramter by input:{}", startOtdrInput);
        String refractiveIndex = startOtdrInput.getRefractiveIndex();
        String reflectionThreshold = startOtdrInput.getReflectionThreshold();
        String spliceLossThreshold = startOtdrInput.getSpliceLossThreshold();
        String endOfFiberThreshold = startOtdrInput.getEndOfFiberThreshold();
        Long pulseWidth = startOtdrInput.getPulseWidth();
        Long distanceRange = startOtdrInput.getDistanceRange();
        String samplingResolution = startOtdrInput.getSamplingResolution();
        Long scanTime = startOtdrInput.getScanTime();
        ScanWavelength scanWaveLength = startOtdrInput.getScanWavelength();

        return OTDRScanParameters.builder()
                .scanTime(scanTime)
                .distanceRange(distanceRange)
                .endOfFiberThreshold(endOfFiberThreshold)
                .pulseWidth(pulseWidth)
                .samplingResolution(samplingResolution)
                .scanWaveLength(scanWaveLength == null ? null : scanWaveLength.name())
                .refractiveIndex(refractiveIndex)
                .reflectionThreshold(reflectionThreshold)
                .spliceLossThreshold(spliceLossThreshold)
                .build();
    }
}
