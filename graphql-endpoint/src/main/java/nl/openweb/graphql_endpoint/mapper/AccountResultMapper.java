package nl.openweb.graphql_endpoint.mapper;

import lombok.experimental.UtilityClass;
import nl.openweb.data.AccountCreationConfirmed;
import nl.openweb.data.AccountCreationFailed;
import nl.openweb.graphql_endpoint.model.AccountResult;

@UtilityClass
public class AccountResultMapper {

    public AccountResult fromAccountCreationFailed(AccountCreationFailed failed){
        AccountResult result = new AccountResult();
        result.setReason(failed.getReason());
        return result;
    }

    public AccountResult fromAccountCreationConfirmed(AccountCreationConfirmed confirmed){
        AccountResult result = new AccountResult();
        result.setIban(confirmed.getIban());
        result.setToken(confirmed.getToken());
        return result;
    }
}
