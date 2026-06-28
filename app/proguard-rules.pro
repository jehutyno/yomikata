# ============================================================================
# Règles R8 / ProGuard — Yomikata Z
# ============================================================================
# La plupart des bibliothèques (Room, Compose, Firebase, Media3, Kodein 7.x,
# AndroidX) embarquent leurs propres "consumer rules" → très peu de règles
# manuelles nécessaires ici. Ce fichier ne garde que ce qui est spécifique à
# l'app et non couvert automatiquement.

# ----------------------------------------------------------------------------
# Crashlytics : conserver les infos de débogage pour des stack traces lisibles
# (le mapping.txt est uploadé automatiquement par le plugin firebase-crashlytics
#  pour les builds release minifiés).
# ----------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-renamesourcefileattribute SourceFile

# ----------------------------------------------------------------------------
# Vues custom inflatées depuis XML (libs jitpack : KenBurnsView, CircleImageView,
# HiraganaEditText, VerticalSeekBar, Crescento, ExpandableTextView, Calligraphy…).
# Android les instancie par réflexion via les constructeurs (Context, AttributeSet).
# ----------------------------------------------------------------------------
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ----------------------------------------------------------------------------
# Méthodes natives
# ----------------------------------------------------------------------------
-keepclasseswithmembernames class * {
    native <methods>;
}

# ----------------------------------------------------------------------------
# Enums : values()/valueOf() conservés (réflexion + Parcel)
# ----------------------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ----------------------------------------------------------------------------
# Parcelable : conserver le champ CREATOR
# ----------------------------------------------------------------------------
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ----------------------------------------------------------------------------
# Modèles : transportés via Bundle (Parcelable + Serializable) à travers toute
# l'app. On conserve la structure pour éviter toute surprise de (dé)sérialisation.
# ----------------------------------------------------------------------------
-keep class com.jehutyno.yomikata.model.** { *; }
