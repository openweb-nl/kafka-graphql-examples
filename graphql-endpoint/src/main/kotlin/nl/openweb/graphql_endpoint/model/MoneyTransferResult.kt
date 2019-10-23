package nl.openweb.graphql_endpoint.model

import nl.openweb.data.MoneyTransferConfirmed
import nl.openweb.data.MoneyTransferFailed
import nl.openweb.graphql_endpoint.toJavaUUID
import java.util.*

sealed class MoneyTransferResult(val reason: String?, val success: Boolean, val uuid: String) {
    class Succeeded(uuid: UUID) : MoneyTransferResult(null, true, uuid.toString())
    class Failed(uuid: UUID, reason: String) : MoneyTransferResult(reason, false, uuid.toString())
}

fun MoneyTransferConfirmed.toMoneyTransferResult(): MoneyTransferResult =
        MoneyTransferResult.Succeeded(this.id.toJavaUUID())

fun MoneyTransferFailed.toMoneyTransferResult(): MoneyTransferResult =
        MoneyTransferResult.Failed(this.id.toJavaUUID(), this.reason)