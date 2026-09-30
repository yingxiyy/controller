package net.flex.dci.otn.controller.allocate.designer.config;

import lombok.NonNull;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;

import java.util.List;

public interface SiteModelConfigInterface {
     List<String> getMainCardTypes(String nodeType, Integer grid, @NonNull Boolean isProtected, @NonNull String linkModel) throws NeDesignerException;
     List<String> getSlaveCardTypes(String nodeType, Integer grid, Boolean isProtected, @NonNull String linkModel);
}
