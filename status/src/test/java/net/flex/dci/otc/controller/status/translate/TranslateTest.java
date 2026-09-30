package net.flex.dci.otc.controller.status.translate;

import net.flex.dci.otc.controller.status.StatusApplication;
import net.flex.dci.otc.controller.status.alarm.utils.AlarmConverterUtils;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @version 1.0
 * @date 10/31/2023 1:19 PM
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StatusApplication.class)
public class TranslateTest {

    @Test
    public void translateTest() {
        AlarmRecord alarm = new AlarmRecord();
        alarm.setAlarmId("Site-1688835019101048832#Ne-1688854328921690112;11012290");
        alarm.setSeverity(AlarmSeverity.Critical);
        alarm.setResourceRef("TRANSCEIVER-1-1-L1");
        alarm.setComponentRef("PORT-1-1-L1");
        alarm.setAlarmText("Loss_of_Signal");
        alarm.setAlarmGroup("OTU_Port_Failure");
        alarm.setAlarmTypeId("LOS");
        String message = AlarmConverterUtils.getAlarmMessage(alarm);
        System.out.println(message);
    }
}
