package nl.openweb.graphql_endpoint.model;

import lombok.Data;

@Data
public class AccountResult {
    private String iban;
    private String reason;
    private String token;
}
