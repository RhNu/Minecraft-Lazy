package rhx.lazy.core.testing

import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps

internal fun <T> Codec<T>.jsonRoundTrip(value: T): T {
    val encoded =
        encodeStart(JsonOps.INSTANCE, value)
            .result()
            .orElseThrow()
    return parse(JsonOps.INSTANCE, encoded)
        .result()
        .orElseThrow()
}
