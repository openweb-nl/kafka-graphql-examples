package nl.openweb.graphql_endpoint.resolver

import com.expediagroup.graphql.spring.operations.Query
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import nl.openweb.graphql_endpoint.model.Transaction
import nl.openweb.graphql_endpoint.service.TransactionService
import org.springframework.stereotype.Component

@InternalCoroutinesApi
@FlowPreview
@Component
class MyQuery(private val transactionService: TransactionService) : Query {
    fun all_last_transactions(): List<Transaction> {
        return transactionService.allLastTransactions()
    }

    fun transaction_by_id(id: Int): Transaction? {
        return transactionService.transactionById(id)
    }

    fun transactions_by_iban(iban: String, maxItems: Int): List<Transaction> {
        return transactionService.transactionsByIban(iban, maxItems)
    }
}