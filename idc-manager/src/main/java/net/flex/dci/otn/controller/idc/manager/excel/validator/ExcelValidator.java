package net.flex.dci.otn.controller.idc.manager.excel.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.AllArgsConstructor;
import net.flex.dci.otn.controller.idc.manager.dto.RowError;
import net.flex.dci.otn.controller.idc.manager.model.IdcData;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

/**
 * @version 1.0
 * @date 2022/1/20 10:16
 */
@AllArgsConstructor
public class ExcelValidator<T> {

    private final Validator validator;
    private final MessageSource messageSource;

    public List<RowError> validateRow(Collection<T> datas) {
        List<RowError> errors = new ArrayList<>();
        for (T data : datas) {
            errors.addAll(doValidateRow(data));
        }
        return errors;
    }


    private List<RowError> doValidateRow(T data) {
        Set<ConstraintViolation<T>> violations = validator.validate(data);
        if (violations == null || violations.isEmpty()) {
            return Collections.emptyList();
        }
        int physicalRow = (data instanceof IdcData && ((IdcData) data).getPhysicalRow() != null)
                ? ((IdcData) data).getPhysicalRow() : 0;
        List<RowError> rowErrors = new ArrayList<>();
        for (ConstraintViolation<T> v : violations) {
            String key = v.getMessage();
            rowErrors.add(RowError.builder()
                    .rowNumber(physicalRow)
                    .messageKey(key)
                    .message(translate(key))
                    .build());
        }
        return rowErrors;
    }

    private String translate(String key) {
        try {
            return messageSource.getMessage(key, null, Locale.ENGLISH);
        } catch (NoSuchMessageException ex) {
            return key;
        }
    }
}
