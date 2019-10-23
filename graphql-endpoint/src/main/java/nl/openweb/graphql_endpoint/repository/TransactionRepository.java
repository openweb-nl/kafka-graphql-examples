package nl.openweb.graphql_endpoint.repository;

import nl.openweb.graphql_endpoint.model.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TransactionRepository extends CrudRepository<Transaction, Integer> {

    @Query(
            value = "SELECT * FROM transaction WHERE id IN (SELECT MAX(id) FROM transaction GROUP BY iban) ORDER BY iban",
            nativeQuery = true
    )
    List<Transaction> allLastTransactions();

    List<Transaction> findAllByIbanOrderByIdDesc(String iban, Pageable pageable);
}
