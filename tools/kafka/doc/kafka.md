## kafka配置 示例

可以修改的地方

```declarative
kafka:
bootstrap-servers: localhost:9092
consumer-group: alarm-consumer
partition-number: 3
replication-factor: 1
client-concurrency: 3
max-poll-records: 200
max-poll-interval-ms: 300000
session-timeout-ms: 15000
heartbeat-interval-ms: 3000
enable-auto-commit: false
auto-commit-interval-ms: 5000
max-partition-fetch-bytes: 11534336
fetch-max-bytes: 10485760
## 生产者
producer-max-request-size: 16777216
producer-compression-type: gzip
producer-buffer-memory: 134217728
```