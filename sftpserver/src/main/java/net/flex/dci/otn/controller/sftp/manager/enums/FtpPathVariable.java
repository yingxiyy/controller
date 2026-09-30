package net.flex.dci.otn.controller.sftp.manager.enums;

/**
 * @version 1.0
 * @date 6/7/2023 3:41 PM
 */
public enum FtpPathVariable {
    Name("name");

    private String name;

    FtpPathVariable(String name) {
        this.name = name;
    }

    public static FtpPathVariable getByName(String name) {
        for (FtpPathVariable ftpPathVariable : FtpPathVariable.values()) {
            if (ftpPathVariable.name.equals(name)) {
                return ftpPathVariable;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }
}
