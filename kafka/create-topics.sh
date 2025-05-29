# ./kafka/create-topics.sh
#!/bin/bash

echo "[Kafka Init] Waiting for Kafka to be ready..."
sleep 10

TOPICS=("order-export")

for topic in "${TOPICS[@]}"
do
  echo "[Kafka Init] Creating topic: $topic"
  /opt/bitnami/kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 \
    --create \
    --if-not-exists \
    --topic "$topic" \
    --partitions 3 \
    --replication-factor 1
done

echo "[Kafka Init] Topic creation complete."
