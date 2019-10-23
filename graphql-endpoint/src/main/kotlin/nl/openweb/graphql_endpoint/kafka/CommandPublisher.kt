package nl.openweb.graphql_endpoint.kafka

import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.KafkaKey
import io.micronaut.configuration.kafka.annotation.Topic
import org.apache.avro.specific.SpecificRecordBase

@KafkaClient("command-publisher")
interface CommandPublisher {

    @Topic("commands")
    fun publish(@KafkaKey username: String, avroRecord: SpecificRecordBase)
}