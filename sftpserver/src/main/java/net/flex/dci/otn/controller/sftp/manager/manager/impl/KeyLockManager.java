package net.flex.dci.otn.controller.sftp.manager.manager.impl;


import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class KeyLockManager {

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public Object getLock(String key) {
        return locks.computeIfAbsent(key, k -> new Object());
    }
}