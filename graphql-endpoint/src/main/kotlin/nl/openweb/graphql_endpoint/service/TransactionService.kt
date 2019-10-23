package nl.openweb.graphql_endpoint.service

import io.micronaut.data.model.Pageable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import nl.openweb.data.BalanceChanged
import nl.openweb.graphql_endpoint.logger
import nl.openweb.graphql_endpoint.model.*
import nl.openweb.graphql_endpoint.repository.TransactionRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import javax.inject.Singleton

@FlowPreview
@ExperimentalCoroutinesApi
@Singleton
class TransactionService(private val transactionRepository: TransactionRepository) {
    private val broadcast = BroadcastChannel<Transaction>(capacity = Channel.CONFLATED)
    private val log by logger()

    suspend fun handleRecord(record: ConsumerRecord<String, Any>) {
        if (record.value() is BalanceChanged) {
            handleBalanceChanged(record.value() as BalanceChanged)
        } else {
            log.warn("Unexpected item {} with class {}, expected BalanceChanged",
                    record.value(), record.value().javaClass.name)
        }
    }

    private suspend fun handleBalanceChanged(changed: BalanceChanged) {
        var transaction = changed.toTransaction()
        transaction = transactionRepository.save(transaction)
        broadcast.send(transaction)
    }

    fun allLastTransactions(): List<Transaction> = transactionRepository.allLastTransactions()

    fun transactionById(id: Int): Transaction? = transactionRepository.findById(id).orElse(null)

    fun transactionsByIban(iban: String, maxItems: Int): List<Transaction> =
            transactionRepository.findAllByIbanOrderByIdDesc(iban, Pageable.from(0, maxItems))

    fun filterFunction(
        direction: DType?,
        iban: String?,
        minAmount: Int?,
        maxAmount: Int?,
        descrIncluded: String?
    ): (Transaction) -> Boolean = { t ->
        t.hasDirection(direction) && t.hasIban(iban) && t.hasMinAmount(minAmount) &&
                t.hasMaxAmount(maxAmount) && t.containsDesc(descrIncluded)
    }

    fun stream(
        direction: DType?,
        iban: String?,
        minAmount: Int?,
        maxAmount: Int?,
        descrIncluded: String?
    ): Flow<Transaction> = broadcast
            .openSubscription()
            .consumeAsFlow()
            .filter { t -> filterFunction(direction, iban, minAmount, maxAmount, descrIncluded).invoke(t) }
}