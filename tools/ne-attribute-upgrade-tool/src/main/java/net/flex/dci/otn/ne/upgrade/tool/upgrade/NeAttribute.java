package net.flex.dci.otn.ne.upgrade.tool.upgrade;

/**
 * 2026/3/13
 *
 * @author musa
 * @version 1.0
 **/

public interface NeAttribute {

    void upgradeNeSubType();

    void upgradeNeIp();

    void removeTelemetryRecord();
}
