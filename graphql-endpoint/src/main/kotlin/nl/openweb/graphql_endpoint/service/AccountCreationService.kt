package nl.openweb.graphql_endpoint.service

import arrow.core.Either
import arrow.core.Tuple2
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import nl.openweb.data.AccountCreationConfirmed
import nl.openweb.data.AccountCreationFailed
import nl.openweb.data.ConfirmAccountCreation
import nl.openweb.graphql_endpoint.kafka.CommandPublisher
import nl.openweb.graphql_endpoint.logger
import nl.openweb.graphql_endpoint.model.Account
import nl.openweb.graphql_endpoint.model.AccountResult
import nl.openweb.graphql_endpoint.model.toAccountResult
import nl.openweb.graphql_endpoint.repository.AccountRepository
import nl.openweb.graphql_endpoint.toAvroUuid
import nl.openweb.graphql_endpoint.toJavaUUID
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*
import javax.inject.Singleton

@FlowPreview
@ExperimentalCoroutinesApi
@Singleton
class AccountCreationService(private val accountRepository: AccountRepository, private val publisher: CommandPublisher) {
    private val broadcast = BroadcastChannel<Tuple2<UUID, AccountResult>>(capacity = Channel.CONFLATED)
    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder(10)
    private val log by logger()

    suspend fun handleRecord(record: ConsumerRecord<String, Any>) {
        if (record.value() is AccountCreationConfirmed) {
            handleAccountCreationConfirmed(record.value() as AccountCreationConfirmed)
        } else if (record.value() is AccountCreationFailed) {
            handleAccountCreationFailed(record.value() as AccountCreationFailed)
        } else {
            log.warn(
                    "Unexpected item {} with class {}, expected either AccountCreationConfirmed or " +
                            "AccountCreationFailed",
                    record.value(), record.value().javaClass.name)
        }
    }

    fun getAccount(username: String, password: String): Flow<AccountResult> {
        val encoded = passwordEncoder.encode(password)
        val check: Either<UUID, String> = accountRepository
                .findById(username)
                .map { account -> check(account, password) }
                .orElseGet { createAccount(username, encoded) }
        return check.fold(
                { uuid: UUID -> handleSuccess(uuid, username) },
                { reason: String -> handleError(reason) })
    }

    private suspend fun handleAccountCreationConfirmed(confirmed: AccountCreationConfirmed) {
        val result = confirmed.toAccountResult()
        val item = Tuple2(confirmed.id.toJavaUUID(), result)
        broadcast.send(item)
    }

    private suspend fun handleAccountCreationFailed(failed: AccountCreationFailed) {
        val result = failed.toAccountResult()
        val item = Tuple2(failed.id.toJavaUUID(), result)
        broadcast.send(item)
    }

    private fun handleError(reason: String): Flow<AccountResult> = flowOf(AccountResult.Failed(reason))

    private fun handleSuccess(uuid: UUID, username: String): Flow<AccountResult> {
        val creation = ConfirmAccountCreation(uuid.toAvroUuid(), username)
        publisher.publish(username, creation)
        return broadcast
                .openSubscription()
                .consumeAsFlow()
                .filter { x: Tuple2<UUID, AccountResult> -> x.a == uuid }
                .map { obj: Tuple2<UUID, AccountResult> -> obj.b }
    }

    private fun check(account: Account, password: String): Either<UUID, String> {
        return if (passwordEncoder.matches(password, account.password)) {
            Either.Left(account.uuid)
        } else {
            log.error("Expected {}, but  was {}", password, account.password)
            Either.Right("incorrect password")
        }
    }

    private fun createAccount(username: String, password: String): Either<UUID, String> {
        val uuid = UUID.randomUUID()
        val account = Account(username, password, uuid)
        accountRepository.save(account)
        return Either.Left(uuid)
    }
}