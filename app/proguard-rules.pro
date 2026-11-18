# Keep generic type signatures and runtime-visible annotations (Firestore @DocumentId, @ServerTimestamp, @PropertyName)
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod

# Keep all Firestore POJO models and nested data classes intact for reflective toObject/toObjects deserialization
-keep class com.example.data.model.** { *; }

# Keep stack trace line numbers with anonymized source file name for production crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

