package foundation.polar.gratify.lang;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.annotation.*;

@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@NonNull
public @interface NonNullFields {}
