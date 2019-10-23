package nl.openweb.graphql_endpoint.mapper;

import lombok.experimental.UtilityClass;
import nl.openweb.data.Uuid;

import java.nio.ByteBuffer;
import java.util.UUID;

@UtilityClass
public class UuidMapper {

    public Uuid fromString(String name){
        return fromJavaUuid(UUID.fromString(name));
    }

    public Uuid fromJavaUuid(UUID javaUuid){
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(javaUuid.getMostSignificantBits());
        buffer.putLong(javaUuid.getLeastSignificantBits());
        return new Uuid(buffer.array());
    }

    public UUID fromAvroUuid(Uuid avroUuid){
        ByteBuffer buffer = ByteBuffer.wrap(avroUuid.bytes());
        long mostSignificant = buffer.getLong();
        long leastSignificant = buffer.getLong();
        return new UUID(mostSignificant, leastSignificant);
    }
}
