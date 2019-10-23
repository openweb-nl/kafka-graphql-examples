package nl.openweb.graphql_endpoint.mapper;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import nl.openweb.data.Uuid;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidMapperTest {

    public static final String GENERATOR_NAMESPACE = "nl.openweb.topology.value-generator";

    @Test
    void fromString(){
        UUID javaUuid = UUID.randomUUID();

        Uuid avroUuid = UuidMapper.fromString(javaUuid.toString());

        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read(GENERATOR_NAMESPACE));
        IFn toUuid = Clojure.var(GENERATOR_NAMESPACE, "bytes->uuid");
        UUID result = (UUID)toUuid.invoke(avroUuid.bytes());
        assertThat(result).isEqualTo(javaUuid);
    }

    @Test
    void fromAvroUuid(){
        UUID javaUuid = UUID.randomUUID();

        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read(GENERATOR_NAMESPACE));
        IFn toBytes = Clojure.var(GENERATOR_NAMESPACE, "uuid->bytes");
        Uuid avroUuid = new Uuid((byte[])toBytes.invoke(javaUuid));
        UUID result = UuidMapper.fromAvroUuid(avroUuid);
        assertThat(result).isEqualTo(javaUuid);
    }
}
