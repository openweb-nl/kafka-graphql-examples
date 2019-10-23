package nl.openweb.graphql_endpoint.model;

import lombok.Data;

import java.util.UUID;

@Data
public class MoneyTransferResult {
    private String reason;
    private boolean success;
    private UUID uuid;
}
