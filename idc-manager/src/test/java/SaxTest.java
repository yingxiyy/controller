import java.io.InputStream;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;

/**
 * @version 1.0
 * @date 2022/1/19 14:35
 */
public class SaxTest {

    public static void main(String[] args) {
        String filename = "c:\\tmp\\computer_room_template.xlsx";
        OPCPackage pkg;
        try {
            pkg = OPCPackage.open(filename);
            XSSFReader r = new XSSFReader(pkg);
            //查看转换的xml原始文件，方便理解后面解析时的处理,
            InputStream in = r.getSheet("rId1");
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) != -1) {
                System.out.write(buf, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
