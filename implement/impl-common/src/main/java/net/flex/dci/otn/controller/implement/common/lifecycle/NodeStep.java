package net.flex.dci.otn.controller.implement.common.lifecycle;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;

@Data
public class NodeStep {
	private String name;
	private StepResult value;
	private String failReason;
	
	@Override
	public boolean equals(Object anObject) {  
    	if (this == anObject) {  
            return true;  
        }  
        if (anObject instanceof NodeStep) { 
        	NodeStep anotherStep = (NodeStep)anObject;
        	
        	boolean resEq = StringUtils.equals(name, anotherStep.name);
        	return resEq;
        }  
        return false;  
	}
    
    @Override
    public int hashCode() {
    	return name.hashCode();
    }
}
