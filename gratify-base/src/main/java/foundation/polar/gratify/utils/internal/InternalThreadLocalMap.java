//package foundation.polar.gratify.utils.internal;
//
//import foundation.polar.gratify.utils.logging.LoggerFactory;
//import org.apache.commons.logging.Log;
//
//import java.nio.charset.Charset;
//import java.nio.charset.CharsetDecoder;
//import java.nio.charset.CharsetEncoder;
//import java.util.*;
//import java.util.concurrent.ThreadLocalRandom;
//
///**
// * The internal data structure that stores the thread-local variables for Netty and all {@link FastThreadLocal}s.
// * Note that this class is for internal use only and is subject to change at any time.  Use {@link FastThreadLocal}
// * unless you know what you are doing.
// */
//public final class InternalThreadLocalMap extends UnpaddedInternalThreadLocalMap {
//
//   private static final Log logger = LoggerFactory.getLog(InternalThreadLocalMap.class);
//
//   private static final int DEFAULT_ARRAY_LIST_INITIAL_CAPACITY = 8;
//   private static final int STRING_BUILDER_INITIAL_SIZE;
//   private static final int STRING_BUILDER_MAX_SIZE;
//
//   public static final Object UNSET = new Object();
//
//   private BitSet cleanerFlags;
//
//   static {
//      STRING_BUILDER_INITIAL_SIZE =
//         SystemPropertyUtil.getInt("foundation.polar.gratify.threadLocalMap.stringBuilder.initialSize", 1024);
//      logger.debug(String.format("-Dfoundation.polar.gratify.threadLocalMap.stringBuilder.initialSize: %s", STRING_BUILDER_INITIAL_SIZE));
//
//      STRING_BUILDER_MAX_SIZE = SystemPropertyUtil.getInt("foundation.polar.gratify.threadLocalMap.stringBuilder.maxSize", 1024 * 4);
//      logger.debug(String.format("-Dfoundation.polar.gratify.threadLocalMap.stringBuilder.maxSize: %s", STRING_BUILDER_MAX_SIZE));
//   }
//
//   public static InternalThreadLocalMap getIfSet() {
//      Thread thread = Thread.currentThread();
//      if (thread instanceof FastThreadLocalThread) {
//         return ((FastThreadLocalThread) thread).threadLocalMap();
//      }
//      return slowThreadLocalMap.get();
//   }
//
//   public static InternalThreadLocalMap get() {
//      Thread thread = Thread.currentThread();
//      if (thread instanceof FastThreadLocalThread) {
//         return fastGet((FastThreadLocalThread) thread);
//      } else {
//         return slowGet();
//      }
//   }
//
//   private static InternalThreadLocalMap fastGet(FastThreadLocalThread thread) {
//      InternalThreadLocalMap threadLocalMap = thread.threadLocalMap();
//      if (threadLocalMap == null) {
//         thread.setThreadLocalMap(threadLocalMap = new InternalThreadLocalMap());
//      }
//      return threadLocalMap;
//   }
//
//   private static InternalThreadLocalMap slowGet() {
//      ThreadLocal<InternalThreadLocalMap> slowThreadLocalMap = UnpaddedInternalThreadLocalMap.slowThreadLocalMap;
//      InternalThreadLocalMap ret = slowThreadLocalMap.get();
//      if (ret == null) {
//         ret = new InternalThreadLocalMap();
//         slowThreadLocalMap.set(ret);
//      }
//      return ret;
//   }
//
//   public static void remove() {
//      Thread thread = Thread.currentThread();
//      if (thread instanceof FastThreadLocalThread) {
//         ((FastThreadLocalThread) thread).setThreadLocalMap(null);
//      } else {
//         slowThreadLocalMap.remove();
//      }
//   }
//
//   public static void destroy() {
//      slowThreadLocalMap.remove();
//   }
//
//   public static int nextVariableIndex() {
//      int index = nextIndex.getAndIncrement();
//      if (index < 0) {
//         nextIndex.decrementAndGet();
//         throw new IllegalStateException("too many thread-local indexed variables");
//      }
//      return index;
//   }
//
//   public static int lastVariableIndex() {
//      return nextIndex.get() - 1;
//   }
//
//   // Cache line padding (must be public)
//   // With CompressedOops enabled, an instance of this class should occupy at least 128 bytes.
//   public long rp1, rp2, rp3, rp4, rp5, rp6, rp7, rp8, rp9;
//
//   private InternalThreadLocalMap() {
//      super(newIndexedVariableTable());
//   }
//
//   private static Object[] newIndexedVariableTable() {
//      Object[] array = new Object[32];
//      Arrays.fill(array, UNSET);
//      return array;
//   }
//
//   public int size() {
//      int count = 0;
//
//      if (futureListenerStackDepth != 0) {
//         count ++;
//      }
//      if (localChannelReaderStackDepth != 0) {
//         count ++;
//      }
//      if (handlerSharableCache != null) {
//         count ++;
//      }
//      if (random != null) {
//         count ++;
//      }
//      if (typeParameterMatcherGetCache != null) {
//         count ++;
//      }
//      if (typeParameterMatcherFindCache != null) {
//         count ++;
//      }
//      if (stringBuilder != null) {
//         count ++;
//      }
//      if (charsetEncoderCache != null) {
//         count ++;
//      }
//      if (charsetDecoderCache != null) {
//         count ++;
//      }
//      if (arrayList != null) {
//         count ++;
//      }
//
//      for (Object o: indexedVariables) {
//         if (o != UNSET) {
//            count ++;
//         }
//      }
//
//      // We should subtract 1 from the count because the first element in 'indexedVariables' is reserved
//      // by 'FastThreadLocal' to keep the list of 'FastThreadLocal's to remove on 'FastThreadLocal.removeAll()'.
//      return count - 1;
//   }
//
//   public StringBuilder stringBuilder() {
//      StringBuilder sb = stringBuilder;
//      if (sb == null) {
//         return stringBuilder = new StringBuilder(STRING_BUILDER_INITIAL_SIZE);
//      }
//      if (sb.capacity() > STRING_BUILDER_MAX_SIZE) {
//         sb.setLength(STRING_BUILDER_INITIAL_SIZE);
//         sb.trimToSize();
//      }
//      sb.setLength(0);
//      return sb;
//   }
//
//   public Map<Charset, CharsetEncoder> charsetEncoderCache() {
//      Map<Charset, CharsetEncoder> cache = charsetEncoderCache;
//      if (cache == null) {
//         charsetEncoderCache = cache = new IdentityHashMap<>();
//      }
//      return cache;
//   }
//
//   public Map<Charset, CharsetDecoder> charsetDecoderCache() {
//      Map<Charset, CharsetDecoder> cache = charsetDecoderCache;
//      if (cache == null) {
//         charsetDecoderCache = cache = new IdentityHashMap<>();
//      }
//      return cache;
//   }
//
//   public <E> ArrayList<E> arrayList() {
//      return arrayList(DEFAULT_ARRAY_LIST_INITIAL_CAPACITY);
//   }
//
//   @SuppressWarnings("unchecked")
//   public <E> ArrayList<E> arrayList(int minCapacity) {
//      ArrayList<E> list = (ArrayList<E>) arrayList;
//      if (list == null) {
//         arrayList = new ArrayList<>(minCapacity);
//         return (ArrayList<E>) arrayList;
//      }
//      list.clear();
//      list.ensureCapacity(minCapacity);
//      return list;
//   }
//
//   public int futureListenerStackDepth() {
//      return futureListenerStackDepth;
//   }
//
//   public void setFutureListenerStackDepth(int futureListenerStackDepth) {
//      this.futureListenerStackDepth = futureListenerStackDepth;
//   }
//
//   public ThreadLocalRandom random() {
//      ThreadLocalRandom r = random;
//      if (r == null) {
//         random = r = ThreadLocalRandom.current();
//      }
//      return r;
//   }
//
//   public Map<Class<?>, TypeParameterMatcher> typeParameterMatcherGetCache() {
//      Map<Class<?>, TypeParameterMatcher> cache = typeParameterMatcherGetCache;
//      if (cache == null) {
//         typeParameterMatcherGetCache = cache = new IdentityHashMap<>();
//      }
//      return cache;
//   }
//
//   public Map<Class<?>, Map<String, TypeParameterMatcher>> typeParameterMatcherFindCache() {
//      Map<Class<?>, Map<String, TypeParameterMatcher>> cache = typeParameterMatcherFindCache;
//      if (cache == null) {
//         typeParameterMatcherFindCache = cache = new IdentityHashMap<>();
//      }
//      return cache;
//   }
//
//   public Map<Class<?>, Boolean> handlerSharableCache() {
//      Map<Class<?>, Boolean> cache = handlerSharableCache;
//      if (cache == null) {
//         // Start with small capacity to keep memory overhead as low as possible.
//         handlerSharableCache = cache = new WeakHashMap<>(4);
//      }
//      return cache;
//   }
//
//   public int localChannelReaderStackDepth() {
//      return localChannelReaderStackDepth;
//   }
//
//   public void setLocalChannelReaderStackDepth(int localChannelReaderStackDepth) {
//      this.localChannelReaderStackDepth = localChannelReaderStackDepth;
//   }
//
//   public Object indexedVariable(int index) {
//      Object[] lookup = indexedVariables;
//      return index < lookup.length? lookup[index] : UNSET;
//   }
//
//   /**
//    * @return {@code true} if and only if a new thread-local variable has been created
//    */
//   public boolean setIndexedVariable(int index, Object value) {
//      Object[] lookup = indexedVariables;
//      if (index < lookup.length) {
//         Object oldValue = lookup[index];
//         lookup[index] = value;
//         return oldValue == UNSET;
//      } else {
//         expandIndexedVariableTableAndSet(index, value);
//         return true;
//      }
//   }
//
//   private void expandIndexedVariableTableAndSet(int index, Object value) {
//      Object[] oldArray = indexedVariables;
//      final int oldCapacity = oldArray.length;
//      int newCapacity = index;
//      newCapacity |= newCapacity >>>  1;
//      newCapacity |= newCapacity >>>  2;
//      newCapacity |= newCapacity >>>  4;
//      newCapacity |= newCapacity >>>  8;
//      newCapacity |= newCapacity >>> 16;
//      newCapacity ++;
//
//      Object[] newArray = Arrays.copyOf(oldArray, newCapacity);
//      Arrays.fill(newArray, oldCapacity, newArray.length, UNSET);
//      newArray[index] = value;
//      indexedVariables = newArray;
//   }
//
//   public Object removeIndexedVariable(int index) {
//      Object[] lookup = indexedVariables;
//      if (index < lookup.length) {
//         Object v = lookup[index];
//         lookup[index] = UNSET;
//         return v;
//      } else {
//         return UNSET;
//      }
//   }
//
//   public boolean isIndexedVariableSet(int index) {
//      Object[] lookup = indexedVariables;
//      return index < lookup.length && lookup[index] != UNSET;
//   }
//
//   public boolean isCleanerFlagSet(int index) {
//      return cleanerFlags != null && cleanerFlags.get(index);
//   }
//
//   public void setCleanerFlag(int index) {
//      if (cleanerFlags == null) {
//         cleanerFlags = new BitSet();
//      }
//      cleanerFlags.set(index);
//   }
//}