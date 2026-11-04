package br.com.flaviotorres.iptvpremium.core.session

import java.nio.ByteBuffer
import java.util.zip.CRC32

/** Bounded, versioned local snapshot. No expiry: elapsed time does not discard a session. */
internal object SessionSnapshotCodec {
    const val MAX_BYTES=4*1024*1024
    private const val MAGIC=0x49505456
    fun encode(payload:ByteArray,version:Int):ByteArray {
        require(payload.isNotEmpty() && payload.size<=MAX_BYTES)
        return ByteBuffer.allocate(20+payload.size).putInt(MAGIC).putInt(1).putInt(version)
            .putInt(payload.size).putInt(CRC32().apply{update(payload)}.value.toInt()).put(payload).array()
    }
    fun decode(bytes:ByteArray,version:Int):ByteArray?=runCatching {
        if(bytes.size !in 21..MAX_BYTES+20)return null
        val data=ByteBuffer.wrap(bytes)
        if(data.int!=MAGIC || data.int!=1 || data.int!=version)return null
        val size=data.int;val checksum=data.int
        if(size!=data.remaining())return null
        ByteArray(size).also{data.get(it);if(CRC32().apply{update(it)}.value.toInt()!=checksum)return null}
    }.getOrNull()
}
