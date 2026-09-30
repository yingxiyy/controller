package net.flex.dci.otn.controller.idc.manager.common;

import java.io.IOException;
import javax.validation.ValidationException;
import net.flex.dci.otn.controller.idc.manager.config.I18Properties;
import net.flex.dci.otn.controller.webapp.exception.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @version 1.0
 * @date 2022/2/9 15:11
 */
@RestControllerAdvice
public class ValidationExceptionAdvice {

    private String language;

    public ValidationExceptionAdvice(I18Properties properties) {
        this.language = properties.getLanguage();
    }

    @ExceptionHandler({ValidationException.class})
    protected ResponseEntity<Object> handleValidationException(ValidationException ex)
            throws IOException {

        ApiErrorResponse apiErrorResponse = new ApiErrorResponse.ApiErrorResponseBuilder()
                .withStatus(HttpStatus.BAD_REQUEST)
                .withErrorCode(HttpStatus.BAD_REQUEST.name())
                .withMessage(ex.getMessage())
                .build();
        return new ResponseEntity<>(apiErrorResponse, HttpStatus.BAD_REQUEST);
    }
}
