package net.flex.dci.otc.controller.otdr.components;

import java.math.BigInteger;
import java.util.List;
import net.flex.dci.otc.controller.otdr.dto.OTDRLinkQueryDto;
import net.flex.dci.otc.controller.otdr.model.breifly.OtdrBrieflyResult;
import net.flex.dci.otc.controller.otdr.model.link.WrappedLink;
import net.flex.dci.otc.controller.otdr.model.otsLink.OtsLinkOtdrLatestResult;
import net.flex.dci.otc.controller.otdr.topology.OTDRTopoHolder;
import net.flex.dci.otn.db.jpa.entity.OtdrResultRecord;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.get.otdr.results.output.Result;
import org.springframework.data.domain.Page;

/**
 * @version 1.0
 * @date 2023/1/9 12:18
 */
public interface OTDRResult {

    Page<Result> listByQueryItemPaged(OTDRLinkQueryDto pagedQueryDto);

    OtdrResultRecord getOtdrDetailResultByTaskId(BigInteger taskId);

    Page<OtdrBrieflyResult> listResultByQueryItemPaged(OTDRLinkQueryDto pagedQueryDto);

    List<OtsLinkOtdrLatestResult> getOtsLinksOtdrLatestResult(List<WrappedLink> otsWrappedLinks,
            OTDRTopoHolder otdrTopoHolder);

}
