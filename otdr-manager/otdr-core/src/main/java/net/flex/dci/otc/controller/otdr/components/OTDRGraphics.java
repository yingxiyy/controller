package net.flex.dci.otc.controller.otdr.components;

import java.util.List;
import net.flex.dci.otc.controller.otdr.model.graphics.ShowOtdrRes;

/**
 * @version 1.0
 * @date 8/20/2023 11:40 AM
 */
public interface OTDRGraphics {

    List<ShowOtdrRes> showOtdrGraphicsByTaskIds(List<String> taskIds);
}
