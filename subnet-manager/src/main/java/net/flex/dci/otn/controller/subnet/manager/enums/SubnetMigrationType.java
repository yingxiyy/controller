package net.flex.dci.otn.controller.subnet.manager.enums;

import lombok.Getter;

/**
 * 2026/2/11
 *
 * @author musa
 * @version 1.0
 **/
@Getter
public enum SubnetMigrationType {
    MIGRATION_OUT("migration_out"),
    MIGRATION_IN("migration_in");
    private final String desc;

    SubnetMigrationType(String desc) {
        this.desc = desc;
    }
}
