package net.flex.dci.otc.controller.otdr.service;

import net.flex.dci.otc.controller.otdr.model.LatestOtdrResOutput;
import net.flex.dci.otc.controller.otdr.model.breifly.OtdrBrieflyResultOutput;
import net.flex.dci.otc.controller.otdr.model.graphics.ShowOtdrResOutput;
import net.flex.dci.otc.controller.otdr.model.otsLink.GetOmsLinkOtdrLatestResultOutputDto;

/**
 * @version 1.0
 * @date 2022/8/31 17:19
 */
public interface OtdrResultService {

    LatestOtdrResOutput getOtdrLatestResult(String input);

//    Object getOTDRResults(String input);

    Object getOTDRDetail(String requestBody);

    ShowOtdrResOutput showOtdrResultGraphics(String requestBody);

    OtdrBrieflyResultOutput getOTDRBrieflyResults(String requestBody);

    GetOmsLinkOtdrLatestResultOutputDto getOMSLinkOtdrLatestResultOutput(String requestBody);
}
