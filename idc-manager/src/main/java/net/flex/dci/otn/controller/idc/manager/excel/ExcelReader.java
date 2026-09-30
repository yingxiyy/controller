package net.flex.dci.otn.controller.idc.manager.excel;

import com.alibaba.excel.EasyExcel;
import java.io.InputStream;
import java.util.Collection;
import java.util.function.Consumer;
import javax.validation.Validator;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.flex.dci.otn.controller.idc.manager.excel.validator.ExcelValidator;
import org.springframework.context.MessageSource;

/**
 * @version 1.0
 * @date 2022/1/20 10:42
 */
@AllArgsConstructor
public class ExcelReader {

    private final Validator validator;
    private final MessageSource messageSource;

    public <T> void read(Meta<T> meta) {
        ExcelValidator<T> excelValidator = new ExcelValidator<>(validator, messageSource);
        ExcelEventListener<T> readListener = new ExcelEventListener<>(excelValidator,
                meta.consumer, meta.skipRows);
        EasyExcel.read(meta.excelStream, meta.domain, readListener)
                .headRowNumber(meta.headRowNumber)
                .sheet("idc")
                .doRead();
    }

    @Data
    public static class Meta<T> {

        private InputStream excelStream;

        private Integer headRowNumber;

        private Integer skipRows;

        private Class<T> domain;

        private Consumer<Collection<T>> consumer;
    }
}
