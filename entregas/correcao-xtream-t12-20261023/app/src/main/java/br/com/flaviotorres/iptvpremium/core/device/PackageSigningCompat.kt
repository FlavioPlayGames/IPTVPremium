package br.com.flaviotorres.iptvpremium.core.device

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/** Keep certificate retrieval identical on Android 12 and newer devices. */
internal object PackageSigningCompat {
    @Suppress("DEPRECATION")
    fun installed(pm: PackageManager, packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else if (Build.VERSION.SDK_INT >= 28) {
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        }

    @Suppress("DEPRECATION")
    fun archive(pm: PackageManager, path: String): PackageInfo? =
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else if (Build.VERSION.SDK_INT >= 28) {
            pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNATURES)
        }
    @Suppress("DEPRECATION")
    fun version(info: PackageInfo): Long = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    fun signers(info: PackageInfo, lineage: Boolean = false): List<android.content.pm.Signature> =
        if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.let { signing ->
                (if (lineage && !signing.hasMultipleSigners()) signing.signingCertificateHistory else signing.apkContentsSigners)?.toList()
            }.orEmpty()
        } else info.signatures?.toList().orEmpty()
}
