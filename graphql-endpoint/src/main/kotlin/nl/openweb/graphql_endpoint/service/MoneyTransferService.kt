package nl.openweb.graphql_endpoint.service

import arrow.core.Tuple2
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import nl.openweb.data.ConfirmMoneyTransfer
import nl.openweb.data.MoneyTransferConfirmed
import nl.openweb.data.MoneyTransferFailed
import nl.openweb.graphql_endpoint.kafka.CommandPublisher
import nl.openweb.graphql_endpoint.logger
import nl.openweb.graphql_endpoint.model.MoneyTransferResult
import nl.openweb.graphql_endpoint.model.toMoneyTransferResult
import nl.openweb.graphql_endpoint.toAvroUuid
import nl.openweb.graphql_endpoint.toJavaUUID
import org.apache.kafka.clients.consumer.ConsumerRecord
import javax.inject.Singleton

@FlowPreview
@ExperimentalCoroutinesApi
@Singleton
class MoneyTransferService(private val publisher: CommandPublisher) {
    private val broadcast = BroadcastChannel<Tuple2<String, MoneyTransferResult>>(capacity = Channel.CONFLATED)
    private val log by logger()

    suspend fun handleRecord(record: ConsumerRecord<String, Any>) {
        if (record.value() is MoneyTransferConfirmed) {
            handleMoneyTransferConfirmed(record.value() as MoneyTransferConfirmed)
        } else if (record.value() is MoneyTransferFailed) {
            handleMoneyTransferFailed(record.value() as MoneyTransferFailed)
        } else {
            log.warn(
                    "Unexpected item {} with class {}, expected either AccountCreationConfirmed or " +
                            "AccountCreationFailed",
                    record.value(), record.value().javaClass.name)
        }
    }

    private suspend fun handleMoneyTransferConfirmed(confirmed: MoneyTransferConfirmed) {
        val result = confirmed.toMoneyTransferResult()
        val item = Tuple2(confirmed.id.toJavaUUID().toString(), result)
        broadcast.send(item)
    }

    private suspend fun handleMoneyTransferFailed(failed: MoneyTransferFailed) {
        val result = failed.toMoneyTransferResult()
        val item = Tuple2(failed.id.toJavaUUID().toString(), result)
        broadcast.send(item)
    }

    fun transfer(
        amount: Int,
        descr: String,
        from: String,
        to: String,
        token: String,
        username: String,
        uuid: String
    ): Flow<MoneyTransferResult> {
        val transfer = ConfirmMoneyTransfer()
        transfer.id = uuid.toAvroUuid()
        transfer.amount = amount.toLong()
        transfer.description = descr
        transfer.from = from
        transfer.to = to
        transfer.token = token
        publisher.publish(username, transfer)
        return broadcast
                .openSubscription()
                .consumeAsFlow()
                .filter { x: Tuple2<String, MoneyTransferResult> -> x.a == uuid }
                .map { obj: Tuple2<String, MoneyTransferResult> -> obj.b }
    }
}