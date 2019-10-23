package nl.openweb.graphql_endpoint

import io.micronaut.runtime.Micronaut

object GraphqlEndpointApplication {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
                .packages("nl.openweb.graphql_endpoint")
                .mainClass(GraphqlEndpointApplication.javaClass)
                .start()
    }
}