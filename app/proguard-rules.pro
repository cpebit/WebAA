# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn sun.misc.Unsafe
-dontwarn com.google.protobuf.**
-dontwarn sun.reflect.**
-dontwarn org.mockito.**
-dontwarn android.net.**
-dontwarn com.android.dx.mockito.**
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn java.lang.instrument.IllegalClassFormatException
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl
-dontwarn org.newsclub.net.unix.AFUNIXSocket
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn org.slf4j.impl.StaticLoggerBinder

-keepattributes *Annotation*
-keepattributes InnerClasses

-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class it.cpeb.teslaa.proto** {*;}
-keep class it.cpeb.teslaa.helpers.video.JpegDecoder {*; }
-keep class org.mockito** {*;}
-keep class android.net** {*;}
-keep class com.android.dx.mockito** {*;}

# --- KEEP PROTOBUF GENERATED CLASSES ---

# Keep all generated message classes and their builder classes
-keep class com.google.protobuf.** { *; }

# Keep all classes that extend GeneratedMessage or GeneratedMessageLite
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class * extends com.google.protobuf.GeneratedMessage { *; }

# Don't strip EnumValueDescriptor reflection (used internally)
-keepclassmembers class com.google.protobuf.Descriptors$FieldDescriptor {
    private final java.lang.String name;
    private final int number;
    private final java.lang.Object defaultValue;
}

# Don't strip used extension registry methods
-keepclassmembers class com.google.protobuf.ExtensionRegistry* {
    public static ** getEmptyRegistry(...);
}

# If you use TextFormat or reflection printing
-keepclassmembers class com.google.protobuf.TextFormat$Printer {
    *;
}

# Needed for reflection in builders (causing your exact error)
-keepclassmembers class * {
    public *** getFieldPresence();
}

# Needed for bouncycastle to work properly
-keep class javax.naming.** { *; }
-dontwarn javax.naming.**
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }
