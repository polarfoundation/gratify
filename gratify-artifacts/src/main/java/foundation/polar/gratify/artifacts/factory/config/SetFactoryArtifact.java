package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactUtils;
import foundation.polar.gratify.artifacts.TypeConverter;
import foundation.polar.gratify.core.ResolvableType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Simple factory for shared Set instances. Allows for central setup
 * of Sets via the "set" element in XML bean definitions.
 *
 * @author Juergen Hoeller
 * @see ListFactoryArtifact
 * @see MapFactoryArtifact
 */
public class SetFactoryArtifact extends AbstractFactoryArtifact<Set<Object>> {
   @Nullable
   private Set<?> sourceSet;

   @SuppressWarnings("rawtypes")
   @Nullable
   private Class<? extends Set> targetSetClass;

   /**
    * Set the source Set, typically populated via XML "set" elements.
    */
   public void setSourceSet(Set<?> sourceSet) {
      this.sourceSet = sourceSet;
   }

   /**
    * Set the class to use for the target Set. Can be populated with a fully
    * qualified class name when defined in a Gratify application context.
    * <p>Default is a linked HashSet, keeping the registration order.
    * @see java.util.LinkedHashSet
    */
   @SuppressWarnings("rawtypes")
   public void setTargetSetClass(@Nullable Class<? extends Set> targetSetClass) {
      if (targetSetClass == null) {
         throw new IllegalArgumentException("'targetSetClass' must not be null");
      }
      if (!Set.class.isAssignableFrom(targetSetClass)) {
         throw new IllegalArgumentException("'targetSetClass' must implement [java.util.Set]");
      }
      this.targetSetClass = targetSetClass;
   }


   @Override
   @SuppressWarnings("rawtypes")
   public Class<Set> getObjectType() {
      return Set.class;
   }

   @Override
   @SuppressWarnings("unchecked")
   protected Set<Object> createInstance() {
      if (this.sourceSet == null) {
         throw new IllegalArgumentException("'sourceSet' is required");
      }
      Set<Object> result = null;
      if (this.targetSetClass != null) {
         result = ArtifactUtils.instantiateClass(this.targetSetClass);
      }
      else {
         result = new LinkedHashSet<>(this.sourceSet.size());
      }
      Class<?> valueType = null;
      if (this.targetSetClass != null) {
         valueType = ResolvableType.forClass(this.targetSetClass).asCollection().resolveGeneric();
      }
      if (valueType != null) {
         TypeConverter converter = getArtifactTypeConverter();
         for (Object elem : this.sourceSet) {
            result.add(converter.convertIfNecessary(elem, valueType));
         }
      }
      else {
         result.addAll(this.sourceSet);
      }
      return result;
   }
}
