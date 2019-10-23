package nl.openweb.graphql_endpoint.mapper

import clojure.java.api.Clojure
import clojure.lang.IFn
import nl.openweb.data.Uuid
import org.junit.jupiter.api.Test
import org.springframework.test.util.AssertionErrors.assertEquals
import toAvroUuid
import toJavaUUID
import java.util.*

internal class UuidMapperTest {
    @Test
    fun fromString() {
        val javaUuid = UUID.randomUUID()
        val avroUuid: Uuid = javaUuid.toString().toAvroUuid()
        val require: IFn = Clojure.`var`("clojure.core", "require")
        require.invoke(Clojure.read(GENERATOR_NAMESPACE))
        val toUuid: IFn = Clojure.`var`(GENERATOR_NAMESPACE, "bytes->uuid")
        val result = toUuid.invoke(avroUuid.bytes()) as UUID
        assertEquals("uuid correctly converted", javaUuid, result)
    }

    @Test
    fun fromAvroUuid() {
        val javaUuid = UUID.randomUUID()
        val require: IFn = Clojure.`var`("clojure.core", "require")
        require.invoke(Clojure.read(GENERATOR_NAMESPACE))
        val toBytes: IFn = Clojure.`var`(GENERATOR_NAMESPACE, "uuid->bytes")
        val avroUuid = Uuid(toBytes.invoke(javaUuid) as ByteArray)
        val result = avroUuid.toJavaUUID()
        assertEquals("uuid correctly converted", javaUuid, result)
    }

    companion object {
        const val GENERATOR_NAMESPACE = "nl.openweb.topology.value-generator"
    }
}