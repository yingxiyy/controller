package net.flex.dci.otn.controller.nms.nms.dto.thumbnail.design;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

/**
 * @version 1.0
 * @date 7/15/2025 4:46 PM
 */
@Data
@Builder
public class ThumbnailDesignSequenceDto implements Serializable {

    private ThumbnailDesignSequenceDto primary;
    private ThumbnailDesignSequenceDto secondary;
    private ThumbnailDesignSequenceDto tertiary;

    private String nodeId;
    private String linkId;

    private Class<?> refClazz;

    @Tolerate
    public ThumbnailDesignSequenceDto() {

    }
}
