package nl.openweb.graphql_endpoint.resolver;

import com.coxautodev.graphql.tools.GraphQLSubscriptionResolver;
import lombok.AllArgsConstructor;
import nl.openweb.graphql_endpoint.model.AccountResult;
import nl.openweb.graphql_endpoint.model.DType;
import nl.openweb.graphql_endpoint.model.MoneyTransferResult;
import nl.openweb.graphql_endpoint.model.Transaction;
import nl.openweb.graphql_endpoint.service.AccountCreationService;
import nl.openweb.graphql_endpoint.service.MoneyTransferService;
import nl.openweb.graphql_endpoint.service.TransactionService;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class Subscription implements GraphQLSubscriptionResolver {

    private final AccountCreationService accountCreationService;
    private final MoneyTransferService moneyTransferService;
    private final TransactionService transactionService;

    Publisher<AccountResult> get_account(String username, String password) {
        return accountCreationService.getAccount(username, password);
    }

    Publisher<MoneyTransferResult> money_transfer(long amount, String descr, String from, String to, String token,
            String username, String uuid) {
        return moneyTransferService.transfer(amount, descr, from, to, token, username, uuid);
    }

    Publisher<Transaction> stream_transactions(DType direction, String iban, Long minAmount, Long maxAmount,
            String descrIncludes) {
        return transactionService.stream(direction, iban, minAmount, maxAmount, descrIncludes);
    }
}
