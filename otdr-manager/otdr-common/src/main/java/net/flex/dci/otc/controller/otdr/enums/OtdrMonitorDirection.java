package net.flex.dci.otc.controller.otdr.enums;

/**
 * @version 1.0
 * @date 2022/9/1 15:52
 */
public enum OtdrMonitorDirection {
    AZ("A-Z", 0),
    ZA("Z-A", 1);

    private String directionStr;

    private int directionNum;

    OtdrMonitorDirection(String directionStr, int directionNum) {
        this.directionNum = directionNum;
        this.directionStr = directionStr;
    }


    public static OtdrMonitorDirection getDirection(int val) {
        for (OtdrMonitorDirection direction : OtdrMonitorDirection.values()) {
            if (direction.getDirectionNum() == val) {
                return direction;
            }
        }
        return null;
    }

    public String getDirectionStr() {
        return directionStr;
    }

    public void setDirectionStr(String directionStr) {
        this.directionStr = directionStr;
    }

    public Integer getDirectionNum() {
        return directionNum;
    }

    public void setDirectionNum(Integer directionNum) {
        this.directionNum = directionNum;
    }
}


