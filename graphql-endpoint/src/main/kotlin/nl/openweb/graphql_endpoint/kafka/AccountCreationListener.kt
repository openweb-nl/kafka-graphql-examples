package nl.openweb.graphql_endpoint.kafka

import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.Topic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import nl.openweb.graphql_endpoint.service.AccountCreationService
import org.apache.kafka.clients.consumer.ConsumerRecord

@ExperimentalCoroutinesApi
@FlowPreview
@KafkaListener
class AccountCreationListener(private val accountCreationService: AccountCreationService) {

    @Topic("account_creation_feedback")
    fun receive(record: ConsumerRecord<String, Any>) {
        runBlocking { accountCreationService.handleRecord(record) }
    }
}