package nl.openweb.graphql_endpoint.kafka

import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.Topic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import nl.openweb.graphql_endpoint.service.MoneyTransferService
import org.apache.kafka.clients.consumer.ConsumerRecord

@ExperimentalCoroutinesApi
@FlowPreview
@KafkaListener
class MoneyTransferListener(private val moneyTransferService: MoneyTransferService) {

    @Topic("money_transfer_feedback")
    fun receive(record: ConsumerRecord<String, Any>) {
        runBlocking { moneyTransferService.handleRecord(record) }
    }
}