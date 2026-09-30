import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.base.core.SimpleMongoDao;
import net.flex.dci.otn.controller.db.monitor.DbMonitorApplication;
import net.flex.dci.otn.controller.db.monitor.core.dto.Property;
import net.flex.dci.otn.controller.db.monitor.utils.PhysicalPropertyTool;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @version 1.0
 * @date 2022/11/16 14:08
 */
@SpringBootTest(classes = DbMonitorApplication.class)
@Slf4j
public class DbMonitorTest {

    @Autowired
    private SimpleMongoDao simpleMongoDao;


    @Test
    public void test() {
        List<Property> properties = new ArrayList<>();

        PhysicalPropertyTool.putKeyValue(properties, "test,", "test");
        PhysicalPropertyTool.putKeyValue(properties, "test1", "test");

        System.out.println(properties);

        List<String> result = split("/a/b");
    }

    public List<String> split(String s) {
        List<String> ret = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            if (ch == '/') {
                ret.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        ret.add(cur.toString());
        return ret;
    }


}
