import nl.openweb.data.Uuid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.*

sealed class Either<out A, out B> {
    class Left<A>(val value: A) : Either<A, Nothing>()
    class Right<B>(val value: B) : Either<Nothing, B>()
}

inline fun <A, B, V> Either<A, B>.fold(lf: (A) -> V, rf: (B) -> V): V = when (this) {
    is Either.Left -> lf(this.value)
    is Either.Right -> rf(this.value)
}

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
