package nl.openweb.graphql_endpoint.kafka;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.openweb.graphql_endpoint.properties.KafkaProperties;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class CommandPublisher {

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String username, SpecificRecordBase avroRecord) {
        kafkaTemplate.send(kafkaProperties.getCommandsTopic(), username, avroRecord);
    }
}
