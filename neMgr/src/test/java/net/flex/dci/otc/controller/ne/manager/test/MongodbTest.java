package net.flex.dci.otc.controller.ne.manager.test;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.NeManagerApplication;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @version 1.0
 * @date 11/21/2023 2:26 PM
 */
@SpringBootTest(classes = NeManagerApplication.class)
@Slf4j
@RunWith(SpringRunner.class)
public class MongodbTest {

    @Autowired
    private TerminationPointDao terminationPointDao;

    @Test
    public void test() {
        terminationPointDao.batchRemoveConfigTerminationPointByIds(
                "Site-1673077370735#Ne-1676199324917",
                Arrays.asList("Site-1673077370735#Ne-1676199324917#LINECARD-1-1#PORT-1-1-C1",
                        "Site-1673077370735#Ne-1676199324917#LINECARD-1-1#PORT-1-1-C4"));
    }
}
