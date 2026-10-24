# Preserve native LibVLC bindings. This test does not optimize its JNI API.
-keep class org.videolan.** { *; }
# Keep app stack samples readable while allowing optimizer transformations.
-keep,allowoptimization,allowshrinking class br.com.flaviotorres.iptvpremium.** { *; }
-keepattributes SourceFile,LineNumberTable
