package net.flex.dci.otc.controller.notifier.core.domain.broadcast;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageBody implements Serializable {

    @SerializedName("time")
    String time;

    @SerializedName("content")
    String content;
}
