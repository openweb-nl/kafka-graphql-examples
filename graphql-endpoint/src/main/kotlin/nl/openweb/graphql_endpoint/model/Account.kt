package nl.openweb.graphql_endpoint.model

import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "account")
class Account(
    @Id
    var username: String,
    var password: String,
    var uuid: UUID
)