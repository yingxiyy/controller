package net.flex.dci.otn.controller.subnet.manager.component;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 2026/1/11
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class SnowflakeIdGenerator {

    private final static Snowflake snowFlak = IdUtil.getSnowflake(1, 1);

    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String SUBNET_PREFIX = "SUBNET_";


    public synchronized long nextId() {
        return snowFlak.nextId();
    }

    public synchronized String[] nextBatchIds(int count) {
        if (count <= 0 || count > 1000) {
            throw new IllegalArgumentException("generate count should be in [1-1000]");
        }

        String[] ids = new String[count];
        for (int i = 0; i < count; i++) {
            ids[i] = nextBase62Id();
        }
        return ids;
    }

    public String nextBase62Id() {
        return SUBNET_PREFIX + toBase62(nextId());
    }

    public String nextBase62IdWithoutPrefix() {
        return toBase62(nextId());
    }

    private String toBase62(long snowFlakeId) {
        if (snowFlakeId == 0) {
            return "0";
        }
        boolean negative = false;
        if (snowFlakeId < 0) {
            negative = true;
            snowFlakeId = -snowFlakeId;
        }
        StringBuilder sb = new StringBuilder();
        while (snowFlakeId > 0) {
            int remainder = (int) (snowFlakeId % 62);
            sb.append(BASE62_CHARS.charAt(remainder));
            snowFlakeId /= 62;
        }
        if (negative) {
            sb.append("-");
        }
        return sb.reverse().toString();
    }
}
