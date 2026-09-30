package net.flex.dci.otn.controller.module;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class HistoryPmUploadRequest implements Serializable {
    List<String> neIds;
}
