# kotlinx.serialization keeps generated serializers reachable via reflection on the
# companion; R8 cannot see those edges, so they are pinned explicitly here.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Our @Serializable DTOs and their synthesized serializers.
-keep,includedescriptorclasses class com.ritik.wordpuzzle.data.model.**$$serializer { *; }
-keepclassmembers class com.ritik.wordpuzzle.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.ritik.wordpuzzle.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
