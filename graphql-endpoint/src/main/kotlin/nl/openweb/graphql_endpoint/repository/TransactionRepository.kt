package nl.openweb.graphql_endpoint.repository

import nl.openweb.graphql_endpoint.model.Transaction
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TransactionRepository : CrudRepository<Transaction, Int> {
    @Query(value = "SELECT * FROM transaction WHERE id IN (SELECT MAX(id) FROM transaction GROUP BY iban) ORDER BY iban", nativeQuery = true)
    fun allLastTransactions(): List<Transaction>

    fun findAllByIbanOrderByIdDesc(iban: String, pageable: Pageable): List<Transaction>
}