package net.flex.dci.otn.ne.upgrade.tool.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NeIdListReader {

    private final ObjectMapper objectMapper;
    @Getter
    private List<String> neIdList;

    @PostConstruct
    public void init() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/neUpperIp.json");

        Map<String, List<String>> data = objectMapper.readValue(
                inputStream,
                new TypeReference<Map<String, List<String>>>() {
                }
        );

        this.neIdList = data.get("neIdList");
        log.info("Loaded {} neIds", neIdList.size());
    }

}