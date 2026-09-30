package net.flex.dci.otn.controller.implement.common.lifecycle;

public enum StepResult {
	pending("pending"),
	success("success"),
	failure("failure");
	
	String value;
	
	private StepResult(String value) {
        this.value = value;
    }
	
	private static final java.util.Map<String, StepResult> VALUE_MAP;

    static {
        final com.google.common.collect.ImmutableMap.Builder<String, StepResult> b = com.google.common.collect.ImmutableMap.builder();
        for (StepResult enumItem : StepResult.values())
        {
            b.put(enumItem.value, enumItem);
        }
        VALUE_MAP = b.build();
    }
    
    public String getStringValue() {
        return value;
    }

    public static StepResult forValue(String valueArg) {
        return VALUE_MAP.get(valueArg);
    }
	
}
