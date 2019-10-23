package nl.openweb.graphql_endpoint.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {
    @NotEmpty
    private String commandsTopic;
}
