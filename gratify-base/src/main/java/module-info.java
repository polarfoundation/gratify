module gratify.base {
   requires java.compiler;
   requires jdk.compiler;
   requires java.management;
   requires transitive java.desktop;
   requires transitive org.apache.logging.log4j;
   requires java.logging;
   requires org.checkerframework.checker.qual;
   requires org.slf4j;
   requires java.annotation;
   requires jopt.simple;
   requires reactor.core;
   requires org.reactivestreams; // migratory
   requires org.objectweb.asm;
   requires org.aspectj.weaver;
   requires commons.logging;

   exports foundation.polar.gratify.utils;
   exports foundation.polar.gratify.core;
   exports foundation.polar.gratify.core.io.support to gratify.artifacts;
   exports foundation.polar.gratify.core.convert;
   exports foundation.polar.gratify.utils.logging;
   exports foundation.polar.gratify.ds;
   exports foundation.polar.gratify.core.io;
   exports foundation.polar.gratify.env;
   exports foundation.polar.gratify.inject;
}