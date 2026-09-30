package net.flex.dci.otc.controller.otdr.manager;

import java.math.BigInteger;
import net.flex.dci.otc.controller.otdr.model.LatestOtdrResOutput;
import net.flex.dci.otc.controller.otdr.model.OtdrResultOutput;
import net.flex.dci.otc.controller.otdr.model.breifly.OtdrBrieflyResultOutput;
import net.flex.dci.otc.controller.otdr.model.otsLink.GetOmsLinkOtdrLatestResultOutputDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrDetailInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultsOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkRole;

/**
 * @version 1.0
 * @date 2022/9/1 11:27
 */
public interface OtdrResultManager {

    OtdrResultOutput getLatestOtdrResult(String monitorTpId);


    GetOtdrResultsOutput getOtdrResultsPaged(GetOtdrResultsInput input);

    String getOtdrResultDetail(GetOtdrDetailInput getOtdrDetailInput);

    LatestOtdrResOutput getLatestOTDRResult(String monitorTpId);

    void setOtdrBaseBenchmark(String linkId, BigInteger taskId);

    OtdrBrieflyResultOutput getOtdrBrieflyResultsPaged(GetOtdrResultsInput getOtdrResultsInput);

    GetOmsLinkOtdrLatestResultOutputDto getOMSRefOtsLinkLatestOtdrResult(String value,
            LinkRole linkRole);
}
