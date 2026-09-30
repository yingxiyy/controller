package net.flex.dci.otn.controller.resource.statistic.core.inventory;

import lombok.extern.slf4j.Slf4j;

/**
 * @version 1.0
 * @date 11/5/2025 4:24 PM
 */
@Slf4j
public abstract class AbstractInventory<T> implements Inventory<T> {


    protected abstract Class<T> getClazz();
}
