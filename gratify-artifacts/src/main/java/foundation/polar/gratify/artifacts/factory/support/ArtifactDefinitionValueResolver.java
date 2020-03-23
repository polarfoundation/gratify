package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.ArtifactWrapper;
import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.TypeConverter;
import foundation.polar.gratify.artifacts.factory.*;
import foundation.polar.gratify.artifacts.factory.config.*;
import foundation.polar.gratify.utils.ClassUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Helper class for use in artifact factory implementations,
 * resolving values contained in artifact definition objects
 * into the actual values applied to the target artifact instance.
 *
 * <p>Operates on an {@link AbstractArtifactFactory} and a plain
 * {@link foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition} object.
 * Used by {@link AbstractAutowireCapableArtifactFactory}.
 *
 * @author Juergen Hoeller
 * @see AbstractAutowireCapableArtifactFactory
 */
public class ArtifactDefinitionValueResolver {
   private final AbstractAutowireCapableArtifactFactory artifactFactory;

   private final String artifactName;

   private final ArtifactDefinition artifactDefinition;

   private final TypeConverter typeConverter;

   /**
    * Create a ArtifactDefinitionValueResolver for the given ArtifactFactory and ArtifactDefinition.
    * @param artifactFactory the ArtifactFactory to resolve against
    * @param artifactName the name of the artifact that we work on
    * @param artifactDefinition the ArtifactDefinition of the artifact that we work on
    * @param typeConverter the TypeConverter to use for resolving TypedStringValues
    */
   public ArtifactDefinitionValueResolver(AbstractAutowireCapableArtifactFactory artifactFactory, String artifactName,
                                      ArtifactDefinition artifactDefinition, TypeConverter typeConverter) {

      this.artifactFactory = artifactFactory;
      this.artifactName = artifactName;
      this.artifactDefinition = artifactDefinition;
      this.typeConverter = typeConverter;
   }

   /**
    * Given a PropertyValue, return a value, resolving any references to other
    * artifacts in the factory if necessary. The value could be:
    * <li>A ArtifactDefinition, which leads to the creation of a corresponding
    * new artifact instance. Singleton flags and names of such "inner artifacts"
    * are always ignored: Inner artifacts are anonymous prototypes.
    * <li>A RuntimeArtifactReference, which must be resolved.
    * <li>A ManagedList. This is a special collection that may contain
    * RuntimeArtifactReferences or Collections that will need to be resolved.
    * <li>A ManagedSet. May also contain RuntimeArtifactReferences or
    * Collections that will need to be resolved.
    * <li>A ManagedMap. In this case the value may be a RuntimeArtifactReference
    * or Collection that will need to be resolved.
    * <li>An ordinary object or {@code null}, in which case it's left alone.
    * @param argName the name of the argument that the value is defined for
    * @param value the value object to resolve
    * @return the resolved object
    */
   @Nullable
   public Object resolveValueIfNecessary(Object argName, @Nullable Object value) {
      // We must check each value to see whether it requires a runtime reference
      // to another artifact to be resolved.
      if (value instanceof RuntimeArtifactReference) {
         RuntimeArtifactReference ref = (RuntimeArtifactReference) value;
         return resolveReference(argName, ref);
      }
      else if (value instanceof RuntimeArtifactNameReference) {
         String refName = ((RuntimeArtifactNameReference) value).getArtifactName();
         refName = String.valueOf(doEvaluate(refName));
         if (!this.artifactFactory.containsArtifact(refName)) {
            throw new ArtifactDefinitionStoreException(
               "Invalid artifact name '" + refName + "' in artifact reference for " + argName);
         }
         return refName;
      }
      else if (value instanceof ArtifactDefinitionHolder) {
         // Resolve ArtifactDefinitionHolder: contains ArtifactDefinition with name and aliases.
         ArtifactDefinitionHolder bdHolder = (ArtifactDefinitionHolder) value;
         return resolveInnerArtifact(argName, bdHolder.getArtifactName(), bdHolder.getArtifactDefinition());
      }
      else if (value instanceof ArtifactDefinition) {
         // Resolve plain ArtifactDefinition, without contained name: use dummy name.
         ArtifactDefinition bd = (ArtifactDefinition) value;
         String innerArtifactName = "(inner artifact)" + ArtifactFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR +
            ObjectUtils.getIdentityHexString(bd);
         return resolveInnerArtifact(argName, innerArtifactName, bd);
      }
      else if (value instanceof DependencyDescriptor) {
         Set<String> autowiredArtifactNames = new LinkedHashSet<>(4);
         Object result = this.artifactFactory.resolveDependency(
            (DependencyDescriptor) value, this.artifactName, autowiredArtifactNames, this.typeConverter);
         for (String autowiredArtifactName : autowiredArtifactNames) {
            if (this.artifactFactory.containsArtifact(autowiredArtifactName)) {
               this.artifactFactory.registerDependentArtifact(autowiredArtifactName, this.artifactName);
            }
         }
         return result;
      }
      else if (value instanceof ManagedArray) {
         // May need to resolve contained runtime references.
         ManagedArray array = (ManagedArray) value;
         Class<?> elementType = array.resolvedElementType;
         if (elementType == null) {
            String elementTypeName = array.getElementTypeName();
            if (StringUtils.hasText(elementTypeName)) {
               try {
                  elementType = ClassUtils.forName(elementTypeName, this.artifactFactory.getArtifactClassLoader());
                  array.resolvedElementType = elementType;
               }
               catch (Throwable ex) {
                  // Improve the message by showing the context.
                  throw new ArtifactCreationException(
                     this.artifactDefinition.getResourceDescription(), this.artifactName,
                     "Error resolving array type for " + argName, ex);
               }
            }
            else {
               elementType = Object.class;
            }
         }
         return resolveManagedArray(argName, (List<?>) value, elementType);
      }
      else if (value instanceof ManagedList) {
         // May need to resolve contained runtime references.
         return resolveManagedList(argName, (List<?>) value);
      }
      else if (value instanceof ManagedSet) {
         // May need to resolve contained runtime references.
         return resolveManagedSet(argName, (Set<?>) value);
      }
      else if (value instanceof ManagedMap) {
         // May need to resolve contained runtime references.
         return resolveManagedMap(argName, (Map<?, ?>) value);
      }
      else if (value instanceof ManagedProperties) {
         Properties original = (Properties) value;
         Properties copy = new Properties();
         original.forEach((propKey, propValue) -> {
            if (propKey instanceof TypedStringValue) {
               propKey = evaluate((TypedStringValue) propKey);
            }
            if (propValue instanceof TypedStringValue) {
               propValue = evaluate((TypedStringValue) propValue);
            }
            if (propKey == null || propValue == null) {
               throw new ArtifactCreationException(
                  this.artifactDefinition.getResourceDescription(), this.artifactName,
                  "Error converting Properties key/value pair for " + argName + ": resolved to null");
            }
            copy.put(propKey, propValue);
         });
         return copy;
      }
      else if (value instanceof TypedStringValue) {
         // Convert value to target type here.
         TypedStringValue typedStringValue = (TypedStringValue) value;
         Object valueObject = evaluate(typedStringValue);
         try {
            Class<?> resolvedTargetType = resolveTargetType(typedStringValue);
            if (resolvedTargetType != null) {
               return this.typeConverter.convertIfNecessary(valueObject, resolvedTargetType);
            }
            else {
               return valueObject;
            }
         }
         catch (Throwable ex) {
            // Improve the message by showing the context.
            throw new ArtifactCreationException(
               this.artifactDefinition.getResourceDescription(), this.artifactName,
               "Error converting typed String value for " + argName, ex);
         }
      }
      else if (value instanceof NullArtifact) {
         return null;
      }
      else {
         return evaluate(value);
      }
   }

   /**
    * Evaluate the given value as an expression, if necessary.
    * @param value the candidate value (may be an expression)
    * @return the resolved value
    */
   @Nullable
   protected Object evaluate(TypedStringValue value) {
      Object result = doEvaluate(value.getValue());
      if (!ObjectUtils.nullSafeEquals(result, value.getValue())) {
         value.setDynamic();
      }
      return result;
   }

   /**
    * Evaluate the given value as an expression, if necessary.
    * @param value the original value (may be an expression)
    * @return the resolved value if necessary, or the original value
    */
   @Nullable
   protected Object evaluate(@Nullable Object value) {
      if (value instanceof String) {
         return doEvaluate((String) value);
      }
      else if (value instanceof String[]) {
         String[] values = (String[]) value;
         boolean actuallyResolved = false;
         Object[] resolvedValues = new Object[values.length];
         for (int i = 0; i < values.length; i++) {
            String originalValue = values[i];
            Object resolvedValue = doEvaluate(originalValue);
            if (resolvedValue != originalValue) {
               actuallyResolved = true;
            }
            resolvedValues[i] = resolvedValue;
         }
         return (actuallyResolved ? resolvedValues : values);
      }
      else {
         return value;
      }
   }

   /**
    * Evaluate the given String value as an expression, if necessary.
    * @param value the original value (may be an expression)
    * @return the resolved value if necessary, or the original String value
    */
   @Nullable
   private Object doEvaluate(@Nullable String value) {
      return this.artifactFactory.evaluateArtifactDefinitionString(value, this.artifactDefinition);
   }

   /**
    * Resolve the target type in the given TypedStringValue.
    * @param value the TypedStringValue to resolve
    * @return the resolved target type (or {@code null} if none specified)
    * @throws ClassNotFoundException if the specified type cannot be resolved
    * @see TypedStringValue#resolveTargetType
    */
   @Nullable
   protected Class<?> resolveTargetType(TypedStringValue value) throws ClassNotFoundException {
      if (value.hasTargetType()) {
         return value.getTargetType();
      }
      return value.resolveTargetType(this.artifactFactory.getArtifactClassLoader());
   }

   /**
    * Resolve a reference to another artifact in the factory.
    */
   @Nullable
   private Object resolveReference(Object argName, RuntimeArtifactReference ref) {
      try {
         Object artifact;
         Class<?> artifactType = ref.getArtifactType();
         if (ref.isToParent()) {
            ArtifactFactory parent = this.artifactFactory.getParentArtifactFactory();
            if (parent == null) {
               throw new ArtifactCreationException(
                  this.artifactDefinition.getResourceDescription(), this.artifactName,
                  "Cannot resolve reference to artifact " + ref +
                     " in parent factory: no parent factory available");
            }
            if (artifactType != null) {
               artifact = parent.getArtifact(artifactType);
            }
            else {
               artifact = parent.getArtifact(String.valueOf(doEvaluate(ref.getArtifactName())));
            }
         }
         else {
            String resolvedName;
            if (artifactType != null) {
               NamedArtifactHolder<?> namedArtifact = this.artifactFactory.resolveNamedArtifact(artifactType);
               artifact = namedArtifact.getArtifactInstance();
               resolvedName = namedArtifact.getArtifactName();
            }
            else {
               resolvedName = String.valueOf(doEvaluate(ref.getArtifactName()));
               artifact = this.artifactFactory.getArtifact(resolvedName);
            }
            this.artifactFactory.registerDependentArtifact(resolvedName, this.artifactName);
         }
         if (artifact instanceof NullArtifact) {
            artifact = null;
         }
         return artifact;
      }
      catch (ArtifactsException ex) {
         throw new ArtifactCreationException(
            this.artifactDefinition.getResourceDescription(), this.artifactName,
            "Cannot resolve reference to artifact '" + ref.getArtifactName() + "' while setting " + argName, ex);
      }
   }

   /**
    * Resolve an inner artifact definition.
    * @param argName the name of the argument that the inner artifact is defined for
    * @param innerArtifactName the name of the inner artifact
    * @param innerBd the artifact definition for the inner artifact
    * @return the resolved inner artifact instance
    */
   @Nullable
   private Object resolveInnerArtifact(Object argName, String innerArtifactName, ArtifactDefinition innerBd) {
      RootArtifactDefinition mbd = null;
      try {
         mbd = this.artifactFactory.getMergedArtifactDefinition(innerArtifactName, innerBd, this.artifactDefinition);
         // Check given artifact name whether it is unique. If not already unique,
         // add counter - increasing the counter until the name is unique.
         String actualInnerArtifactName = innerArtifactName;
         if (mbd.isSingleton()) {
            actualInnerArtifactName = adaptInnerArtifactName(innerArtifactName);
         }
         this.artifactFactory.registerContainedArtifact(actualInnerArtifactName, this.artifactName);
         // Guarantee initialization of artifacts that the inner artifact depends on.
         String[] dependsOn = mbd.getDependsOn();
         if (dependsOn != null) {
            for (String dependsOnArtifact : dependsOn) {
               this.artifactFactory.registerDependentArtifact(dependsOnArtifact, actualInnerArtifactName);
               this.artifactFactory.getArtifact(dependsOnArtifact);
            }
         }
         // Actually create the inner artifact instance now...
         Object innerArtifact = this.artifactFactory.createArtifact(actualInnerArtifactName, mbd, null);
         if (innerArtifact instanceof FactoryArtifact) {
            boolean synthetic = mbd.isSynthetic();
            innerArtifact = this.artifactFactory.getObjectFromFactoryArtifact(
               (FactoryArtifact<?>) innerArtifact, actualInnerArtifactName, !synthetic);
         }
         if (innerArtifact instanceof NullArtifact) {
            innerArtifact = null;
         }
         return innerArtifact;
      }
      catch (ArtifactsException ex) {
         throw new ArtifactCreationException(
            this.artifactDefinition.getResourceDescription(), this.artifactName,
            "Cannot create inner artifact '" + innerArtifactName + "' " +
               (mbd != null && mbd.getArtifactClassName() != null ? "of type [" + mbd.getArtifactClassName() + "] " : "") +
               "while setting " + argName, ex);
      }
   }

   /**
    * Checks the given artifact name whether it is unique. If not already unique,
    * a counter is added, increasing the counter until the name is unique.
    * @param innerArtifactName the original name for the inner artifact
    * @return the adapted name for the inner artifact
    */
   private String adaptInnerArtifactName(String innerArtifactName) {
      String actualInnerArtifactName = innerArtifactName;
      int counter = 0;
      String prefix = innerArtifactName + ArtifactFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;
      while (this.artifactFactory.isArtifactNameInUse(actualInnerArtifactName)) {
         counter++;
         actualInnerArtifactName = prefix + counter;
      }
      return actualInnerArtifactName;
   }

   /**
    * For each element in the managed array, resolve reference if necessary.
    */
   private Object resolveManagedArray(Object argName, List<?> ml, Class<?> elementType) {
      Object resolved = Array.newInstance(elementType, ml.size());
      for (int i = 0; i < ml.size(); i++) {
         Array.set(resolved, i, resolveValueIfNecessary(new KeyedArgName(argName, i), ml.get(i)));
      }
      return resolved;
   }

   /**
    * For each element in the managed list, resolve reference if necessary.
    */
   private List<?> resolveManagedList(Object argName, List<?> ml) {
      List<Object> resolved = new ArrayList<>(ml.size());
      for (int i = 0; i < ml.size(); i++) {
         resolved.add(resolveValueIfNecessary(new KeyedArgName(argName, i), ml.get(i)));
      }
      return resolved;
   }

   /**
    * For each element in the managed set, resolve reference if necessary.
    */
   private Set<?> resolveManagedSet(Object argName, Set<?> ms) {
      Set<Object> resolved = new LinkedHashSet<>(ms.size());
      int i = 0;
      for (Object m : ms) {
         resolved.add(resolveValueIfNecessary(new KeyedArgName(argName, i), m));
         i++;
      }
      return resolved;
   }

   /**
    * For each element in the managed map, resolve reference if necessary.
    */
   private Map<?, ?> resolveManagedMap(Object argName, Map<?, ?> mm) {
      Map<Object, Object> resolved = new LinkedHashMap<>(mm.size());
      mm.forEach((key, value) -> {
         Object resolvedKey = resolveValueIfNecessary(argName, key);
         Object resolvedValue = resolveValueIfNecessary(new KeyedArgName(argName, key), value);
         resolved.put(resolvedKey, resolvedValue);
      });
      return resolved;
   }

   /**
    * Holder class used for delayed toString building.
    */
   private static class KeyedArgName {

      private final Object argName;

      private final Object key;

      public KeyedArgName(Object argName, Object key) {
         this.argName = argName;
         this.key = key;
      }

      @Override
      public String toString() {
         return this.argName + " with key " + ArtifactWrapper.PROPERTY_KEY_PREFIX +
            this.key + ArtifactWrapper.PROPERTY_KEY_SUFFIX;
      }
   }

}
