package net.flex.dci.otn.controller.pm;

import static net.flex.dci.otn.controller.utils.CommonUtils.partitionList;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otn.controller.properties.PmUploadProperties;
import org.springframework.stereotype.Component;

/**
 * 2026/6/10
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class PmService {

    private final PmUploadProperties pmUploadProperties;

    private final FTPClient ftpClient;

    private final NeManagerRpc neManagerRpc;

    public void uploadHistoryPm(List<String> neIds) {
        log.info("start to upload history pm total nes:{}", neIds.size());
        int maxConcurrentNe = pmUploadProperties.getMaxConcurrentNe();
        List<List<String>> nePartition = partitionList(neIds, maxConcurrentNe);

    }
}
