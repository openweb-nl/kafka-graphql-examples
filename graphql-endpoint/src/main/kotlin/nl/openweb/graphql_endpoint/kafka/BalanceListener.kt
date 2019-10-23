package nl.openweb.graphql_endpoint.kafka

import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.Topic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import nl.openweb.graphql_endpoint.service.TransactionService
import org.apache.kafka.clients.consumer.ConsumerRecord

@ExperimentalCoroutinesApi
@FlowPreview
@KafkaListener
class BalanceListener(private val transactionService: TransactionService) {

    @Topic("balance_changed")
    fun receive(record: ConsumerRecord<String, Any>) {
        runBlocking { transactionService.handleRecord(record) }
    }
}