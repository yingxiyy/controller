package net.flex.dci.otn.controller.resource.statistic.core.utils;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.opencsv.exceptions.CsvValidationException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import org.springframework.http.HttpHeaders;

/**
 * @version 1.0
 * @date 10/30/2025 1:55 PM
 */
@Slf4j
public class CsvUtil {


    public static List<String[]> readCsv(String filePath) {
        log.debug("read Csv file from filePath:{}", filePath);
        return readCsv(filePath, 0);
    }

    public static List<String[]> readCsv(String filePath, int skipLines) {
        List<String[]> records = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
            for (int i = 0; i < skipLines; i++) {
                csvReader.readNext();
            }
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                records.add(values);
                log.debug("read line:{}", Arrays.toString(values));
            }
            log.debug("success read csv file:{},total line:{}", filePath, records.size());
        } catch (IOException | CsvValidationException e) {
            log.error("read csv file failed,the reason is:{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return records;
    }

    public static <T> List<T> readCsv(String filePath, Class<T> clazz) {
        log.debug("read csv file from filePath:{} class is:{}", filePath, clazz);
        return readCsv(filePath, clazz, 1);
    }

    public static <T> List<T> readCsv(String filePath, Class<T> clazz, int skipLines) {
        try (FileReader reader = new FileReader(filePath)) {
            HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(clazz);
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withSkipLines(skipLines)
                    .build();
            List<T> result = csvToBean.parse();
            log.debug("success to reflect csv file to the bean:{},total:{}", clazz, result.size());
            return result;
        } catch (IOException e) {
            log.error("read csv file failed,the reason is:{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static void writeCsv(String filePath, List<String[]> data) {
        writeCsv(filePath, data, false);
    }

    public static void writeCsv(String filePath, List<String[]> data, boolean append) {
        createParentDirs(filePath);
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(filePath, append))) {
            csvWriter.writeAll(data);
            log.debug("success to write to csv file:{},total line:{}", filePath, data.size());
        } catch (IOException e) {
            log.debug("failed to write data to the csv file:{}", filePath, e);
            throw new RuntimeException("failed to write data to the csv file", e);
        }
    }

    public static void writeCsvRow(String filePath, String[] rowData, boolean append) {
        createParentDirs(filePath);
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(filePath, append))) {
            csvWriter.writeNext(rowData);
            log.debug("success to write to csv file:{},data:{}", filePath,
                    Arrays.toString(rowData));
        } catch (IOException e) {
            log.debug("failed to write to csv file:{}", filePath, e);
            throw new RuntimeException("failed to write to csv file", e);
        }
    }

    public static <T> void writeBeansToCsv(String filePath, List<T> bean) {
        createParentDirs(filePath);
        try (Writer writer = new FileWriter(filePath)) {
            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
                    .withApplyQuotesToAll(false)
                    .build();
            beanToCsv.write(bean);
            log.debug("success to write bean to the csv file:{} total:{}", filePath, bean.size());
        } catch (IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
            log.error("failed to write bean to the csv file:{}", filePath, e);
            throw new RuntimeException("failed to write bean to the csv file", e);
        }
    }

    /**
     * http export support
     */

    public static void exportCsvToHttpResponse(List<String[]> data, String fileName,
            HttpServletResponse response) {
        setupCsvResponse(response, fileName);
        try (Writer writer = new OutputStreamWriter(response.getOutputStream(),
                StandardCharsets.UTF_8)) {
            CSVWriter csvWriter = new CSVWriter(writer);
            csvWriter.writeAll(data);
            writer.flush();
            log.debug("http export csv success:{},total:{}", fileName, data.size());
        } catch (IOException e) {
            log.error("http export csv failed:{}", fileName, e);
            throw new RuntimeException("http export csv failed", e);
        }

    }

    public static <T> void exportBeansToHttpResponse(List<T> beans, String filename,
            HttpServletResponse response, Class<T> clazz) {
        setupCsvResponse(response, filename);
        try (Writer writer = new OutputStreamWriter(response.getOutputStream(),
                StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');
            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
                    .withApplyQuotesToAll(false)
                    .build();
            if (beans.isEmpty()) {
                T emptyInstance = clazz.getDeclaredConstructor().newInstance();
                beanToCsv.write(Collections.singletonList(emptyInstance));
            } else {
                beanToCsv.write(beans);
            }
            writer.flush();
            log.debug("success to export beans to the csv file:{} total:{}", filename,
                    beans.size());
        } catch (IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException |
                 InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            log.error("failed to export beans to the csv file:{}", filename, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to export beans to the csv file reason is:" + e.getMessage(), e);
        }
    }

    private static void setupCsvResponse(HttpServletResponse response, String filename) {
        try {
            response.setContentType("text/csv;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            String encodedFilename = URLEncoder.encode(filename, "UTF-8").replace("\\+", "%20");
            String contentDisposition = String.format(
                    "attachment; filename=\"%s.csv\"; filename*=UTF-8''%s.csv",
                    filename, encodedFilename);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache,no-store,must-revalidate");
            response.setHeader(HttpHeaders.PRAGMA, "no-cache");
            response.setHeader(HttpHeaders.EXPIRES, "0");
        } catch (UnsupportedEncodingException e) {
            log.error("set up csv response failed", e);
            throw new RuntimeException("set up csv response failed", e);
        }
    }

    private static void createParentDirs(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Path parentPath = path.getParent();
            if (parentPath != null && !Files.exists(path)) {
                Files.createDirectories(parentPath);
                log.debug("create target directory:{}", parentPath);
            }
        } catch (IOException e) {
            log.error("failed to create path:{}", filePath, e);
            throw new RuntimeException("failed to create path", e);
        }
    }


}
