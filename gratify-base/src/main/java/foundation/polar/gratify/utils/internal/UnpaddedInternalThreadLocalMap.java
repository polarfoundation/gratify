//package foundation.polar.gratify.utils.internal;
//
//import foundation.polar.gratify.utils.concurrent.FastThreadLocal;
//
//import java.nio.charset.Charset;
//import java.nio.charset.CharsetDecoder;
//import java.nio.charset.CharsetEncoder;
//import java.util.ArrayList;
//import java.util.Map;
//import java.util.concurrent.ThreadLocalRandom;
//import java.util.concurrent.atomic.AtomicInteger;
//
//
///**
// * The internal data structure that stores the thread-local variables for Netty and all {@link FastThreadLocal}s.
// * Note that this class is for internal use only and is subject to change at any time.  Use {@link FastThreadLocal}
// * unless you know what you are doing.
// */
//class UnpaddedInternalThreadLocalMap {
//   static final ThreadLocal<InternalThreadLocalMap> slowThreadLocalMap = new ThreadLocal<>();
//   static final AtomicInteger nextIndex = new AtomicInteger();
//   /** Used by {@link FastThreadLocal} */
//   Object[] indexedVariables;
//
//   // Core thread-locals
//   int futureListenerStackDepth;
//   int localChannelReaderStackDepth;
//   Map<Class<?>, Boolean> handlerSharableCache;
//   ThreadLocalRandom random;
//   Map<Class<?>, TypeParameterMatcher> typeParameterMatcherGetCache;
//   Map<Class<?>, Map<String, TypeParameterMatcher>> typeParameterMatcherFindCache;
//
//   // String-related thread-locals
//   StringBuilder stringBuilder;
//   Map<Charset, CharsetEncoder> charsetEncoderCache;
//   Map<Charset, CharsetDecoder> charsetDecoderCache;
//
//   // ArrayList-related thread-locals
//   ArrayList<Object> arrayList;
//
//   UnpaddedInternalThreadLocalMap(Object[] indexedVariables) {
//      this.indexedVariables = indexedVariables;
//   }
//}
