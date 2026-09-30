/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.flex.dci.otn.controller.batchlock.support;

import net.flex.dci.otn.controller.batchlock.support.annotation.NonNull;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public final class Utils {

    /**
     * A {@link DateTimeFormatter} like {@link DateTimeFormatter#ISO_INSTANT} with the exception
     * that it always appends exactly three fractional digits (nano seconds).
     * <p>
     * This is required in order to guarantee natural sorting, which enables us to use
     * <code>&lt;=</code> comparision in queries.
     *
     * <pre>
     * 2018-12-07T12:30:37.000Z
     * 2018-12-07T12:30:37.810Z
     * 2018-12-07T12:30:37.819Z
     * 2018-12-07T12:30:37.820Z
     * </pre>
     * <p>
     * When using variable fractional digit count as done in {@link DateTimeFormatter#ISO_INSTANT
     * ISO_INSTANT} and {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME ISO_OFFSET_DATE_TIME} the
     * following sorting occurs:
     *
     * <pre>
     * 2018-12-07T12:30:37.819Z
     * 2018-12-07T12:30:37.81Z
     * 2018-12-07T12:30:37.820Z
     * 2018-12-07T12:30:37Z
     * </pre>
     *
     * @see <a href="https://stackoverflow.com/a/5098252">natural sorting of ISO 8601 time
     *      format</a>
     */
    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendInstant(3)
            .toFormatter();

    private static final String hostip = initHostip();

    private Utils() {
    }

    @NonNull
    public static String getHostip() {
        return hostip;
    }

    @NonNull
    public static String getCurrentThreadId() {
        return Long.toHexString(Thread.currentThread().getId());
    }

    public static String getCurrentProcessId() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name.split("@")[0];
        return Long.toHexString(Long.parseLong(pid));
    }

    public static String toIsoString(@NonNull Instant instant) {
        OffsetDateTime utc = instant.atOffset(ZoneOffset.UTC);
        return formatter.format(utc);
    }

    @NonNull
    private static String initHostip() {
        try {
            return bytesToHex(InetAddress.getLocalHost().getAddress());
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    @NonNull
    private static String initHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
