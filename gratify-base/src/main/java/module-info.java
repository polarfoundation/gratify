module gratify.base {
   requires java.compiler;
   requires jdk.compiler;
   requires java.management;
   requires org.checkerframework.checker.qual;
   requires java.desktop;
   requires commons.logging;
   requires java.logging;
   requires org.apache.logging.log4j;
   requires org.slf4j;
   requires java.annotation;
   requires jopt.simple;
   requires reactor.core;
   requires org.reactivestreams;

   exports foundation.polar.gratify.utils;
   exports foundation.polar.gratify.core;
   exports foundation.polar.gratify.core.convert;
}