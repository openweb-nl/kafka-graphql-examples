package nl.openweb.graphql_endpoint.model

import nl.openweb.data.BalanceChanged
import java.text.NumberFormat
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "transaction")
class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var id: Int,
    var changed_by: String,
    var descr: String,
    @Enumerated(value = EnumType.STRING)
    var direction: DType,
    var from_to: String,
    var iban: String,
    var new_balance: String,
    @Transient
    var amount: Long
)

private val format = NumberFormat.getCurrencyInstance(Locale("nl", "NL"))
fun BalanceChanged.toTransaction(): Transaction =
        Transaction(
                0,
                toCurrency(Math.abs(this.changedBy)),
                this.description,
                if (this.changedBy < 0L) DType.DEBIT else DType.CREDIT,
                this.fromTo,
                this.iban,
                toCurrency(this.newBalance),
                this.changedBy
        )

private fun toCurrency(amount: Long): String {
    return format.format(amount * 1.0 / 100)
}

fun Transaction.hasDirection(direction: DType?): Boolean = direction?.equals(this.direction) ?: true

fun Transaction.hasIban(iban: String?): Boolean = iban?.equals(this.iban) ?: true

fun Transaction.hasMinAmount(minAmount: Int?) = minAmount?.let { amount >= it } ?: true

fun Transaction.hasMaxAmount(maxAmount: Int?) = maxAmount?.let { amount <= it } ?: true

fun Transaction.containsDesc(descrIncluded: String?) =
        descrIncluded?.let { descr.contains(descrIncluded, true) } ?: true