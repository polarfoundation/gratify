package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.annotation.AnnotationUtils;
import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.factory.*;
import foundation.polar.gratify.core.OrderComparator;
import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Stream;

/**
 * Static {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory} implementation
 * which allows to register existing singleton instances programmatically.
 * Does not have support for prototype artifacts or aliases.
 *
 * <p>Serves as example for a simple implementation of the
 * {@link foundation.polar.gratify.artifacts.factory.ListableArtifactFactory} interface,
 * managing existing bean instances rather than creating new ones based on bean
 * definitions, and not implementing any extended SPI interfaces (such as
 * {@link foundation.polar.gratify.artifacts.factory.config.ConfigurableArtifactFactory}).
 *
 * <p>For a full-fledged factory based on bean definitions, have a look
 * at {@link DefaultListableArtifactFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 *
 * @see DefaultListableArtifactFactory
 */
public class StaticListableArtifactFactory implements ListableArtifactFactory {
   /** Map from bean name to bean instance. */
   private final Map<String, Object> artifacts;

   /**
    * Create a regular {@code StaticListableArtifactFactory}, to be populated
    * with singleton bean instances through {@link #addArtifact} calls.
    */
   public StaticListableArtifactFactory() {
      this.artifacts = new LinkedHashMap<>();
   }

   /**
    * Create a {@code StaticListableArtifactFactory} wrapping the given {@code Map}.
    * <p>Note that the given {@code Map} may be pre-populated with artifacts;
    * or new, still allowing for artifacts to be registered via {@link #addArtifact};
    * or {@link java.util.Collections#emptyMap()} for a dummy factory which
    * enforces operating against an empty set of artifacts.
    * @param artifacts a {@code Map} for holding this factory's artifacts, with the
    * bean name String as key and the corresponding singleton object as value
    */
   public StaticListableArtifactFactory(Map<String, Object> artifacts) {
      AssertUtils.notNull(artifacts, "Artifacts Map must not be null");
      this.artifacts = artifacts;
   }

   /**
    * Add a new singleton bean.
    * Will overwrite any existing instance for the given name.
    * @param name the name of the bean
    * @param bean the bean instance
    */
   public void addArtifact(String name, Object bean) {
      this.artifacts.put(name, bean);
   }

   //---------------------------------------------------------------------
   // Implementation of ArtifactFactory interface
   //---------------------------------------------------------------------

   @Override
   public Object getArtifact(String name) throws ArtifactsException {
      String beanName = ArtifactFactoryUtils.transformedArtifactName(name);
      Object bean = this.artifacts.get(beanName);

      if (bean == null) {
         throw new NoSuchArtifactDefinitionException(beanName,
            "Defined artifacts are [" + StringUtils.collectionToCommaDelimitedString(this.artifacts.keySet()) + "]");
      }

      // Don't let calling code try to dereference the
      // bean factory if the bean isn't a factory
      if (ArtifactFactoryUtils.isFactoryDereference(name) && !(bean instanceof FactoryArtifact)) {
         throw new ArtifactIsNotAFactoryException(beanName, bean.getClass());
      }

      if (bean instanceof FactoryArtifact && !ArtifactFactoryUtils.isFactoryDereference(name)) {
         try {
            Object exposedObject = ((FactoryArtifact<?>) bean).getObject();
            if (exposedObject == null) {
               throw new ArtifactCreationException(beanName, "FactoryArtifact exposed null object");
            }
            return exposedObject;
         }
         catch (Exception ex) {
            throw new ArtifactCreationException(beanName, "FactoryArtifact threw exception on object creation", ex);
         }
      }
      else {
         return bean;
      }
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> T getArtifact(String name, @Nullable Class<T> requiredType) throws ArtifactsException {
      Object bean = getArtifact(name);
      if (requiredType != null && !requiredType.isInstance(bean)) {
         throw new ArtifactNotOfRequiredTypeException(name, requiredType, bean.getClass());
      }
      return (T) bean;
   }

   @Override
   public Object getArtifact(String name, Object... args) throws ArtifactsException {
      if (!ObjectUtils.isEmpty(args)) {
         throw new UnsupportedOperationException(
            "StaticListableArtifactFactory does not support explicit bean creation arguments");
      }
      return getArtifact(name);
   }

   @Override
   public <T> T getArtifact(Class<T> requiredType) throws ArtifactsException {
      String[] beanNames = getArtifactNamesForType(requiredType);
      if (beanNames.length == 1) {
         return getArtifact(beanNames[0], requiredType);
      }
      else if (beanNames.length > 1) {
         throw new NoUniqueArtifactDefinitionException(requiredType, beanNames);
      }
      else {
         throw new NoSuchArtifactDefinitionException(requiredType);
      }
   }

   @Override
   public <T> T getArtifact(Class<T> requiredType, Object... args) throws ArtifactsException {
      if (!ObjectUtils.isEmpty(args)) {
         throw new UnsupportedOperationException(
            "StaticListableArtifactFactory does not support explicit bean creation arguments");
      }
      return getArtifact(requiredType);
   }

   @Override
   public <T> ObjectProvider<T> getArtifactProvider(Class<T> requiredType) throws ArtifactsException {
      return getArtifactProvider(ResolvableType.forRawClass(requiredType));
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> ObjectProvider<T> getArtifactProvider(ResolvableType requiredType) {
      return new ObjectProvider<T>() {
         @Override
         public T getObject() throws ArtifactsException {
            String[] beanNames = getArtifactNamesForType(requiredType);
            if (beanNames.length == 1) {
               return (T) getArtifact(beanNames[0], requiredType);
            }
            else if (beanNames.length > 1) {
               throw new NoUniqueArtifactDefinitionException(requiredType, beanNames);
            }
            else {
               throw new NoSuchArtifactDefinitionException(requiredType);
            }
         }
         @Override
         public T getObject(Object... args) throws ArtifactsException {
            String[] beanNames = getArtifactNamesForType(requiredType);
            if (beanNames.length == 1) {
               return (T) getArtifact(beanNames[0], args);
            }
            else if (beanNames.length > 1) {
               throw new NoUniqueArtifactDefinitionException(requiredType, beanNames);
            }
            else {
               throw new NoSuchArtifactDefinitionException(requiredType);
            }
         }
         @Override
         @Nullable
         public T getIfAvailable() throws ArtifactsException {
            String[] beanNames = getArtifactNamesForType(requiredType);
            if (beanNames.length == 1) {
               return (T) getArtifact(beanNames[0]);
            }
            else if (beanNames.length > 1) {
               throw new NoUniqueArtifactDefinitionException(requiredType, beanNames);
            }
            else {
               return null;
            }
         }
         @Override
         @Nullable
         public T getIfUnique() throws ArtifactsException {
            String[] beanNames = getArtifactNamesForType(requiredType);
            if (beanNames.length == 1) {
               return (T) getArtifact(beanNames[0]);
            }
            else {
               return null;
            }
         }
         @Override
         public Stream<T> stream() {
            return Arrays.stream(getArtifactNamesForType(requiredType)).map(name -> (T) getArtifact(name));
         }
         @Override
         public Stream<T> orderedStream() {
            return stream().sorted(OrderComparator.INSTANCE);
         }
      };
   }

   @Override
   public boolean containsArtifact(String name) {
      return this.artifacts.containsKey(name);
   }

   @Override
   public boolean isSingleton(String name) throws NoSuchArtifactDefinitionException {
      Object bean = getArtifact(name);
      // In case of FactoryArtifact, return singleton status of created object.
      return (bean instanceof FactoryArtifact && ((FactoryArtifact<?>) bean).isSingleton());
   }

   @Override
   public boolean isPrototype(String name) throws NoSuchArtifactDefinitionException {
      Object bean = getArtifact(name);
      // In case of FactoryArtifact, return prototype status of created object.
      return ((bean instanceof SmartFactoryArtifact && ((SmartFactoryArtifact<?>) bean).isPrototype()) ||
         (bean instanceof FactoryArtifact && !((FactoryArtifact<?>) bean).isSingleton()));
   }

   @Override
   public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchArtifactDefinitionException {
      Class<?> type = getType(name);
      return (type != null && typeToMatch.isAssignableFrom(type));
   }

   @Override
   public boolean isTypeMatch(String name, @Nullable Class<?> typeToMatch) throws NoSuchArtifactDefinitionException {
      Class<?> type = getType(name);
      return (typeToMatch == null || (type != null && typeToMatch.isAssignableFrom(type)));
   }

   @Override
   public Class<?> getType(String name) throws NoSuchArtifactDefinitionException {
      return getType(name, true);
   }

   @Override
   public Class<?> getType(String name, boolean allowFactoryArtifactInit) throws NoSuchArtifactDefinitionException {
      String beanName = ArtifactFactoryUtils.transformedArtifactName(name);

      Object bean = this.artifacts.get(beanName);
      if (bean == null) {
         throw new NoSuchArtifactDefinitionException(beanName,
            "Defined artifacts are [" + StringUtils.collectionToCommaDelimitedString(this.artifacts.keySet()) + "]");
      }

      if (bean instanceof FactoryArtifact && !ArtifactFactoryUtils.isFactoryDereference(name)) {
         // If it's a FactoryArtifact, we want to look at what it creates, not the factory class.
         return ((FactoryArtifact<?>) bean).getObjectType();
      }
      return bean.getClass();
   }

   @Override
   public String[] getAliases(String name) {
      return new String[0];
   }

   //---------------------------------------------------------------------
   // Implementation of ListableArtifactFactory interface
   //---------------------------------------------------------------------

   @Override
   public boolean containsArtifactDefinition(String name) {
      return this.artifacts.containsKey(name);
   }

   @Override
   public int getArtifactDefinitionCount() {
      return this.artifacts.size();
   }

   @Override
   public String[] getArtifactDefinitionNames() {
      return StringUtils.toStringArray(this.artifacts.keySet());
   }

   @Override
   public String[] getArtifactNamesForType(@Nullable ResolvableType type) {
      return getArtifactNamesForType(type, true, true);
   }

   @Override
   public String[] getArtifactNamesForType(@Nullable ResolvableType type,
                                       boolean includeNonSingletons, boolean allowEagerInit) {
      Class<?> resolved = (type != null ? type.resolve() : null);
      boolean isFactoryType = resolved != null && FactoryArtifact.class.isAssignableFrom(resolved);
      List<String> matches = new ArrayList<>();

      for (Map.Entry<String, Object> entry : this.artifacts.entrySet()) {
         String beanName = entry.getKey();
         Object beanInstance = entry.getValue();
         if (beanInstance instanceof FactoryArtifact && !isFactoryType) {
            FactoryArtifact<?> factoryArtifact = (FactoryArtifact<?>) beanInstance;
            Class<?> objectType = factoryArtifact.getObjectType();
            if ((includeNonSingletons || factoryArtifact.isSingleton()) &&
               objectType != null && (type == null || type.isAssignableFrom(objectType))) {
               matches.add(beanName);
            }
         }
         else {
            if (type == null || type.isInstance(beanInstance)) {
               matches.add(beanName);
            }
         }
      }
      return StringUtils.toStringArray(matches);
   }

   @Override
   public String[] getArtifactNamesForType(@Nullable Class<?> type) {
      return getArtifactNamesForType(ResolvableType.forClass(type));
   }

   @Override
   public String[] getArtifactNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
      return getArtifactNamesForType(ResolvableType.forClass(type), includeNonSingletons, allowEagerInit);
   }

   @Override
   public <T> Map<String, T> getArtifactsOfType(@Nullable Class<T> type) throws ArtifactsException {
      return getArtifactsOfType(type, true, true);
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> Map<String, T> getArtifactsOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
      throws ArtifactsException {

      boolean isFactoryType = (type != null && FactoryArtifact.class.isAssignableFrom(type));
      Map<String, T> matches = new LinkedHashMap<>();

      for (Map.Entry<String, Object> entry : this.artifacts.entrySet()) {
         String beanName = entry.getKey();
         Object beanInstance = entry.getValue();
         // Is bean a FactoryArtifact?
         if (beanInstance instanceof FactoryArtifact && !isFactoryType) {
            // Match object created by FactoryArtifact.
            FactoryArtifact<?> factory = (FactoryArtifact<?>) beanInstance;
            Class<?> objectType = factory.getObjectType();
            if ((includeNonSingletons || factory.isSingleton()) &&
               objectType != null && (type == null || type.isAssignableFrom(objectType))) {
               matches.put(beanName, getArtifact(beanName, type));
            }
         }
         else {
            if (type == null || type.isInstance(beanInstance)) {
               // If type to match is FactoryArtifact, return FactoryArtifact itself.
               // Else, return bean instance.
               if (isFactoryType) {
                  beanName = FACTORY_BEAN_PREFIX + beanName;
               }
               matches.put(beanName, (T) beanInstance);
            }
         }
      }
      return matches;
   }

   @Override
   public String[] getArtifactNamesForAnnotation(Class<? extends Annotation> annotationType) {
      List<String> results = new ArrayList<>();
      for (String beanName : this.artifacts.keySet()) {
         if (findAnnotationOnArtifact(beanName, annotationType) != null) {
            results.add(beanName);
         }
      }
      return StringUtils.toStringArray(results);
   }

   @Override
   public Map<String, Object> getArtifactsWithAnnotation(Class<? extends Annotation> annotationType)
      throws ArtifactsException {

      Map<String, Object> results = new LinkedHashMap<>();
      for (String beanName : this.artifacts.keySet()) {
         if (findAnnotationOnArtifact(beanName, annotationType) != null) {
            results.put(beanName, getArtifact(beanName));
         }
      }
      return results;
   }

   @Override
   @Nullable
   public <A extends Annotation> A findAnnotationOnArtifact(String beanName, Class<A> annotationType)
      throws NoSuchArtifactDefinitionException {

      Class<?> beanType = getType(beanName);
      return (beanType != null ? AnnotationUtils.findAnnotation(beanType, annotationType) : null);
   }

}
