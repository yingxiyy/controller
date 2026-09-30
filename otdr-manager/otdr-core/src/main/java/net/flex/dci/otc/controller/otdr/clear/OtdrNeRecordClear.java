package net.flex.dci.otc.controller.otdr.clear;

import java.util.List;
import net.flex.dci.otn.db.jpa.entity.OtdrResultRecord;

/**
 * @version 1.0
 * @date 2022/9/2 12:33
 */
public interface OtdrNeRecordClear {

    void clearNeRecord(String nodeId, List<OtdrResultRecord> records);

}
