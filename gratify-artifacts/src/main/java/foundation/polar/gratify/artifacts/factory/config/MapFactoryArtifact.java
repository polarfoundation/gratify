package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactUtils;
import foundation.polar.gratify.artifacts.TypeConverter;
import foundation.polar.gratify.core.ResolvableType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple factory for shared Map instances. Allows for central setup
 * of Maps via the "map" element in XML bean definitions.
 *
 * @author Juergen Hoeller
 *
 * @see SetFactoryArtifact
 * @see ListFactoryArtifact
 */
public class MapFactoryArtifact extends AbstractFactoryArtifact<Map<Object, Object>> {
   @Nullable
   private Map<?, ?> sourceMap;

   @SuppressWarnings("rawtypes")
   @Nullable
   private Class<? extends Map> targetMapClass;

   /**
    * Set the source Map, typically populated via XML "map" elements.
    */
   public void setSourceMap(Map<?, ?> sourceMap) {
      this.sourceMap = sourceMap;
   }

   /**
    * Set the class to use for the target Map. Can be populated with a fully
    * qualified class name when defined in a Gratify application context.
    * <p>Default is a linked HashMap, keeping the registration order.
    * @see java.util.LinkedHashMap
    */
   @SuppressWarnings("rawtypes")
   public void setTargetMapClass(@Nullable Class<? extends Map> targetMapClass) {
      if (targetMapClass == null) {
         throw new IllegalArgumentException("'targetMapClass' must not be null");
      }
      if (!Map.class.isAssignableFrom(targetMapClass)) {
         throw new IllegalArgumentException("'targetMapClass' must implement [java.util.Map]");
      }
      this.targetMapClass = targetMapClass;
   }

   @Override
   @SuppressWarnings("rawtypes")
   public Class<Map> getObjectType() {
      return Map.class;
   }

   @Override
   @SuppressWarnings("unchecked")
   protected Map<Object, Object> createInstance() {
      if (this.sourceMap == null) {
         throw new IllegalArgumentException("'sourceMap' is required");
      }
      Map<Object, Object> result = null;
      if (this.targetMapClass != null) {
         result = ArtifactUtils.instantiateClass(this.targetMapClass);
      }
      else {
         result = new LinkedHashMap<>(this.sourceMap.size());
      }
      Class<?> keyType = null;
      Class<?> valueType = null;
      if (this.targetMapClass != null) {
         ResolvableType mapType = ResolvableType.forClass(this.targetMapClass).asMap();
         keyType = mapType.resolveGeneric(0);
         valueType = mapType.resolveGeneric(1);
      }
      if (keyType != null || valueType != null) {
         TypeConverter converter = getArtifactTypeConverter();
         for (Map.Entry<?, ?> entry : this.sourceMap.entrySet()) {
            Object convertedKey = converter.convertIfNecessary(entry.getKey(), keyType);
            Object convertedValue = converter.convertIfNecessary(entry.getValue(), valueType);
            result.put(convertedKey, convertedValue);
         }
      }
      else {
         result.putAll(this.sourceMap);
      }
      return result;
   }
}
