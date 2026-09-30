package net.flex.dci.otn.controller.schedule.configuration;

import static org.junit.jupiter.api.Assertions.*;

import net.flex.dci.otc.serialization.JsonUtil;
import net.flex.dci.otc.serialization.autoconfigure.SerializationAutoConfigure;
import net.flex.dci.otc.serialization.util.SerializeUtil;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ListInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.PutOutputBuilder;

class ScheduleSchemaTest {
    @Test
    void ftpSchemaInitializesWithoutDeviceTopology() throws Exception {
        JsonUtil json = new SerializationAutoConfigure().initJsonUtil();
        assertNotNull(json);
        // Explicit RPC names avoid the production controller stack/annotation lookup in a unit test.
        ListInput input = SerializeUtil.parseRpcInput("ftp-server", "list", "{\"input\":{\"server-name\":\"localhost\",\"folder-name\":\"controller/db/backup\"}}", ListInput.class);
        assertEquals("localhost", input.getServerName());
        assertEquals("controller/db/backup", input.getFolderName());
        assertTrue(SerializeUtil.serializeDataObject("ftp-server", "put", new PutOutputBuilder().setResult("success").build()).contains("success"));
    }
}
