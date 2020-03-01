//package foundation.polar.gratify.ds;
//
//import org.checkerframework.checker.nullness.qual.Nullable;
//
//import java.lang.ref.SoftReference;
//import java.lang.ref.WeakReference;
//import java.lang.reflect.Array;
//import java.util.AbstractMap;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ConcurrentMap;
//
//public class ConcurrentReferenceHashMap<K, V> extends AbstractMap<K, V>
//   implements ConcurrentMap<K, V> {
//   public enum ReferenceType {
//      SOFT,
//      WEAK
//   }
//
//   private static final int DEFAULT_INITIAL_CAPACITY = 16;
//   private static final float DEFAULT_LOAD_FACTOR = 0.75f;
//   private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
//   private static final ReferenceType DEFAULT_REFERENCE_TYPE = ReferenceType.SOFT;
//   private static final int MAXIMUM_CONCURRENCY_LEVEL = 1 << 16;
//   private static final int MAXIMUM_SEGMENT_SIZE = 1 << 30;
//   private final Segment[] segments;
//   private final float loadFactor;
//   private final ReferenceType referenceType;
//   private final int shift;
//
//   @Nullable
//   private volatile Set<Entry<K, V>> entrySet;
//
//   public ConcurrentReferenceHashMap() {
//      this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
//   }
//
//   public ConcurrentReferenceHashMap(int initialCapacity) {
//      this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
//   }
//
//   public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor) {
//      this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
//   }
//
//   public ConcurrentReferenceHashMap(int initialCapacity, int concurrencyLevel) {
//      this(initialCapacity, DEFAULT_LOAD_FACTOR, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
//   }
//
//   public ConcurrentReferenceHashMap(int initialCapacity, ReferenceType referenceType) {
//      this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, referenceType);
//   }
//
//   public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
//      this(initialCapacity, loadFactor, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
//   }
//
//   @SuppressWarnings("unchecked")
//   public ConcurrentReferenceHashMap(
//      int initialCapacity, float loadFactor, int concurrencyLevel, ReferenceType referenceType) {
//
//      Assert.isTrue(initialCapacity >= 0, "Initial capacity must not be negative");
//      Assert.isTrue(loadFactor > 0f, "Load factor must be positive");
//      Assert.isTrue(concurrencyLevel > 0, "Concurrency level must be positive");
//      Assert.notNull(referenceType, "Reference type must not be null");
//      this.loadFactor = loadFactor;
//      this.shift = calculateShift(concurrencyLevel, MAXIMUM_CONCURRENCY_LEVEL);
//      int size = 1 << this.shift;
//      this.referenceType = referenceType;
//      int roundedUpSegmentCapacity = (int) ((initialCapacity + size - 1L) / size);
//      int initialSize = 1 << calculateShift(roundedUpSegmentCapacity, MAXIMUM_SEGMENT_SIZE);
//      Segment[] segments = (Segment[]) Array.newInstance(Segment.class, size);
//      int resizeThreshold = (int) (initialSize * getLoadFactor());
//      for (int i = 0; i < segments.length; i++) {
//         segments[i] = new Segment(initialSize, resizeThreshold);
//      }
//      this.segments = segments;
//   }
//}
