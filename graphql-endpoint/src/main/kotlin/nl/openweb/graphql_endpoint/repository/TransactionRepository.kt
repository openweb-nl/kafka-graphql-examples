package nl.openweb.graphql_endpoint.repository

import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.CrudRepository
import nl.openweb.graphql_endpoint.model.Transaction

@Repository
interface TransactionRepository : CrudRepository<Transaction, Int> {
    @Query(value = "SELECT * FROM transaction WHERE id IN (SELECT MAX(id) FROM transaction GROUP BY iban) ORDER BY iban", nativeQuery = true)
    fun allLastTransactions(): List<Transaction>

    fun findAllByIbanOrderByIdDesc(iban: String, pageable: Pageable): List<Transaction>
}