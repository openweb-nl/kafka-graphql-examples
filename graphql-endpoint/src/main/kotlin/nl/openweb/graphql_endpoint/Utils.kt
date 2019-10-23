package nl.openweb.graphql_endpoint

import nl.openweb.data.Uuid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.*

fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy { LoggerFactory.getLogger(this.javaClass) }
}

fun String.toAvroUuid(): Uuid {
    return UUID.fromString(this).toAvroUuid()
}

fun UUID.toAvroUuid(): Uuid {
    val buffer = ByteBuffer.allocate(16)
    buffer.putLong(this.mostSignificantBits)
    buffer.putLong(this.leastSignificantBits)
    return Uuid(buffer.array())
}

fun Uuid.toJavaUUID(): UUID {
    val buffer = ByteBuffer.wrap(this.bytes())
    val mostSignificant = buffer.long
    val leastSignificant = buffer.long
    return UUID(mostSignificant, leastSignificant)
}
