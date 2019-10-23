package nl.openweb.graphql_endpoint.mapper;

import lombok.experimental.UtilityClass;
import nl.openweb.data.MoneyTransferConfirmed;
import nl.openweb.data.MoneyTransferFailed;
import nl.openweb.graphql_endpoint.model.MoneyTransferResult;

import java.util.UUID;

@UtilityClass
public class MoneyTransferResultMapper {

    public MoneyTransferResult fromMoneyTransferFailed(MoneyTransferFailed failed){
        MoneyTransferResult result = new MoneyTransferResult();
        result.setReason(failed.getReason());
        result.setSuccess(false);
        result.setUuid(UUID.nameUUIDFromBytes(failed.getId().bytes()));
        return result;
    }

    public MoneyTransferResult fromMoneyTransferConfirmed(MoneyTransferConfirmed confirmed){
        MoneyTransferResult result = new MoneyTransferResult();
        result.setSuccess(true);
        result.setUuid(UUID.nameUUIDFromBytes(confirmed.getId().bytes()));
        return result;
    }
}
