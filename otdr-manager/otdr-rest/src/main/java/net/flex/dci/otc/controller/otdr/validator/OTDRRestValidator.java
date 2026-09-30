package net.flex.dci.otc.controller.otdr.validator;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.SetOtdrBaseInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.ShowOtdrGraphicsInput;

/**
 * @version 1.0
 * @date 8/18/2023 4:42 PM
 */
public interface OTDRRestValidator {

    void validateShowOtdrResultGraphicsInput(ShowOtdrGraphicsInput input);

    void validateSetOtdrBaseBenchmarkInput(SetOtdrBaseInput setOtdrBaseInput);
}
