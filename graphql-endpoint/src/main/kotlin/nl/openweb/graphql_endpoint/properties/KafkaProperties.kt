package nl.openweb.graphql_endpoint.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "kafka")
class KafkaProperties {
    lateinit var commandsTopic: String
}