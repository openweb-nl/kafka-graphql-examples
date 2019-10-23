package nl.openweb.graphql_endpoint.resolver

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.asPublisher
import nl.openweb.graphql_endpoint.model.AccountResult
import nl.openweb.graphql_endpoint.model.DType
import nl.openweb.graphql_endpoint.model.MoneyTransferResult
import nl.openweb.graphql_endpoint.model.Transaction
import nl.openweb.graphql_endpoint.service.AccountCreationService
import nl.openweb.graphql_endpoint.service.MoneyTransferService
import nl.openweb.graphql_endpoint.service.TransactionService
import org.reactivestreams.Publisher
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@FlowPreview
@Singleton
class GetAccount(private val accountCreationService: AccountCreationService) : DataFetcher<Publisher<AccountResult>> {
    override fun get(env: DataFetchingEnvironment): Publisher<AccountResult> {
        val username = env.getArgument<String>("username")!!
        val password = env.getArgument<String>("password")!!
        return accountCreationService.getAccount(username, password).asPublisher()
    }
}

@ExperimentalCoroutinesApi
@FlowPreview
@Singleton
class MoneyTransfer(private val moneyTransferService: MoneyTransferService) : DataFetcher<Publisher<MoneyTransferResult>> {
    override fun get(env: DataFetchingEnvironment): Publisher<MoneyTransferResult> {
        val amount = env.getArgument<Int>("amount")!!
        val descr = env.getArgument<String>("descr")!!
        val from = env.getArgument<String>("from")!!
        val to = env.getArgument<String>("to")!!
        val token = env.getArgument<String>("token")!!
        val username = env.getArgument<String>("username")!!
        val uuid = env.getArgument<String>("uuid")!!
        return moneyTransferService.transfer(amount, descr, from, to, token, username, uuid).asPublisher()
    }
}

@ExperimentalCoroutinesApi
@FlowPreview
@Singleton
class StreamTransactions(private val transactionService: TransactionService) : DataFetcher<Publisher<Transaction>> {
    override fun get(env: DataFetchingEnvironment): Publisher<Transaction> {
        val direction = env.getArgument<DType>("direction")
        val iban = env.getArgument<String>("iban")
        val minAmount = env.getArgument<Int>("min_amount")
        val maxAmount = env.getArgument<Int>("max_amount")
        val descrIncludes = env.getArgument<String>("descr_includes")
        return transactionService.stream(direction, iban, minAmount, maxAmount, descrIncludes).asPublisher()
    }
}
