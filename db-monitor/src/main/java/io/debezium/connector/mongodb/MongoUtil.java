/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.debezium.connector.mongodb;

import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ServerDescription;
import io.debezium.DebeziumException;
import io.debezium.annotation.Immutable;
import io.debezium.function.BlockingConsumer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bson.Document;
import org.bson.types.Binary;
import org.slf4j.Logger;

@Immutable
public class MongoUtil {

    private static final Pattern IPV6_BRACKET_PATTERN = Pattern.compile("(\\[[^]]+\\])(:(\\d+))?");
    private static final Pattern PORT_PATTERN = Pattern.compile(":(\\d+)$");
    private static final Pattern IPV6_LIKE_PATTERN = Pattern.compile(
            "^(?:" +
                    "([a-fA-F0-9]{1,4}:){2,}[a-fA-F0-9]{1,4}" +
                    "|" +
                    "::[a-fA-F0-9]{1,4}" +
                    "|" +
                    "[a-fA-F0-9]{1,4}::[a-fA-F0-9]{1,4}" +
                    "|" +
                    "[a-fA-F0-9]{1,4}::" +
                    "|" +
                    "(([a-fA-F0-9]{1,4}:)+[a-fA-F0-9]{1,4}::([a-fA-F0-9]{1,4}:)*[a-fA-F0-9]{1,4})" +
                    ")$");

    public static final Pattern ADDRESS_DELIMITER_PATTERN = Pattern.compile(",");

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MongoUtil.class);

    public static List<ServerAddress> parseAddresses(String addressStr) {
        List<ServerAddress> addresses = new ArrayList<>();
        if (addressStr != null) {
            addressStr = addressStr.trim();
            for (String address : ADDRESS_DELIMITER_PATTERN.split(addressStr)) {
                String hostAndPort;
                if (address.startsWith("[")) {
                    hostAndPort = address;
                } else {
                    int index = address.indexOf("/[");
                    if (index >= 0) {
                        if ((index + 2) < address.length()) {
                            hostAndPort = address.substring(index + 1);
                        } else {
                            continue;
                        }
                    } else {
                        index = address.indexOf("/");
                        if (index >= 0) {
                            if ((index + 1) < address.length()) {
                                hostAndPort = address.substring(index + 1);
                            } else {
                                hostAndPort = ServerAddress.defaultHost();
                            }
                        } else {
                            hostAndPort = address;
                        }
                    }
                }
                ServerAddress newAddress = parseAddress(hostAndPort);
                if (newAddress != null) {
                    addresses.add(newAddress);
                }
            }
        }
        return addresses;
    }

    protected static ServerAddress parseAddress(String hostAndPort) {
        String trimmed = hostAndPort.trim();
        // 1. Try bracketed IPv6: [::1]:27017
        Matcher bracketMatcher = IPV6_BRACKET_PATTERN.matcher(trimmed);
        if (bracketMatcher.matches()) {
            String host = bracketMatcher.group(1);
            if (host.startsWith("[") && host.endsWith("]")) {
                host = host.substring(1, host.length() - 1);
            }
            String portStr = bracketMatcher.group(3);
            try {
                int port =
                        portStr != null ? Integer.parseInt(portStr) : ServerAddress.defaultPort();
                return new ServerAddress(new InetSocketAddress(InetAddress.getByName(host), port));
            } catch (UnknownHostException e) {
                LOGGER.warn("Unable to resolve IPv6 address '{}': {}", host, e.getMessage());
                return null;
            }
        }
        // 2. Check for unbracketed IPv6 + port: ::1:27017
        //    lastIndexOf(":") correctly separates ::1 and 27017
        if (trimmed.chars().filter(c -> c == ':').count() > 1) {
            int lastColon = trimmed.lastIndexOf(":");
            String candidateHost = trimmed.substring(0, lastColon);
            String portStr = trimmed.substring(lastColon + 1);
            if (portStr.matches("\\d+") && isLikelyIpv6(candidateHost)) {
                try {
                    int port = Integer.parseInt(portStr);
                    return new ServerAddress(
                            new InetSocketAddress(InetAddress.getByName(candidateHost), port));
                } catch (UnknownHostException e) {
                    LOGGER.warn("Unable to resolve IPv6 address '{}': {}", candidateHost,
                            e.getMessage());
                    return null;
                }
            }
        }
        // 3. Default host:port parsing (IPv4 or hostname)
        int lastColon = trimmed.lastIndexOf(":");
        if (lastColon >= 0) {
            String host = trimmed.substring(0, lastColon);
            String portStr = trimmed.substring(lastColon + 1);
            try {
                int port = Integer.parseInt(portStr);
                return new ServerAddress(host, port);
            } catch (NumberFormatException e) {
                return new ServerAddress(trimmed);
            }
        }
        return new ServerAddress(trimmed);
    }

    private static boolean isLikelyIpv6(String host) {
        return IPV6_LIKE_PATTERN.matcher(host).matches();
    }

    public static String toString(ServerAddress address) {
        String host = address.getHost();
        int port = address.getPort();
        if (host.contains(":")) {
            return "[" + host + "]:" + port;
        }
        return host + ":" + port;
    }

    public static String byAddress(ServerAddress address) {
        return address.getHost() + ":" + address.getPort();
    }

    public static String hostname(ServerAddress address) {
        return address.getHost();
    }

    public static boolean matches(ServerAddress address, String hostAddress) {
        String host = address.getHost();
        int port = address.getPort();
        if (hostAddress.equals(host + ":" + port)) {
            return true;
        }
        if (host.contains(":")) {
            if (hostAddress.equals("[" + host + "]:" + port)) {
                return true;
            }
        }
        if (hostAddress.equals(host)) {
            return true;
        }
        if (hostAddress.contains(":")) {
            String addrHost = hostAddress.contains("[")
                    ? hostAddress.substring(1, hostAddress.indexOf("]"))
                    : hostAddress.substring(0, hostAddress.lastIndexOf(":"));
            int addrPort = Integer.parseInt(
                    hostAddress.substring(hostAddress.lastIndexOf(":") + 1));
            return host.equals(addrHost) && port == addrPort;
        }
        return false;
    }

    public static ServerAddress getPrimaryAddress(MongoClient client) {
        ClusterDescription clusterDescription = client.getClusterDescription();
        if (clusterDescription == null || !clusterDescription.hasReadableServer(
                com.mongodb.ReadPreference.primaryPreferred())) {
            client.listDatabaseNames().first();
            clusterDescription = client.getClusterDescription();
        }
        if (clusterDescription == null) {
            throw new DebeziumException(
                    "Unable to read cluster description from MongoDB connection.");
        } else if (!clusterDescription.hasReadableServer(
                com.mongodb.ReadPreference.primaryPreferred())) {
            throw new DebeziumException(
                    "Unable to use cluster description from MongoDB connection: "
                            + clusterDescription);
        }
        List<ServerDescription> serverDescriptions = clusterDescription.getServerDescriptions();
        if (serverDescriptions == null || serverDescriptions.isEmpty()) {
            throw new DebeziumException(
                    "Unable to read server descriptions from MongoDB connection (Null or empty list).");
        }
        java.util.Optional<ServerDescription> primaryDescription = serverDescriptions.stream()
                .filter(ServerDescription::isPrimary).findFirst();
        if (!primaryDescription.isPresent()) {
            throw new DebeziumException(
                    "Unable to find primary from MongoDB connection, got '" + serverDescriptions
                            + "'");
        }
        com.mongodb.ServerAddress primaryAddress = primaryDescription.get().getAddress();
        return new com.mongodb.ServerAddress(primaryAddress.getHost(), primaryAddress.getPort());
    }

    public static Document getOplogEntry(MongoClient client, int seconds, Logger logger) {
        MongoDatabase local = client.getDatabase("local");
        MongoCollection<Document> oplog = local.getCollection("oplog.rs");
        Document lastEntry = oplog.find()
                .sort(new Document("$natural", -1))
                .limit(1)
                .first();
        if (lastEntry == null && logger != null) {
            logger.warn("No oplog entry found in local.oplog.rs");
        }
        return lastEntry;
    }

    public static void onCollectionDocuments(MongoClient client, String databaseName,
            String collectionName, BlockingConsumer<Document> consumer)
            throws InterruptedException {
        MongoDatabase db = client.getDatabase(databaseName);
        MongoCollection<Document> collection = db.getCollection(collectionName);
        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document document = cursor.next();
                consumer.accept(document);
            }
        }
    }

    public static void forEachDatabaseName(MongoClient client,
            java.util.function.Consumer<String> operation) {
        client.listDatabaseNames().forEach(operation);
    }

    public static void forEachCollectionNameInDatabase(MongoClient client, String databaseName,
            java.util.function.Consumer<String> operation) {
        MongoDatabase db = client.getDatabase(databaseName);
        db.listCollectionNames().forEach(operation);
    }

    public static String replicaSetUsedIn(String addresses) {
        if (addresses.startsWith("[")) {
            return null;
        }
        int index = addresses.indexOf('/');
        if (index < 0) {
            return null;
        }
        return addresses.substring(0, index);
    }

    protected static String toString(List<ServerAddress> addresses) {
        return String.join(",", addresses.stream()
                .map(MongoUtil::toString).collect(java.util.stream.Collectors.toList()));
    }


    public static String getOplogSessionTransactionId(Document oplogEvent) {
        if (!oplogEvent.containsKey("txnNumber")) {
            return null;
        }
        final Document lsidDoc = oplogEvent.get("lsid", Document.class);
        final Object id = lsidDoc.get("id");
        // MongoDB 4.2 returns Binary instead of UUID
        final String lsid =
                (id instanceof Binary) ? UUID.nameUUIDFromBytes(((Binary) id).getData()).toString()
                        : ((UUID) id).toString();
        final Long txnNumber = oplogEvent.getLong("txnNumber");
        return lsid + ":" + txnNumber;
    }

    public static SourceInfo.SessionTransactionId getChangeStreamSessionTransactionId(
            ChangeStreamDocument<Document> event) {
        if (event.getLsid() == null || event.getTxnNumber() == null) {
            return null;
        }

        return new SourceInfo.SessionTransactionId(event.getLsid() == null ? null
                : event.getLsid().toJson(JsonSerialization.COMPACT_JSON_SETTINGS),
                event.getTxnNumber() == null ? null : event.getTxnNumber().longValue());
    }
}
