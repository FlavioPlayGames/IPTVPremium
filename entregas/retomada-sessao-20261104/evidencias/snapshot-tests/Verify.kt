package br.com.flaviotorres.iptvpremium.core.session

fun main() {
    var count=0
    fun checkCase(label:String,condition:Boolean){check(condition){label};count++;println("PASS $label")}
    val original=ByteArray(8192){(it*31).toByte()}
    val encoded=SessionSnapshotCodec.encode(original,112)
    checkCase("snapshot survives a fresh decode with exact bytes",SessionSnapshotCodec.decode(encoded.copyOf(),112)?.contentEquals(original)==true)
    checkCase("another application version cannot consume old parcel",SessionSnapshotCodec.decode(encoded,113)==null)
    checkCase("empty snapshot is rejected",runCatching{SessionSnapshotCodec.encode(byteArrayOf(),112)}.isFailure)
    checkCase("empty file is rejected",SessionSnapshotCodec.decode(byteArrayOf(),112)==null)
    checkCase("incomplete header is rejected",SessionSnapshotCodec.decode(encoded.copyOf(19),112)==null)
    checkCase("interrupted write cannot restore partial UI",SessionSnapshotCodec.decode(encoded.copyOf(encoded.size-1),112)==null)
    checkCase("extra trailing bytes are rejected",SessionSnapshotCodec.decode(encoded+byteArrayOf(1),112)==null)
    val corrupt=encoded.copyOf().also{it[it.lastIndex]=(it.last()+1).toByte()}
    checkCase("corrupted saved state is rejected",SessionSnapshotCodec.decode(corrupt,112)==null)
    checkCase("unknown format is rejected",SessionSnapshotCodec.decode(encoded.copyOf().also{it[7]=2},112)==null)
    checkCase("wrong file signature is rejected",SessionSnapshotCodec.decode(encoded.copyOf().also{it[0]=0},112)==null)
    checkCase("declared oversized length is rejected",SessionSnapshotCodec.decode(encoded.copyOf().also{for(i in 12..15)it[i]=127},112)==null)
    checkCase("oversized input cannot allocate an unbounded state",SessionSnapshotCodec.decode(ByteArray(SessionSnapshotCodec.MAX_BYTES+21),112)==null)
    checkCase("oversized snapshot cannot be written",runCatching{SessionSnapshotCodec.encode(ByteArray(SessionSnapshotCodec.MAX_BYTES+1),112)}.isFailure)
    val maximum=ByteArray(SessionSnapshotCodec.MAX_BYTES){(it%251).toByte()}
    checkCase("maximum bounded snapshot round trips",SessionSnapshotCodec.decode(SessionSnapshotCodec.encode(maximum,112),112)?.contentEquals(maximum)==true)
    val small=byteArrayOf(17)
    checkCase("small snapshot round trips",SessionSnapshotCodec.decode(SessionSnapshotCodec.encode(small,112),112)?.contentEquals(small)==true)
    checkCase("repeated decode does not consume or alter saved state",SessionSnapshotCodec.decode(encoded,112)?.contentEquals(original)==true && SessionSnapshotCodec.decode(encoded,112)?.contentEquals(original)==true)
    println("$count snapshot checks passed; Android Bundle lifecycle requires device validation")
}
