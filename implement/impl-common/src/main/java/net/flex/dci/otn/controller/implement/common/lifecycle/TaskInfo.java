package net.flex.dci.otn.controller.implement.common.lifecycle;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.model.TaskInfoMessage.ResourceType;

@Data
public class TaskInfo {
	private String resourceId;
    private ResourceType resourceType;
    private String resourceName;
    private Long actionTime;
    private ActionType actionType;
    private String who;
    
    
    @Override
	public boolean equals(Object anObject) {  
    	if (this == anObject) {  
            return true;  
        }  
        if (anObject instanceof TaskInfo) { 
        	TaskInfo anotherTask = (TaskInfo)anObject;
        	
        	boolean resEq = StringUtils.equals(resourceId, anotherTask.resourceId);
        	boolean resTypeEq = resourceType == null? anotherTask.resourceType == null : resourceType.equals(anotherTask.resourceType);
        	boolean actionTypeEq = actionType == null? anotherTask.actionType == null: actionType.equals(anotherTask.actionType);
        	
        	return resEq && resTypeEq && actionTypeEq;
        }  
        return false;  
	}
    
    @Override
    public int hashCode() {
    	return (resourceId + resourceType + actionType).hashCode();
    }
}
