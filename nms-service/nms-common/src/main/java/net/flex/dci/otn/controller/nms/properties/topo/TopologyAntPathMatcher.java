package net.flex.dci.otn.controller.nms.properties.topo;

import static net.flex.dci.otc.common.constants.Constants.LEFT_CURLY_BRACKET;
import static net.flex.dci.otc.common.constants.Constants.RIGHT_CURLY_BRACKET;
import static net.flex.dci.otn.controller.nms.utils.Constants.COMMA;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.enums.PathVariableType;

/**
 * @version 1.0
 * @date 2022/4/27 13:40
 */
@Data
public class TopologyAntPathMatcher implements Serializable {

    private String antPathMatcher;

    private String pathVariable;

    private PathVariableType type;

    public String getPathIdentifier(String... params) {
        String pathPattern = getAntPathMatcher();
        List<String> variableName = Arrays.asList(getPathVariable().split(COMMA));
        if (variableName.size() != params.length) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the path variables length should match the pathPattern");
        }
        String path = pathPattern;
        for (int i = 0; i < variableName.size(); i++) {
            path = path.replace(LEFT_CURLY_BRACKET + variableName.get(i) + RIGHT_CURLY_BRACKET,
                    params[i]);
        }
        return path;
    }
}
