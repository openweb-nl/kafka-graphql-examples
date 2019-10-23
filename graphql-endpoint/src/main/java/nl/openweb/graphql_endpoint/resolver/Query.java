package nl.openweb.graphql_endpoint.resolver;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import lombok.AllArgsConstructor;
import nl.openweb.graphql_endpoint.model.Transaction;
import nl.openweb.graphql_endpoint.service.TransactionService;
import org.springframework.stereotype.Component;

import java.util.List;

@AllArgsConstructor
@Component
public class Query implements GraphQLQueryResolver {

    private final TransactionService transactionService;

    List<Transaction> all_last_transactions() {
        return transactionService.allLastTransactions();
    }

    Transaction transaction_by_id(int id) {
        return transactionService.transactionById(id);
    }

    List<Transaction> transactions_by_iban(String iban, int maxItems) {
        return transactionService.transactionsByIban(iban, maxItems);
    }
}
