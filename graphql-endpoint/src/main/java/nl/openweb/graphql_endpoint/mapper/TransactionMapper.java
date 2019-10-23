package nl.openweb.graphql_endpoint.mapper;

import lombok.experimental.UtilityClass;
import nl.openweb.data.BalanceChanged;
import nl.openweb.graphql_endpoint.model.DType;
import nl.openweb.graphql_endpoint.model.Transaction;

import java.text.NumberFormat;
import java.util.Locale;

@UtilityClass
public class TransactionMapper {

    private NumberFormat format =
            NumberFormat.getCurrencyInstance(new Locale("nl", "NL"));

    public Transaction fromBalanceChanged(BalanceChanged changed) {
        Transaction transaction = new Transaction();
        transaction.setIban(changed.getIban());
        transaction.setNew_balance(toCurrency(changed.getNewBalance()));
        transaction.setChanged_by(toCurrency(Math.abs(changed.getChangedBy())));
        transaction.setFrom_to(changed.getFromTo());
        transaction.setDirection(changed.getChangedBy() < 0L ? DType.DEBIT : DType.CREDIT);
        transaction.setDescr(changed.getDescription());
        transaction.setAmount(changed.getChangedBy());
        return transaction;
    }

    private String toCurrency(Long amount) {
        return format.format((amount * 1.0) / 100);
    }
}
