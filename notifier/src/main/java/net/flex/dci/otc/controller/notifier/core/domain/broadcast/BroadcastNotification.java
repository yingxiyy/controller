package net.flex.dci.otc.controller.notifier.core.domain.broadcast;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/3/25 16:20
 */
@Data
@Builder
@AllArgsConstructor
public class BroadcastNotification implements Serializable {

    @SerializedName("broadcast-notification")
    private MessageNotification messageNotification;

}
