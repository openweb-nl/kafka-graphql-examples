package nl.openweb.graphql_endpoint.kafka

import nl.openweb.graphql_endpoint.properties.KafkaProperties
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class CommandPublisher(private val kafkaProperties: KafkaProperties, private val kafkaTemplate: KafkaTemplate<String, Any>) {
    fun publish(username: String, avroRecord: SpecificRecordBase) {
        kafkaTemplate.send(kafkaProperties.commandsTopic, username, avroRecord)
    }
}