package nl.openweb.graphql_endpoint.model

import nl.openweb.data.AccountCreationConfirmed
import nl.openweb.data.AccountCreationFailed

sealed class AccountResult(val iban: String?, val reason: String?, val token: String?) {
    class Succeeded(iban: String, token: String) : AccountResult(iban, null, token)
    class Failed(reason: String) : AccountResult(null, reason, null)
}

fun AccountCreationConfirmed.toAccountResult(): AccountResult {
    return AccountResult.Succeeded(this.iban, this.token)
}

fun AccountCreationFailed.toAccountResult(): AccountResult {
    return AccountResult.Failed(this.reason)
}