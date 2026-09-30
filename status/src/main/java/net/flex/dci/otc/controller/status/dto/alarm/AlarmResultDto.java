package net.flex.dci.otc.controller.status.dto.alarm;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 2026/8/17
 *
 * @author musa
 * @version 1.0
 **/
@Data
public class AlarmResultDto implements Serializable {


    @SerializedName("total-records")
    private Long totalRecords;


    private List<AlarmDto> alarm;

}
