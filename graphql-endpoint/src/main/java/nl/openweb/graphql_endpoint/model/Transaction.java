package nl.openweb.graphql_endpoint.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "transaction")
@Data
public class Transaction {

    @Id
    @GeneratedValue
    private Integer id;
    private String changed_by;
    private String descr;
    @Enumerated(value = EnumType.STRING)
    private DType direction;
    private String from_to;
    private String iban;
    private String new_balance;
    @Transient
    private Long amount;
}
