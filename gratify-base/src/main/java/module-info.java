module gratify.base {
   requires java.compiler;
   requires jdk.compiler;
   requires java.management;
   requires org.checkerframework.checker.qual;
   exports foundation.polar.gratify.utils;
   exports foundation.polar.gratify.buffer;
}