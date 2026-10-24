# Keep original packages/names. Obfuscation is unnecessary for this performance comparison.
-dontobfuscate
# Protect Kotlin's package-private facade hierarchies from merging/relocation.
-keep class kotlin.** { *; }
# Preserve native LibVLC bindings.
-keep class org.videolan.** { *; }
# App and AndroidX optimizations remain enabled by the build-type configuration.
-keep,allowoptimization,allowshrinking class br.com.flaviotorres.iptvpremium.** { *; }
-keepattributes SourceFile,LineNumberTable
