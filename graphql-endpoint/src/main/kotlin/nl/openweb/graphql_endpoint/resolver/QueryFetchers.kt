package nl.openweb.graphql_endpoint.resolver

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import nl.openweb.graphql_endpoint.model.Transaction
import nl.openweb.graphql_endpoint.service.TransactionService
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@FlowPreview
@Singleton
class AllLastTransactions(private val transactionService: TransactionService) : DataFetcher<List<Transaction>> {
    override fun get(env: DataFetchingEnvironment): List<Transaction> {
        return transactionService.allLastTransactions()
    }
}

@ExperimentalCoroutinesApi
@FlowPreview
@Singleton
class TransactionById(private val transactionService: TransactionService) : DataFetcher<Transaction?> {
    override fun get(env: DataFetchingEnvironment): Transaction? {
        val id = env.getArgument<Int>("id")!!
        return transactionService.transactionById(id)
    }
}

@ExperimentalCoroutinesApi
@FlowPreview
@Singleton
class TransactionsByIban(private val transactionService: TransactionService) : DataFetcher<List<Transaction>> {
    override fun get(env: DataFetchingEnvironment): List<Transaction> {
        val iban = env.getArgument<String>("iban")!!
        val maxItems = env.getArgument<Int>("max_items")!!
        return transactionService.transactionsByIban(iban, maxItems)
    }
}