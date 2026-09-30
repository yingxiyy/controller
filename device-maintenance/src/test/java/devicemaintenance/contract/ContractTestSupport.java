package devicemaintenance.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import devicemaintenance.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Assertions;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

final class ContractTestSupport {

    private ContractTestSupport() {
    }

    static final ObjectMapper CONTRACT_OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    static MockMvc standaloneMvc(Object controller) {
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(CONTRACT_OBJECT_MAPPER);
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    static void assertSnapshot(String snapshotName, String actualRawBody) throws Exception {
        String expectedRawBody = loadSnapshot(snapshotName);
        String normalizedExpected = normalizeDynamicFields(expectedRawBody);
        String normalizedActual = normalizeDynamicFields(actualRawBody);
        Assertions.assertEquals(normalizedExpected, normalizedActual,
                "Snapshot mismatch for " + snapshotName);
    }

    private static String loadSnapshot(String snapshotName) throws Exception {
        ClassPathResource resource = new ClassPathResource("contract/snapshots/" + snapshotName + ".json");
        try (InputStream in = resource.getInputStream();
             Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private static String normalizeDynamicFields(String raw) {
        String normalized = raw;
        normalized = normalized.replaceAll("\"timestamp\"\\s*:\\s*\\d+", "\"timestamp\":\"<dynamic>\"");
        normalized = normalized.replaceAll("\"timestamp\"\\s*:\\s*\"[^\"]+\"", "\"timestamp\":\"<dynamic>\"");
        return normalized;
    }
}

