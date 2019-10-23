package nl.openweb.graphql_endpoint.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "account")
@Data
public class Account {
    @Id
    private String username;
    private String password;
    private UUID uuid;
}
