package net.flex.dci.otn.controller.nms.nms.component.retrieve;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.enums.RetrieverHandlerType;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 2026/4/19
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class NmsRetrieverOperationsResolver {


    private final List<RetrieverHandlerType> sortedRetrieverHandlerTypes;

    private final ApplicationContext applicationContext;

    public NmsRetrieverOperationsResolver(
            ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.sortedRetrieverHandlerTypes = Arrays.stream(RetrieverHandlerType.values())
                .sorted(Comparator.comparingInt(RetrieverHandlerType::getOrder))
                .collect(Collectors.toList());
    }

    public INMSRetrieveOperations resolve(RetrieveTopologyDto dto) {
        log.info(
                "Resolving handler for: topology={}, node={}, rack={}, equip={}, tp={}, link={}, tunnel={}, retrieveType={}",
                dto.getTopologyRef(),
                dto.getNodeRef(),
                dto.getRackRef(),
                dto.getEquipRef(),
                dto.getTpRef(),
                dto.getLinkRef(),
                dto.getTunnelRef(),
                dto.getRetrieveType());

        // 参数校验
        if (dto.getTopologyRef() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "topology-ref is mandatory.");
        }

        if (dto.getRetrieveType() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "retrieve-type is mandatory.");
        }

        RetrieverHandlerType matchedType = sortedRetrieverHandlerTypes.stream()
                .filter(type -> type.getRetrieveType() == dto.getRetrieveType())
                .filter(type -> type.match(dto, applicationContext))
                .findFirst()
                .orElse(null);

        if (matchedType != null) {
            log.debug("Matched handler type: {}", matchedType.name());
            return matchedType.getHandler(applicationContext);
        }

        log.warn("No handler matched for dto: retrieveType={}, topology={}",
                dto.getRetrieveType(), dto.getTopologyRef());
        return new DefaultRetrieveOperations();
    }
}
