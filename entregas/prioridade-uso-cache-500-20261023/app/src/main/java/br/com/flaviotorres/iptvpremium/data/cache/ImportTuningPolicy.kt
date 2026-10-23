package br.com.flaviotorres.iptvpremium.data.cache

/** Diagnostic profiles; release always uses the 500-item defaults for both formats. No full-catalog materialization. */
internal enum class ImportProfile(val forceWal: Boolean, val normal: Boolean, val batchSize: Int, val batchCharacters: Int,
    val m3uBatchSize: Int = 500,
    val m3uBatchCharacters: Int = 512 * 1024
) {
    PADRAO(false, false, 500, 256 * 1024),
    ATUAL(false, false, 500, 256 * 1024),
    WAL(true, false, 500, 256 * 1024),
    NORMAL(true, true, 500, 256 * 1024),
    LOTES(true, true, 500, 256 * 1024),
    XTREAM_1000(false, false, 500, 256 * 1024),
    XTREAM_500(false, false, 500, 256 * 1024),
    XTREAM_650(false, false, 500, 256 * 1024);
    companion object {
        val DEFAULT: ImportProfile get() = PADRAO
        fun stored(value: String?, debug: Boolean): ImportProfile =
            if (debug) entries.firstOrNull { it.name == value } ?: DEFAULT else DEFAULT
        fun requested(value: String): ImportProfile = entries.firstOrNull { it.name == value }
            ?: throw IllegalArgumentException("Perfil de importacao invalido.")
    }
}
internal object ImportTuningState {
    @Volatile var profile: ImportProfile = ImportProfile.DEFAULT
}
