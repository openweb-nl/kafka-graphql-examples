(ns nl.openweb.topology.clients
  (:import (io.confluent.kafka.serializers KafkaAvroSerializer AbstractKafkaAvroSerDeConfig KafkaAvroDeserializer KafkaAvroDeserializerConfig)
           (io.confluent.kafka.serializers.subject TopicRecordNameStrategy)
           (java.util Properties UUID)
           (org.apache.kafka.clients CommonClientConfigs)
           (org.apache.kafka.clients.consumer ConsumerConfig KafkaConsumer ConsumerRecords ConsumerRecord)
           (org.apache.kafka.clients.producer ProducerRecord KafkaProducer ProducerConfig)
           (org.apache.kafka.common.config SslConfigs)
           (org.apache.kafka.common.security.auth SecurityProtocol)
           (org.apache.kafka.common.serialization StringSerializer StringDeserializer)
           (org.apache.avro.specific SpecificRecord))
  (:gen-class))

(def brokers (or (System/getenv "KAFKA_BROKERS") "localhost:9092"))
(def schema-url (or (System/getenv "SCHEMA_REGISTRY_URL") "http://localhost:8081"))
(def keystore-location (System/getenv "SSL_KEYSTORE_LOCATION"))
(def truststore-location(System/getenv "SSL_TRUSTSTORE_LOCATION"))
(def ssl-password (System/getenv "SSL_PASSWORD"))

(defn produce
  [^KafkaProducer producer ^String topic-name ^UUID key ^SpecificRecord value]
  (if-let [pr (ProducerRecord. topic-name key value)]
    (.send producer pr)))

(defn optionally-add-ssl
  [properties]
  (when (and keystore-location truststore-location ssl-password)
    (doto properties
      (.put CommonClientConfigs/SECURITY_PROTOCOL_CONFIG (.name SecurityProtocol/SSL))
      (.put SslConfigs/SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG "")
      (.put SslConfigs/SSL_TRUSTSTORE_LOCATION_CONFIG (str "/etc/kafka/secrets/" truststore-location))
      (.put SslConfigs/SSL_TRUSTSTORE_PASSWORD_CONFIG ssl-password)
      (.put SslConfigs/SSL_KEYSTORE_LOCATION_CONFIG (str "/etc/kafka/secrets/" keystore-location))
      (.put SslConfigs/SSL_KEYSTORE_PASSWORD_CONFIG ssl-password)
      (.put SslConfigs/SSL_KEY_PASSWORD_CONFIG ssl-password)
      )))

(defn get-producer
  [client-id & {:keys [config]}]
  (let [properties (Properties.)]
    (doto properties
      (.put ProducerConfig/BOOTSTRAP_SERVERS_CONFIG brokers)
      (.put ProducerConfig/CLIENT_ID_CONFIG client-id)
      (.put ProducerConfig/KEY_SERIALIZER_CLASS_CONFIG (.getName StringSerializer))
      (.put ProducerConfig/VALUE_SERIALIZER_CLASS_CONFIG (.getName KafkaAvroSerializer))
      (.put ProducerConfig/LINGER_MS_CONFIG (.intValue 100))
      (.put ProducerConfig/ACKS_CONFIG "all")
      (.put AbstractKafkaAvroSerDeConfig/SCHEMA_REGISTRY_URL_CONFIG schema-url)
      (.put AbstractKafkaAvroSerDeConfig/AUTO_REGISTER_SCHEMAS false)
      (.put AbstractKafkaAvroSerDeConfig/VALUE_SUBJECT_NAME_STRATEGY (.getName TopicRecordNameStrategy))
      (optionally-add-ssl)
      #(doseq [[prop-name prop-val] config] (.put % prop-name prop-val)))
    (KafkaProducer. properties)))

(defn poll-execute
  [consumer function]
  (let [^ConsumerRecords records (.poll consumer 100)]
    (doseq [^ConsumerRecord record records] (function record))))

(defn consumer-loop [keep-running consumer function]
  (if @keep-running
    (do
      (poll-execute consumer function)
      (recur keep-running consumer function))
    (.close consumer)))

(defn get-consumer
  [client-id group-id & {:keys [config]}]
  (let [properties (Properties.)]
    (doto properties
      (.put ConsumerConfig/BOOTSTRAP_SERVERS_CONFIG brokers)
      (.put ConsumerConfig/CLIENT_ID_CONFIG client-id)
      (.put ConsumerConfig/GROUP_ID_CONFIG group-id)
      (.put ConsumerConfig/AUTO_OFFSET_RESET_CONFIG "earliest")
      (.put ConsumerConfig/MAX_POLL_RECORDS_CONFIG (int 100))
      (.put ConsumerConfig/KEY_DESERIALIZER_CLASS_CONFIG (.getName StringDeserializer))
      (.put ConsumerConfig/VALUE_DESERIALIZER_CLASS_CONFIG (.getName KafkaAvroDeserializer))
      (.put KafkaAvroDeserializerConfig/SPECIFIC_AVRO_READER_CONFIG "true")
      (.put AbstractKafkaAvroSerDeConfig/SCHEMA_REGISTRY_URL_CONFIG schema-url)
      (optionally-add-ssl)
      #(doseq [[prop-name prop-val] config] (.put % prop-name prop-val)))
    (KafkaConsumer. properties)))

(defn consume
  [client-id group-id topic function & {:keys [config]}]
  (let [keep-running (atom true)
        consumer (if config (get-consumer client-id group-id config) (get-consumer client-id group-id))]
    (if (vector? topic)
      (.subscribe consumer topic)
      (.subscribe consumer [topic]))
    (future (consumer-loop keep-running consumer function))
    #(reset! keep-running false)))
