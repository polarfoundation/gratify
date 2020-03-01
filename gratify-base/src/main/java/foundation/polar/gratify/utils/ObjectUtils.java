package foundation.polar.gratify.utils;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Array;
import java.util.*;

public abstract class ObjectUtils {
   private static final int INITIAL_HASH = 7;
   private static final int MULTIPLIER = 31;
   private static final String EMPTY_STRING = "";
   private static final String NULL_STRING = "null";
   private static final String ARRAY_START = "{";
   private static final String ARRAY_END = "}";
   private static final String EMPTY_ARRAY = ARRAY_START + ARRAY_END;
   private static final String ARRAY_ELEMENT_SEPARATOR = ", ";
   private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

   public static boolean isCheckedException(Throwable ex) {
      return !(ex instanceof RuntimeException || ex instanceof Error);
   }

   public static boolean isCompatibleWithThrowsClause(Throwable ex, @Nullable Class<?>... declaredExceptions) {
      if (!isCheckedException(ex)) {
         return true;
      }
      if (declaredExceptions != null) {
         for (Class<?> declaredException : declaredExceptions) {
            if (declaredException != null && declaredException.isInstance(ex)) {
               return true;
            }
         }
      }
      return false;
   }

   public static boolean isArray(@Nullable Object obj) {
      return (obj != null && obj.getClass().isArray());
   }

   public static boolean isEmpty(@Nullable Object[] array) {
      return (array == null || array.length == 0);
   }

   @SuppressWarnings("rawtypes")
   public static boolean isEmpty(@Nullable Object obj) {
      if (obj == null) {
         return true;
      }

      if (obj instanceof Optional) {
         return !((Optional) obj).isPresent();
      }
      if (obj instanceof CharSequence) {
         return ((CharSequence) obj).length() == 0;
      }
      if (obj.getClass().isArray()) {
         return Array.getLength(obj) == 0;
      }
      if (obj instanceof Collection) {
         return ((Collection) obj).isEmpty();
      }
      if (obj instanceof Map) {
         return ((Map) obj).isEmpty();
      }

      // else
      return false;
   }

   @Nullable
   public static Object unwrapOptional(@Nullable Object obj) {
      if (obj instanceof Optional) {
         Optional<?> optional = (Optional<?>) obj;
         if (!optional.isPresent()) {
            return null;
         }
         Object result = optional.get();
         AssertUtils.isTrue(!(result instanceof Optional), "Multi-level Optional usage not supported");
         return result;
      }
      return obj;
   }

   public static boolean containsElement(@Nullable Object[] array, Object element) {
      if (array == null) {
         return false;
      }
      for (Object arrayEle : array) {
         if (nullSafeEquals(arrayEle, element)) {
            return true;
         }
      }
      return false;
   }

   public static boolean containsConstant(Enum<?>[] enumValues, String constant) {
      return containsConstant(enumValues, constant, false);
   }

   public static boolean containsConstant(Enum<?>[] enumValues, String constant, boolean caseSensitive) {
      for (Enum<?> candidate : enumValues) {
         if (caseSensitive ? candidate.toString().equals(constant) :
            candidate.toString().equalsIgnoreCase(constant)) {
            return true;
         }
      }
      return false;
   }

   public static <E extends Enum<?>> E caseInsensitiveValueOf(E [] enumValues, String constant) {
      for (E candidate : enumValues) {
         if (candidate.toString().equalsIgnoreCase(constant)) {
            return candidate;
         }
      }
      throw new IllegalArgumentException("Constant [" + constant + "] does not exist in enum type " +
         enumValues.getClass().getComponentType().getName());
   }

   public static <A, O extends A> A[] addObjectToArray(@Nullable A[] array, @Nullable O obj) {
      Class<?> compType = Object.class;
      if (array != null) {
         compType = array.getClass().getComponentType();
      }
      else if (obj != null) {
         compType = obj.getClass();
      }
      int newArrLength = (array != null ? array.length + 1 : 1);
      @SuppressWarnings("unchecked")
      A[] newArr = (A[]) Array.newInstance(compType, newArrLength);
      if (array != null) {
         System.arraycopy(array, 0, newArr, 0, array.length);
      }
      newArr[newArr.length - 1] = obj;
      return newArr;
   }

   public static Object[] toObjectArray(@Nullable Object source) {
      if (source instanceof Object[]) {
         return (Object[]) source;
      }
      if (source == null) {
         return EMPTY_OBJECT_ARRAY;
      }
      if (!source.getClass().isArray()) {
         throw new IllegalArgumentException("Source is not an array: " + source);
      }
      int length = Array.getLength(source);
      if (length == 0) {
         return EMPTY_OBJECT_ARRAY;
      }
      Class<?> wrapperType = Array.get(source, 0).getClass();
      Object[] newArray = (Object[]) Array.newInstance(wrapperType, length);
      for (int i = 0; i < length; i++) {
         newArray[i] = Array.get(source, i);
      }
      return newArray;
   }

   public static boolean nullSafeEquals(@Nullable Object o1, @Nullable Object o2) {
      if (o1 == o2) {
         return true;
      }
      if (o1 == null || o2 == null) {
         return false;
      }
      if (o1.equals(o2)) {
         return true;
      }
      if (o1.getClass().isArray() && o2.getClass().isArray()) {
         return arrayEquals(o1, o2);
      }
      return false;
   }

   private static boolean arrayEquals(Object o1, Object o2) {
      if (o1 instanceof Object[] && o2 instanceof Object[]) {
         return Arrays.equals((Object[]) o1, (Object[]) o2);
      }
      if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
         return Arrays.equals((boolean[]) o1, (boolean[]) o2);
      }
      if (o1 instanceof byte[] && o2 instanceof byte[]) {
         return Arrays.equals((byte[]) o1, (byte[]) o2);
      }
      if (o1 instanceof char[] && o2 instanceof char[]) {
         return Arrays.equals((char[]) o1, (char[]) o2);
      }
      if (o1 instanceof double[] && o2 instanceof double[]) {
         return Arrays.equals((double[]) o1, (double[]) o2);
      }
      if (o1 instanceof float[] && o2 instanceof float[]) {
         return Arrays.equals((float[]) o1, (float[]) o2);
      }
      if (o1 instanceof int[] && o2 instanceof int[]) {
         return Arrays.equals((int[]) o1, (int[]) o2);
      }
      if (o1 instanceof long[] && o2 instanceof long[]) {
         return Arrays.equals((long[]) o1, (long[]) o2);
      }
      if (o1 instanceof short[] && o2 instanceof short[]) {
         return Arrays.equals((short[]) o1, (short[]) o2);
      }
      return false;
   }

   public static int nullSafeHashCode(@Nullable Object obj) {
      if (obj == null) {
         return 0;
      }
      if (obj.getClass().isArray()) {
         if (obj instanceof Object[]) {
            return nullSafeHashCode((Object[]) obj);
         }
         if (obj instanceof boolean[]) {
            return nullSafeHashCode((boolean[]) obj);
         }
         if (obj instanceof byte[]) {
            return nullSafeHashCode((byte[]) obj);
         }
         if (obj instanceof char[]) {
            return nullSafeHashCode((char[]) obj);
         }
         if (obj instanceof double[]) {
            return nullSafeHashCode((double[]) obj);
         }
         if (obj instanceof float[]) {
            return nullSafeHashCode((float[]) obj);
         }
         if (obj instanceof int[]) {
            return nullSafeHashCode((int[]) obj);
         }
         if (obj instanceof long[]) {
            return nullSafeHashCode((long[]) obj);
         }
         if (obj instanceof short[]) {
            return nullSafeHashCode((short[]) obj);
         }
      }
      return obj.hashCode();
   }

   public static int nullSafeHashCode(@Nullable Object[] array) {
      if (array == null) {
         return 0;
      }
      int hash = INITIAL_HASH;
      for (Object element : array) {
         hash = MULTIPLIER * hash + nullSafeHashCode(element);
      }
      return hash;
   }

   public static int nullSafeHashCode(@Nullable boolean[] array) {
      if (array == null) {
         return 0;
      }
      int hash = INITIAL_HASH;
      for (boolean element : array) {
         hash = MULTIPLIER * hash + Boolean.hashCode(element);
      }
      return hash;
   }

   public static int nullSafeHashCode(@Nullable byte[] array) {
      if (array == null) {
         return 0;
      }
      int hash = INITIAL_HASH;
      for (byte element : array) {
         hash = MULTIPLIER * hash + element;
      }
      return hash;
   }

   public static int nullSafeHashCode(@Nullable char[] array) {
      if (array == null) {
         return 0;
      }
      int hash = INITIAL_HASH;
      for (char element : array) {
         hash = MULTIPLIER * hash + element;
      }
      return hash;
   }

   public static int nullSafeHashCode(@Nullable double[] array) {
      if (array == null) {
         return 0;
      }
      int hash = INITIAL_HASH;
      for (double element : array) {
         hash = MULTIPLIER * hash + Double.hashCode(element);
      }
      return hash;
   }

   public static int nullSafeHashCode(float @Nullable[] array) {
      if (array == null) {
         return 0;
      }
      int hash = INITIAL_HASH;
      for (float element : array) {
         hash = MULTIPLIER * hash + Float.hashCode(element);
      }
      return hash;
   }

   public static int nullSafeHashCode(@Nullable int[] array) {
      if (array == null) {
         return 0;
      }
      int hash = INITIAL_HASH;
      for (int element : array) {
         hash = MULTIPLIER * hash + element;
      }
      return hash;
   }

   public static int nullSafeHashCode(@Nullable long[] array) {
      if (array == null) {
         return 0;
      }
      int hash = INITIAL_HASH;
      for (long element : array) {
         hash = MULTIPLIER * hash + Long.hashCode(element);
      }
      return hash;
   }

   public static int nullSafeHashCode(@Nullable short[] array) {
      if (array == null) {
         return 0;
      }
      int hash = INITIAL_HASH;
      for (short element : array) {
         hash = MULTIPLIER * hash + element;
      }
      return hash;
   }

   public static String identityToString(@Nullable Object obj) {
      if (obj == null) {
         return EMPTY_STRING;
      }
      String className = obj.getClass().getName();
      String identityHexString = getIdentityHexString(obj);
      return className + '@' + identityHexString;
   }

   public static String getIdentityHexString(Object obj) {
      return Integer.toHexString(System.identityHashCode(obj));
   }

   public static String getDisplayString(@Nullable Object obj) {
      if (obj == null) {
         return EMPTY_STRING;
      }
      return nullSafeToString(obj);
   }

   public static String nullSafeClassName(@Nullable Object obj) {
      return (obj != null ? obj.getClass().getName() : NULL_STRING);
   }

   public static String nullSafeToString(@Nullable Object obj) {
      if (obj == null) {
         return NULL_STRING;
      }
      if (obj instanceof String) {
         return (String) obj;
      }
      if (obj instanceof Object[]) {
         return nullSafeToString((Object[]) obj);
      }
      if (obj instanceof boolean[]) {
         return nullSafeToString((boolean[]) obj);
      }
      if (obj instanceof byte[]) {
         return nullSafeToString((byte[]) obj);
      }
      if (obj instanceof char[]) {
         return nullSafeToString((char[]) obj);
      }
      if (obj instanceof double[]) {
         return nullSafeToString((double[]) obj);
      }
      if (obj instanceof float[]) {
         return nullSafeToString((float[]) obj);
      }
      if (obj instanceof int[]) {
         return nullSafeToString((int[]) obj);
      }
      if (obj instanceof long[]) {
         return nullSafeToString((long[]) obj);
      }
      if (obj instanceof short[]) {
         return nullSafeToString((short[]) obj);
      }
      String str = obj.toString();
      return (str != null ? str : EMPTY_STRING);
   }

   public static String nullSafeToString(@Nullable Object[] array) {
      if (array == null) {
         return NULL_STRING;
      }
      int length = array.length;
      if (length == 0) {
         return EMPTY_ARRAY;
      }
      StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
      for (Object o : array) {
         stringJoiner.add(String.valueOf(o));
      }
      return stringJoiner.toString();
   }

   public static String nullSafeToString(@Nullable boolean[] array) {
      if (array == null) {
         return NULL_STRING;
      }
      int length = array.length;
      if (length == 0) {
         return EMPTY_ARRAY;
      }
      StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
      for (boolean b : array) {
         stringJoiner.add(String.valueOf(b));
      }
      return stringJoiner.toString();
   }

   public static String nullSafeToString(@Nullable byte[] array) {
      if (array == null) {
         return NULL_STRING;
      }
      int length = array.length;
      if (length == 0) {
         return EMPTY_ARRAY;
      }
      StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
      for (byte b : array) {
         stringJoiner.add(String.valueOf(b));
      }
      return stringJoiner.toString();
   }

   public static String nullSafeToString(@Nullable char[] array) {
      if (array == null) {
         return NULL_STRING;
      }
      int length = array.length;
      if (length == 0) {
         return EMPTY_ARRAY;
      }
      StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
      for (char c : array) {
         stringJoiner.add('\'' + String.valueOf(c) + '\'');
      }
      return stringJoiner.toString();
   }

   public static String nullSafeToString(@Nullable double[] array) {
      if (array == null) {
         return NULL_STRING;
      }
      int length = array.length;
      if (length == 0) {
         return EMPTY_ARRAY;
      }
      StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
      for (double d : array) {
         stringJoiner.add(String.valueOf(d));
      }
      return stringJoiner.toString();
   }

   public static String nullSafeToString(@Nullable float[] array) {
      if (array == null) {
         return NULL_STRING;
      }
      int length = array.length;
      if (length == 0) {
         return EMPTY_ARRAY;
      }
      StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
      for (float f : array) {
         stringJoiner.add(String.valueOf(f));
      }
      return stringJoiner.toString();
   }

   public static String nullSafeToString(@Nullable int[] array) {
      if (array == null) {
         return NULL_STRING;
      }
      int length = array.length;
      if (length == 0) {
         return EMPTY_ARRAY;
      }
      StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
      for (int i : array) {
         stringJoiner.add(String.valueOf(i));
      }
      return stringJoiner.toString();
   }

   public static String nullSafeToString(@Nullable long[] array) {
      if (array == null) {
         return NULL_STRING;
      }
      int length = array.length;
      if (length == 0) {
         return EMPTY_ARRAY;
      }
      StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
      for (long l : array) {
         stringJoiner.add(String.valueOf(l));
      }
      return stringJoiner.toString();
   }

   public static String nullSafeToString(@Nullable short[] array) {
      if (array == null) {
         return NULL_STRING;
      }
      int length = array.length;
      if (length == 0) {
         return EMPTY_ARRAY;
      }
      StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
      for (short s : array) {
         stringJoiner.add(String.valueOf(s));
      }
      return stringJoiner.toString();
   }
}
