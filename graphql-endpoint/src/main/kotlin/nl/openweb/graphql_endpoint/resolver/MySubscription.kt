package nl.openweb.graphql_endpoint.resolver

import com.expediagroup.graphql.spring.operations.Subscription
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
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@FlowPreview
@Component
class MySubscription(
    private val accountCreationService: AccountCreationService,
    private val moneyTransferService: MoneyTransferService,
    private val transactionService: TransactionService
) : Subscription {

    fun get_account(username: String, password: String): Publisher<AccountResult> {
        return accountCreationService.getAccount(username, password).asPublisher()
    }

    fun money_transfer(
        amount: Int,
        descr: String,
        from: String,
        to: String,
        token: String,
        username: String,
        uuid: String
    ): Publisher<MoneyTransferResult> {
        return moneyTransferService.transfer(amount, descr, from, to, token, username, uuid).asPublisher()
    }

    fun stream_transactions(
        direction: DType?,
        iban: String?,
        minAmount: Int?,
        maxAmount: Int?,
        descrIncludes: String?
    ): Publisher<Transaction> {
        return transactionService.stream(direction, iban, minAmount, maxAmount, descrIncludes).asPublisher()
    }
}