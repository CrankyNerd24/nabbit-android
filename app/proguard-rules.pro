# Play's review library refers to an annotation that isn't shipped with it; it's only
# there for its own builds, so nothing at run time needs it.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# Glance finds the widget buttons' callback by class name.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
