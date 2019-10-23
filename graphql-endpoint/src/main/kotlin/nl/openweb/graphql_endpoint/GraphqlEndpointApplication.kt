package nl.openweb.graphql_endpoint

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class GraphqlEndpointApplication

fun main(args: Array<String>) {
    runApplication<GraphqlEndpointApplication>(*args)
}