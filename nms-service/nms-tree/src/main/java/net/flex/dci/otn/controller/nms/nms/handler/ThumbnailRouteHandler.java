package net.flex.dci.otn.controller.nms.nms.handler;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.component.thumbnail.DesignThumbnailRoute;
import net.flex.dci.otn.controller.nms.nms.component.thumbnail.ThumbnailRoute;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailRouteDto;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetDesignSimpleRouteInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetDesignSimpleRouteOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetDesignSimpleRouteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSimpleRouteInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSimpleRouteOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSimpleRouteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.TertiaryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/12/6 15:34
 */
@Component
@Slf4j
public class ThumbnailRouteHandler extends AbstractBaseHandler {


    private final ThumbnailRoute thumbnailRoute;

    private final DesignThumbnailRoute designThumbnailRoute;


    public ThumbnailRouteHandler(
            NetconfTopology netconfTopology, ThumbnailRoute thumbnailRoute,
            DesignThumbnailRoute designThumbnailRoute) {
        super(netconfTopology);
        this.thumbnailRoute = thumbnailRoute;
        this.designThumbnailRoute = designThumbnailRoute;
    }

    @Override
    public GetSimpleRouteOutput getSimpleRoute(GetSimpleRouteInput input) {
        log.debug("start to get the simple route,the input is:{}", input);
        String refLinkId = getRetrieveLinkId(input);
        ThumbnailRouteDto thumbnailRouteDto = thumbnailRoute.getThumbnailSequence(refLinkId);
        GetSimpleRouteOutputBuilder resultBuilder = new GetSimpleRouteOutputBuilder();
        resultBuilder.setPrimary(new PrimaryBuilder().setRouteSequence(
                thumbnailRouteDto.getPrimary().getRouteSequences()).build());
        if (thumbnailRouteDto.getSecondary() != null) {
            resultBuilder.setSecondary(
                    new SecondaryBuilder().setRouteSequence(thumbnailRouteDto.getSecondary()
                            .getRouteSequences()).build());
        }
        if (thumbnailRouteDto.getTertiary() != null) {
            resultBuilder.setTertiary(
                    new TertiaryBuilder().setRouteSequence(thumbnailRouteDto.getTertiary()
                            .getRouteSequences()).build());
        }
        //go on
        return resultBuilder.build();
    }

    @Override
    public GetDesignSimpleRouteOutput getDesignThumbnailRoute(GetDesignSimpleRouteInput input) {
        log.debug("start to get the design simple route ,the input is:{}", input);
        List<String> primary = input.getPrimary();
        List<String> secondary = input.getSecondary();
        List<String> tertiary = input.getThird();
        ThumbnailRouteDto thumbnailRouteDto = designThumbnailRoute.getDesignThumbnailSequence(
                primary,
                secondary, tertiary);
        GetDesignSimpleRouteOutputBuilder resultBuilder = new GetDesignSimpleRouteOutputBuilder();
        resultBuilder.setPrimary(new PrimaryBuilder().setRouteSequence(
                thumbnailRouteDto.getPrimary().getRouteSequences()).build());
        if (thumbnailRouteDto.getSecondary() != null) {
            resultBuilder.setSecondary(
                    new SecondaryBuilder().setRouteSequence(thumbnailRouteDto.getSecondary()
                            .getRouteSequences()).build());
        }
        if (thumbnailRouteDto.getTertiary() != null) {
            resultBuilder.setTertiary(
                    new TertiaryBuilder().setRouteSequence(thumbnailRouteDto.getTertiary()
                            .getRouteSequences()).build());
        }

        return resultBuilder.build();
    }

    private String getRetrieveLinkId(GetSimpleRouteInput input) {
        String refLinkId = null;
        LinkId linkId = input.getLinkRef();
        Uri tunnelRef = input.getTunnelRef();
        if (linkId == null && tunnelRef != null) {
            refLinkId = tunnelRef.getValue();
        } else if (linkId != null && tunnelRef == null) {
            refLinkId = linkId.getValue();
        } else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the request is invalid,please try again");
        }
        return refLinkId;
    }


}
