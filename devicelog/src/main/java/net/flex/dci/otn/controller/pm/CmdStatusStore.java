package net.flex.dci.otn.controller.pm;

import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CmdStatusStore implements Serializable {

    private final ConcurrentHashMap<String, String> status = new ConcurrentHashMap<>();

    public void set(String key, String value) {
        status.put(key, value);
    }

    public String get(String key) {
        return status.get(key);
    }

    public Map<String, String> getAll() {
        return new HashMap<>(status);
    }

    public String toString() {
        return status.toString();
    }
}
