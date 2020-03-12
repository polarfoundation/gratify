package foundation.polar.gratify.artifacts;

import foundation.polar.gratify.core.MethodParameter;
import foundation.polar.gratify.ds.ConcurrentReferenceHashMap;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ClassUtils;
import foundation.polar.gratify.utils.ReflectionUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.*;
/**
 * Static convenience methods for JavaArtifacts: for instantiating beans,
 * checking bean property types, copying bean properties, etc.
 *
 * <p>Mainly for internal use within the framework, but to some degree also
 * useful for application classes. Consider
 * <a href="https://commons.apache.org/proper/commons-beanutils/">Apache Commons ArtifactUtils</a>,
 * <a href="https://hotelsdotcom.github.io/bull/">BULL - Artifact Utils Light Library</a>,
 * or similar third-party frameworks for more comprehensive bean utilities.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Sebastien Deleuze
 */
public class ArtifactUtils {

   private static final Log logger = LogFactory.getLog(ArtifactUtils.class);

   private static final Set<Class<?>> unknownEditorTypes =
      Collections.newSetFromMap(new ConcurrentReferenceHashMap<>(64));

   private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES;

   static {
      Map<Class<?>, Object> values = new HashMap<>();
      values.put(boolean.class, false);
      values.put(byte.class, (byte) 0);
      values.put(short.class, (short) 0);
      values.put(int.class, 0);
      values.put(long.class, (long) 0);
      DEFAULT_TYPE_VALUES = Collections.unmodifiableMap(values);
   }

   /**
    * Instantiate a class using its 'primary' constructor (for Kotlin classes,
    * potentially having default arguments declared) or its default constructor
    * (for regular Java classes, expecting a standard no-arg setup).
    * <p>Note that this method tries to set the constructor accessible
    * if given a non-accessible (that is, non-public) constructor.
    * @param clazz the class to instantiate
    * @return the new instance
    * @throws ArtifactInstantiationException if the bean cannot be instantiated.
    * The cause may notably indicate a {@link NoSuchMethodException} if no
    * primary/default constructor was found, a {@link NoClassDefFoundError}
    * or other {@link LinkageError} in case of an unresolvable class definition
    * (e.g. due to a missing dependency at runtime), or an exception thrown
    * from the constructor invocation itself.
    * @see Constructor#newInstance
    */
   public static <T> T instantiateClass(Class<T> clazz) throws ArtifactInstantiationException {
      AssertUtils.notNull(clazz, "Class must not be null");
      if (clazz.isInterface()) {
         throw new ArtifactInstantiationException(clazz, "Specified class is an interface");
      }
      try {
         return instantiateClass(clazz.getDeclaredConstructor());
      }
      catch (NoSuchMethodException ex) {
         throw new ArtifactInstantiationException(clazz, "No default constructor found", ex);
      }
      catch (LinkageError err) {
         throw new ArtifactInstantiationException(clazz, "Unresolvable class definition", err);
      }
   }

   /**
    * Instantiate a class using its no-arg constructor and return the new instance
    * as the specified assignable type.
    * <p>Useful in cases where the type of the class to instantiate (clazz) is not
    * available, but the type desired (assignableTo) is known.
    * <p>Note that this method tries to set the constructor accessible if given a
    * non-accessible (that is, non-public) constructor.
    * @param clazz class to instantiate
    * @param assignableTo type that clazz must be assignableTo
    * @return the new instance
    * @throws ArtifactInstantiationException if the bean cannot be instantiated
    * @see Constructor#newInstance
    */
   @SuppressWarnings("unchecked")
   public static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo) throws ArtifactInstantiationException {
      AssertUtils.isAssignable(assignableTo, clazz);
      return (T) instantiateClass(clazz);
   }

   /**
    * Convenience method to instantiate a class using the given constructor.
    * <p>Note that this method tries to set the constructor accessible if given a
    * non-accessible (that is, non-public) constructor, and supports Kotlin classes
    * with optional parameters and default values.
    * @param ctor the constructor to instantiate
    * @param args the constructor arguments to apply (use {@code null} for an unspecified
    * parameter, Kotlin optional parameters and Java primitive types are supported)
    * @return the new instance
    * @throws ArtifactInstantiationException if the bean cannot be instantiated
    * @see Constructor#newInstance
    */
   public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws ArtifactInstantiationException {
      AssertUtils.notNull(ctor, "Constructor must not be null");
      try {
         ReflectionUtils.makeAccessible(ctor);
         Class<?>[] parameterTypes = ctor.getParameterTypes();
         AssertUtils.isTrue(args.length <= parameterTypes.length, "Can't specify more arguments than constructor parameters");
         Object[] argsWithDefaultValues = new Object[args.length];
         for (int i = 0 ; i < args.length; i++) {
            if (args[i] == null) {
               Class<?> parameterType = parameterTypes[i];
               argsWithDefaultValues[i] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
            }
            else {
               argsWithDefaultValues[i] = args[i];
            }
         }
         return ctor.newInstance(argsWithDefaultValues);
      }
      catch (InstantiationException ex) {
         throw new ArtifactInstantiationException(ctor, "Is it an abstract class?", ex);
      }
      catch (IllegalAccessException ex) {
         throw new ArtifactInstantiationException(ctor, "Is the constructor accessible?", ex);
      }
      catch (IllegalArgumentException ex) {
         throw new ArtifactInstantiationException(ctor, "Illegal arguments for constructor", ex);
      }
      catch (InvocationTargetException ex) {
         throw new ArtifactInstantiationException(ctor, "Constructor threw exception", ex.getTargetException());
      }
   }

   /**
    * Find a method with the given method name and the given parameter types,
    * declared on the given class or one of its superclasses. Prefers public methods,
    * but will return a protected, package access, or private method too.
    * <p>Checks {@code Class.getMethod} first, falling back to
    * {@code findDeclaredMethod}. This allows to find public methods
    * without issues even in environments with restricted Java security settings.
    * @param clazz the class to check
    * @param methodName the name of the method to find
    * @param paramTypes the parameter types of the method to find
    * @return the Method object, or {@code null} if not found
    * @see Class#getMethod
    * @see #findDeclaredMethod
    */
   @Nullable
   public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
      try {
         return clazz.getMethod(methodName, paramTypes);
      }
      catch (NoSuchMethodException ex) {
         return findDeclaredMethod(clazz, methodName, paramTypes);
      }
   }

   /**
    * Find a method with the given method name and the given parameter types,
    * declared on the given class or one of its superclasses. Will return a public,
    * protected, package access, or private method.
    * <p>Checks {@code Class.getDeclaredMethod}, cascading upwards to all superclasses.
    * @param clazz the class to check
    * @param methodName the name of the method to find
    * @param paramTypes the parameter types of the method to find
    * @return the Method object, or {@code null} if not found
    * @see Class#getDeclaredMethod
    */
   @Nullable
   public static Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
      try {
         return clazz.getDeclaredMethod(methodName, paramTypes);
      }
      catch (NoSuchMethodException ex) {
         if (clazz.getSuperclass() != null) {
            return findDeclaredMethod(clazz.getSuperclass(), methodName, paramTypes);
         }
         return null;
      }
   }

   /**
    * Find a method with the given method name and minimal parameters (best case: none),
    * declared on the given class or one of its superclasses. Prefers public methods,
    * but will return a protected, package access, or private method too.
    * <p>Checks {@code Class.getMethods} first, falling back to
    * {@code findDeclaredMethodWithMinimalParameters}. This allows for finding public
    * methods without issues even in environments with restricted Java security settings.
    * @param clazz the class to check
    * @param methodName the name of the method to find
    * @return the Method object, or {@code null} if not found
    * @throws IllegalArgumentException if methods of the given name were found but
    * could not be resolved to a unique method with minimal parameters
    * @see Class#getMethods
    * @see #findDeclaredMethodWithMinimalParameters
    */
   @Nullable
   public static Method findMethodWithMinimalParameters(Class<?> clazz, String methodName)
      throws IllegalArgumentException {

      Method targetMethod = findMethodWithMinimalParameters(clazz.getMethods(), methodName);
      if (targetMethod == null) {
         targetMethod = findDeclaredMethodWithMinimalParameters(clazz, methodName);
      }
      return targetMethod;
   }

   /**
    * Find a method with the given method name and minimal parameters (best case: none),
    * declared on the given class or one of its superclasses. Will return a public,
    * protected, package access, or private method.
    * <p>Checks {@code Class.getDeclaredMethods}, cascading upwards to all superclasses.
    * @param clazz the class to check
    * @param methodName the name of the method to find
    * @return the Method object, or {@code null} if not found
    * @throws IllegalArgumentException if methods of the given name were found but
    * could not be resolved to a unique method with minimal parameters
    * @see Class#getDeclaredMethods
    */
   @Nullable
   public static Method findDeclaredMethodWithMinimalParameters(Class<?> clazz, String methodName)
      throws IllegalArgumentException {

      Method targetMethod = findMethodWithMinimalParameters(clazz.getDeclaredMethods(), methodName);
      if (targetMethod == null && clazz.getSuperclass() != null) {
         targetMethod = findDeclaredMethodWithMinimalParameters(clazz.getSuperclass(), methodName);
      }
      return targetMethod;
   }

   /**
    * Find a method with the given method name and minimal parameters (best case: none)
    * in the given list of methods.
    * @param methods the methods to check
    * @param methodName the name of the method to find
    * @return the Method object, or {@code null} if not found
    * @throws IllegalArgumentException if methods of the given name were found but
    * could not be resolved to a unique method with minimal parameters
    */
   @Nullable
   public static Method findMethodWithMinimalParameters(Method[] methods, String methodName)
      throws IllegalArgumentException {

      Method targetMethod = null;
      int numMethodsFoundWithCurrentMinimumArgs = 0;
      for (Method method : methods) {
         if (method.getName().equals(methodName)) {
            int numParams = method.getParameterCount();
            if (targetMethod == null || numParams < targetMethod.getParameterCount()) {
               targetMethod = method;
               numMethodsFoundWithCurrentMinimumArgs = 1;
            }
            else if (!method.isBridge() && targetMethod.getParameterCount() == numParams) {
               if (targetMethod.isBridge()) {
                  // Prefer regular method over bridge...
                  targetMethod = method;
               }
               else {
                  // Additional candidate with same length
                  numMethodsFoundWithCurrentMinimumArgs++;
               }
            }
         }
      }
      if (numMethodsFoundWithCurrentMinimumArgs > 1) {
         throw new IllegalArgumentException("Cannot resolve method '" + methodName +
            "' to a unique method. Attempted to resolve to overloaded method with " +
            "the least number of parameters but there were " +
            numMethodsFoundWithCurrentMinimumArgs + " candidates.");
      }
      return targetMethod;
   }

   /**
    * Parse a method signature in the form {@code methodName[([arg_list])]},
    * where {@code arg_list} is an optional, comma-separated list of fully-qualified
    * type names, and attempts to resolve that signature against the supplied {@code Class}.
    * <p>When not supplying an argument list ({@code methodName}) the method whose name
    * matches and has the least number of parameters will be returned. When supplying an
    * argument type list, only the method whose name and argument types match will be returned.
    * <p>Note then that {@code methodName} and {@code methodName()} are <strong>not</strong>
    * resolved in the same way. The signature {@code methodName} means the method called
    * {@code methodName} with the least number of arguments, whereas {@code methodName()}
    * means the method called {@code methodName} with exactly 0 arguments.
    * <p>If no method can be found, then {@code null} is returned.
    * @param signature the method signature as String representation
    * @param clazz the class to resolve the method signature against
    * @return the resolved Method
    * @see #findMethod
    * @see #findMethodWithMinimalParameters
    */
   @Nullable
   public static Method resolveSignature(String signature, Class<?> clazz) {
      AssertUtils.hasText(signature, "'signature' must not be empty");
      AssertUtils.notNull(clazz, "Class must not be null");
      int startParen = signature.indexOf('(');
      int endParen = signature.indexOf(')');
      if (startParen > -1 && endParen == -1) {
         throw new IllegalArgumentException("Invalid method signature '" + signature +
            "': expected closing ')' for args list");
      }
      else if (startParen == -1 && endParen > -1) {
         throw new IllegalArgumentException("Invalid method signature '" + signature +
            "': expected opening '(' for args list");
      }
      else if (startParen == -1) {
         return findMethodWithMinimalParameters(clazz, signature);
      }
      else {
         String methodName = signature.substring(0, startParen);
         String[] parameterTypeNames =
            StringUtils.commaDelimitedListToStringArray(signature.substring(startParen + 1, endParen));
         Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
         for (int i = 0; i < parameterTypeNames.length; i++) {
            String parameterTypeName = parameterTypeNames[i].trim();
            try {
               parameterTypes[i] = ClassUtils.forName(parameterTypeName, clazz.getClassLoader());
            }
            catch (Throwable ex) {
               throw new IllegalArgumentException("Invalid method signature: unable to resolve type [" +
                  parameterTypeName + "] for argument " + i + ". Root cause: " + ex);
            }
         }
         return findMethod(clazz, methodName, parameterTypes);
      }
   }


   /**
    * Retrieve the JavaArtifacts {@code PropertyDescriptor}s of a given class.
    * @param clazz the Class to retrieve the PropertyDescriptors for
    * @return an array of {@code PropertyDescriptors} for the given class
    * @throws ArtifactsException if PropertyDescriptor look fails
    */
   public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) throws ArtifactsException {
      CachedIntrospectionResults cr = CachedIntrospectionResults.forClass(clazz);
      return cr.getPropertyDescriptors();
   }

   /**
    * Retrieve the JavaArtifacts {@code PropertyDescriptors} for the given property.
    * @param clazz the Class to retrieve the PropertyDescriptor for
    * @param propertyName the name of the property
    * @return the corresponding PropertyDescriptor, or {@code null} if none
    * @throws ArtifactsException if PropertyDescriptor lookup fails
    */
   @Nullable
   public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName)
      throws ArtifactsException {

      CachedIntrospectionResults cr = CachedIntrospectionResults.forClass(clazz);
      return cr.getPropertyDescriptor(propertyName);
   }

   /**
    * Find a JavaArtifacts {@code PropertyDescriptor} for the given method,
    * with the method either being the read method or the write method for
    * that bean property.
    * @param method the method to find a corresponding PropertyDescriptor for,
    * introspecting its declaring class
    * @return the corresponding PropertyDescriptor, or {@code null} if none
    * @throws ArtifactsException if PropertyDescriptor lookup fails
    */
   @Nullable
   public static PropertyDescriptor findPropertyForMethod(Method method) throws ArtifactsException {
      return findPropertyForMethod(method, method.getDeclaringClass());
   }

   /**
    * Find a JavaArtifacts {@code PropertyDescriptor} for the given method,
    * with the method either being the read method or the write method for
    * that bean property.
    * @param method the method to find a corresponding PropertyDescriptor for
    * @param clazz the (most specific) class to introspect for descriptors
    * @return the corresponding PropertyDescriptor, or {@code null} if none
    * @throws ArtifactsException if PropertyDescriptor lookup fails
    */
   @Nullable
   public static PropertyDescriptor findPropertyForMethod(Method method, Class<?> clazz) throws ArtifactsException {
      AssertUtils.notNull(method, "Method must not be null");
      PropertyDescriptor[] pds = getPropertyDescriptors(clazz);
      for (PropertyDescriptor pd : pds) {
         if (method.equals(pd.getReadMethod()) || method.equals(pd.getWriteMethod())) {
            return pd;
         }
      }
      return null;
   }

   /**
    * Find a JavaArtifacts PropertyEditor following the 'Editor' suffix convention
    * (e.g. "mypackage.MyDomainClass" -> "mypackage.MyDomainClassEditor").
    * <p>Compatible to the standard JavaArtifacts convention as implemented by
    * {@link java.beans.PropertyEditorManager} but isolated from the latter's
    * registered default editors for primitive types.
    * @param targetType the type to find an editor for
    * @return the corresponding editor, or {@code null} if none found
    */
   @Nullable
   public static PropertyEditor findEditorByConvention(@Nullable Class<?> targetType) {
      if (targetType == null || targetType.isArray() || unknownEditorTypes.contains(targetType)) {
         return null;
      }
      ClassLoader cl = targetType.getClassLoader();
      if (cl == null) {
         try {
            cl = ClassLoader.getSystemClassLoader();
            if (cl == null) {
               return null;
            }
         }
         catch (Throwable ex) {
            // e.g. AccessControlException on Google App Engine
            if (logger.isDebugEnabled()) {
               logger.debug("Could not access system ClassLoader: " + ex);
            }
            return null;
         }
      }
      String targetTypeName = targetType.getName();
      String editorName = targetTypeName + "Editor";
      try {
         Class<?> editorClass = cl.loadClass(editorName);
         if (!PropertyEditor.class.isAssignableFrom(editorClass)) {
            if (logger.isInfoEnabled()) {
               logger.info("Editor class [" + editorName +
                  "] does not implement [java.beans.PropertyEditor] interface");
            }
            unknownEditorTypes.add(targetType);
            return null;
         }
         return (PropertyEditor) instantiateClass(editorClass);
      }
      catch (ClassNotFoundException ex) {
         if (logger.isTraceEnabled()) {
            logger.trace("No property editor [" + editorName + "] found for type " +
               targetTypeName + " according to 'Editor' suffix convention");
         }
         unknownEditorTypes.add(targetType);
         return null;
      }
   }

   /**
    * Determine the bean property type for the given property from the
    * given classes/interfaces, if possible.
    * @param propertyName the name of the bean property
    * @param beanClasses the classes to check against
    * @return the property type, or {@code Object.class} as fallback
    */
   public static Class<?> findPropertyType(String propertyName, @Nullable Class<?>... beanClasses) {
      if (beanClasses != null) {
         for (Class<?> beanClass : beanClasses) {
            PropertyDescriptor pd = getPropertyDescriptor(beanClass, propertyName);
            if (pd != null) {
               return pd.getPropertyType();
            }
         }
      }
      return Object.class;
   }

   /**
    * Obtain a new MethodParameter object for the write method of the
    * specified property.
    * @param pd the PropertyDescriptor for the property
    * @return a corresponding MethodParameter object
    */
   public static MethodParameter getWriteMethodParameter(PropertyDescriptor pd) {
      if (pd instanceof GenericTypeAwarePropertyDescriptor) {
         return new MethodParameter(((GenericTypeAwarePropertyDescriptor) pd).getWriteMethodParameter());
      }
      else {
         Method writeMethod = pd.getWriteMethod();
         AssertUtils.state(writeMethod != null, "No write method available");
         return new MethodParameter(writeMethod, 0);
      }
   }

   /**
    * Check if the given type represents a "simple" property: a simple value
    * type or an array of simple value types.
    * <p>See {@link #isSimpleValueType(Class)} for the definition of <em>simple
    * value type</em>.
    * <p>Used to determine properties to check for a "simple" dependency-check.
    * @param type the type to check
    * @return whether the given type represents a "simple" property
    * @see foundation.polar.gratify.artifacts.factory.support.RootArtifactDefinition#DEPENDENCY_CHECK_SIMPLE
    * @see foundation.polar.gratify.artifacts.factory.support.AbstractAutowireCapableArtifactFactory#checkDependencies
    * @see #isSimpleValueType(Class)
    */
   public static boolean isSimpleProperty(Class<?> type) {
      AssertUtils.notNull(type, "'type' must not be null");
      return isSimpleValueType(type) || (type.isArray() && isSimpleValueType(type.getComponentType()));
   }

   /**
    * Check if the given type represents a "simple" value type: a primitive or
    * primitive wrapper, an enum, a String or other CharSequence, a Number, a
    * Date, a Temporal, a URI, a URL, a Locale, or a Class.
    * <p>{@code Void} and {@code void} are not considered simple value types.
    * @param type the type to check
    * @return whether the given type represents a "simple" value type
    * @see #isSimpleProperty(Class)
    */
   public static boolean isSimpleValueType(Class<?> type) {
      return (Void.class != type && void.class != type &&
         (ClassUtils.isPrimitiveOrWrapper(type) ||
            Enum.class.isAssignableFrom(type) ||
            CharSequence.class.isAssignableFrom(type) ||
            Number.class.isAssignableFrom(type) ||
            Date.class.isAssignableFrom(type) ||
            Temporal.class.isAssignableFrom(type) ||
            URI.class == type ||
            URL.class == type ||
            Locale.class == type ||
            Class.class == type));
   }


   /**
    * Copy the property values of the given source bean into the target bean.
    * <p>Note: The source and target classes do not have to match or even be derived
    * from each other, as long as the properties match. Any bean properties that the
    * source bean exposes but the target bean does not will silently be ignored.
    * <p>This is just a convenience method. For more complex transfer needs,
    * consider using a full ArtifactWrapper.
    * @param source the source bean
    * @param target the target bean
    * @throws ArtifactsException if the copying failed
    * @see ArtifactWrapper
    */
   public static void copyProperties(Object source, Object target) throws ArtifactsException {
      copyProperties(source, target, null, (String[]) null);
   }

   /**
    * Copy the property values of the given source bean into the given target bean,
    * only setting properties defined in the given "editable" class (or interface).
    * <p>Note: The source and target classes do not have to match or even be derived
    * from each other, as long as the properties match. Any bean properties that the
    * source bean exposes but the target bean does not will silently be ignored.
    * <p>This is just a convenience method. For more complex transfer needs,
    * consider using a full ArtifactWrapper.
    * @param source the source bean
    * @param target the target bean
    * @param editable the class (or interface) to restrict property setting to
    * @throws ArtifactsException if the copying failed
    * @see ArtifactWrapper
    */
   public static void copyProperties(Object source, Object target, Class<?> editable) throws ArtifactsException {
      copyProperties(source, target, editable, (String[]) null);
   }

   /**
    * Copy the property values of the given source bean into the given target bean,
    * ignoring the given "ignoreProperties".
    * <p>Note: The source and target classes do not have to match or even be derived
    * from each other, as long as the properties match. Any bean properties that the
    * source bean exposes but the target bean does not will silently be ignored.
    * <p>This is just a convenience method. For more complex transfer needs,
    * consider using a full ArtifactWrapper.
    * @param source the source bean
    * @param target the target bean
    * @param ignoreProperties array of property names to ignore
    * @throws ArtifactsException if the copying failed
    * @see ArtifactWrapper
    */
   public static void copyProperties(Object source, Object target, String... ignoreProperties) throws ArtifactsException {
      copyProperties(source, target, null, ignoreProperties);
   }

   /**
    * Copy the property values of the given source bean into the given target bean.
    * <p>Note: The source and target classes do not have to match or even be derived
    * from each other, as long as the properties match. Any bean properties that the
    * source bean exposes but the target bean does not will silently be ignored.
    * @param source the source bean
    * @param target the target bean
    * @param editable the class (or interface) to restrict property setting to
    * @param ignoreProperties array of property names to ignore
    * @throws ArtifactsException if the copying failed
    * @see ArtifactWrapper
    */
   private static void copyProperties(Object source, Object target, @Nullable Class<?> editable,
                                      @Nullable String... ignoreProperties) throws ArtifactsException {

      AssertUtils.notNull(source, "Source must not be null");
      AssertUtils.notNull(target, "Target must not be null");

      Class<?> actualEditable = target.getClass();
      if (editable != null) {
         if (!editable.isInstance(target)) {
            throw new IllegalArgumentException("Target class [" + target.getClass().getName() +
               "] not assignable to Editable class [" + editable.getName() + "]");
         }
         actualEditable = editable;
      }
      PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
      List<String> ignoreList = (ignoreProperties != null ? Arrays.asList(ignoreProperties) : null);

      for (PropertyDescriptor targetPd : targetPds) {
         Method writeMethod = targetPd.getWriteMethod();
         if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {
            PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
            if (sourcePd != null) {
               Method readMethod = sourcePd.getReadMethod();
               if (readMethod != null &&
                  ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType())) {
                  try {
                     if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                        readMethod.setAccessible(true);
                     }
                     Object value = readMethod.invoke(source);
                     if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                        writeMethod.setAccessible(true);
                     }
                     writeMethod.invoke(target, value);
                  }
                  catch (Throwable ex) {
                     throw new FatalArtifactException(
                        "Could not copy property '" + targetPd.getName() + "' from source to target", ex);
                  }
               }
            }
         }
      }
   }
}

