package net.flex.dci.otc.controller.notifier.core.domain.broadcast;


import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MessageNotification implements Serializable {

    @SerializedName("message")
    private String message;

    @SerializedName("title")
    private String title;

    @SerializedName("status")
    private String status;


    @SerializedName("time")
    String time;

}
