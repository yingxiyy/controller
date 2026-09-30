package net.flex.dci.otc.controller.otdr.model.graphics;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 8/18/2023 4:25 PM
 */
@Data
@Builder
public class ShowOtdrResults implements Serializable {

    private List<ShowOtdrRes> result;
}
