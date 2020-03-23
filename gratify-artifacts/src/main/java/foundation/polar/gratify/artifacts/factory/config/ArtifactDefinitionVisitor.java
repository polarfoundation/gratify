package foundation.polar.gratify.artifacts.factory.config;


import foundation.polar.gratify.artifacts.MutablePropertyValues;
import foundation.polar.gratify.artifacts.PropertyValue;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import foundation.polar.gratify.utils.StringValueResolver;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

/**
 * Visitor class for traversing {@link ArtifactDefinition} objects, in particular
 * the property values and constructor argument values contained in them,
 * resolving artifact metadata values.
 *
 * <p>Used by {@link PlaceholderConfigurerSupport} to parse all String values
 * contained in a ArtifactDefinition, resolving any placeholders found.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 *
 * @see ArtifactDefinition
 * @see ArtifactDefinition#getPropertyValues
 * @see ArtifactDefinition#getConstructorArgumentValues
 * @see PlaceholderConfigurerSupport
 */
public class ArtifactDefinitionVisitor {

   @Nullable
   private StringValueResolver valueResolver;

   /**
    * Create a new ArtifactDefinitionVisitor, applying the specified
    * value resolver to all artifact metadata values.
    * @param valueResolver the StringValueResolver to apply
    */
   public ArtifactDefinitionVisitor(StringValueResolver valueResolver) {
      AssertUtils.notNull(valueResolver, "StringValueResolver must not be null");
      this.valueResolver = valueResolver;
   }

   /**
    * Create a new ArtifactDefinitionVisitor for subclassing.
    * Subclasses need to override the {@link #resolveStringValue} method.
    */
   protected ArtifactDefinitionVisitor() {
   }

   /**
    * Traverse the given ArtifactDefinition object and the MutablePropertyValues
    * and ConstructorArgumentValues contained in them.
    * @param artifactDefinition the ArtifactDefinition object to traverse
    * @see #resolveStringValue(String)
    */
   public void visitArtifactDefinition(ArtifactDefinition artifactDefinition) {
      visitParentName(artifactDefinition);
      visitArtifactClassName(artifactDefinition);
      visitFactoryArtifactName(artifactDefinition);
      visitFactoryMethodName(artifactDefinition);
      visitScope(artifactDefinition);
      if (artifactDefinition.hasPropertyValues()) {
         visitPropertyValues(artifactDefinition.getPropertyValues());
      }
      if (artifactDefinition.hasConstructorArgumentValues()) {
         ConstructorArgumentValues cas = artifactDefinition.getConstructorArgumentValues();
         visitIndexedArgumentValues(cas.getIndexedArgumentValues());
         visitGenericArgumentValues(cas.getGenericArgumentValues());
      }
   }

   protected void visitParentName(ArtifactDefinition artifactDefinition) {
      String parentName = artifactDefinition.getParentName();
      if (parentName != null) {
         String resolvedName = resolveStringValue(parentName);
         if (!parentName.equals(resolvedName)) {
            artifactDefinition.setParentName(resolvedName);
         }
      }
   }

   protected void visitArtifactClassName(ArtifactDefinition artifactDefinition) {
      String artifactClassName = artifactDefinition.getArtifactClassName();
      if (artifactClassName != null) {
         String resolvedName = resolveStringValue(artifactClassName);
         if (!artifactClassName.equals(resolvedName)) {
            artifactDefinition.setArtifactClassName(resolvedName);
         }
      }
   }

   protected void visitFactoryArtifactName(ArtifactDefinition artifactDefinition) {
      String factoryArtifactName = artifactDefinition.getFactoryArtifactName();
      if (factoryArtifactName != null) {
         String resolvedName = resolveStringValue(factoryArtifactName);
         if (!factoryArtifactName.equals(resolvedName)) {
            artifactDefinition.setFactoryArtifactName(resolvedName);
         }
      }
   }

   protected void visitFactoryMethodName(ArtifactDefinition artifactDefinition) {
      String factoryMethodName = artifactDefinition.getFactoryMethodName();
      if (factoryMethodName != null) {
         String resolvedName = resolveStringValue(factoryMethodName);
         if (!factoryMethodName.equals(resolvedName)) {
            artifactDefinition.setFactoryMethodName(resolvedName);
         }
      }
   }

   protected void visitScope(ArtifactDefinition artifactDefinition) {
      String scope = artifactDefinition.getScope();
      if (scope != null) {
         String resolvedScope = resolveStringValue(scope);
         if (!scope.equals(resolvedScope)) {
            artifactDefinition.setScope(resolvedScope);
         }
      }
   }

   protected void visitPropertyValues(MutablePropertyValues pvs) {
      PropertyValue[] pvArray = pvs.getPropertyValues();
      for (PropertyValue pv : pvArray) {
         Object newVal = resolveValue(pv.getValue());
         if (!ObjectUtils.nullSafeEquals(newVal, pv.getValue())) {
            pvs.add(pv.getName(), newVal);
         }
      }
   }

   protected void visitIndexedArgumentValues(Map<Integer, ConstructorArgumentValues.ValueHolder> ias) {
      for (ConstructorArgumentValues.ValueHolder valueHolder : ias.values()) {
         Object newVal = resolveValue(valueHolder.getValue());
         if (!ObjectUtils.nullSafeEquals(newVal, valueHolder.getValue())) {
            valueHolder.setValue(newVal);
         }
      }
   }

   protected void visitGenericArgumentValues(List<ConstructorArgumentValues.ValueHolder> gas) {
      for (ConstructorArgumentValues.ValueHolder valueHolder : gas) {
         Object newVal = resolveValue(valueHolder.getValue());
         if (!ObjectUtils.nullSafeEquals(newVal, valueHolder.getValue())) {
            valueHolder.setValue(newVal);
         }
      }
   }

   @SuppressWarnings("rawtypes")
   @Nullable
   protected Object resolveValue(@Nullable Object value) {
      if (value instanceof ArtifactDefinition) {
         visitArtifactDefinition((ArtifactDefinition) value);
      }
      else if (value instanceof ArtifactDefinitionHolder) {
         visitArtifactDefinition(((ArtifactDefinitionHolder) value).getArtifactDefinition());
      }
      else if (value instanceof RuntimeArtifactReference) {
         RuntimeArtifactReference ref = (RuntimeArtifactReference) value;
         String newArtifactName = resolveStringValue(ref.getArtifactName());
         if (newArtifactName == null) {
            return null;
         }
         if (!newArtifactName.equals(ref.getArtifactName())) {
            return new RuntimeArtifactReference(newArtifactName);
         }
      }
      else if (value instanceof RuntimeArtifactNameReference) {
         RuntimeArtifactNameReference ref = (RuntimeArtifactNameReference) value;
         String newArtifactName = resolveStringValue(ref.getArtifactName());
         if (newArtifactName == null) {
            return null;
         }
         if (!newArtifactName.equals(ref.getArtifactName())) {
            return new RuntimeArtifactNameReference(newArtifactName);
         }
      }
      else if (value instanceof Object[]) {
         visitArray((Object[]) value);
      }
      else if (value instanceof List) {
         visitList((List) value);
      }
      else if (value instanceof Set) {
         visitSet((Set) value);
      }
      else if (value instanceof Map) {
         visitMap((Map) value);
      }
      else if (value instanceof TypedStringValue) {
         TypedStringValue typedStringValue = (TypedStringValue) value;
         String stringValue = typedStringValue.getValue();
         if (stringValue != null) {
            String visitedString = resolveStringValue(stringValue);
            typedStringValue.setValue(visitedString);
         }
      }
      else if (value instanceof String) {
         return resolveStringValue((String) value);
      }
      return value;
   }

   protected void visitArray(Object[] arrayVal) {
      for (int i = 0; i < arrayVal.length; i++) {
         Object elem = arrayVal[i];
         Object newVal = resolveValue(elem);
         if (!ObjectUtils.nullSafeEquals(newVal, elem)) {
            arrayVal[i] = newVal;
         }
      }
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   protected void visitList(List listVal) {
      for (int i = 0; i < listVal.size(); i++) {
         Object elem = listVal.get(i);
         Object newVal = resolveValue(elem);
         if (!ObjectUtils.nullSafeEquals(newVal, elem)) {
            listVal.set(i, newVal);
         }
      }
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   protected void visitSet(Set setVal) {
      Set newContent = new LinkedHashSet();
      boolean entriesModified = false;
      for (Object elem : setVal) {
         int elemHash = (elem != null ? elem.hashCode() : 0);
         Object newVal = resolveValue(elem);
         int newValHash = (newVal != null ? newVal.hashCode() : 0);
         newContent.add(newVal);
         entriesModified = entriesModified || (newVal != elem || newValHash != elemHash);
      }
      if (entriesModified) {
         setVal.clear();
         setVal.addAll(newContent);
      }
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   protected void visitMap(Map<?, ?> mapVal) {
      Map newContent = new LinkedHashMap();
      boolean entriesModified = false;
      for (Map.Entry entry : mapVal.entrySet()) {
         Object key = entry.getKey();
         int keyHash = (key != null ? key.hashCode() : 0);
         Object newKey = resolveValue(key);
         int newKeyHash = (newKey != null ? newKey.hashCode() : 0);
         Object val = entry.getValue();
         Object newVal = resolveValue(val);
         newContent.put(newKey, newVal);
         entriesModified = entriesModified || (newVal != val || newKey != key || newKeyHash != keyHash);
      }
      if (entriesModified) {
         mapVal.clear();
         mapVal.putAll(newContent);
      }
   }

   /**
    * Resolve the given String value, for example parsing placeholders.
    * @param strVal the original String value
    * @return the resolved String value
    */
   @Nullable
   protected String resolveStringValue(String strVal) {
      if (this.valueResolver == null) {
         throw new IllegalStateException("No StringValueResolver specified - pass a resolver " +
            "object into the constructor or override the 'resolveStringValue' method");
      }
      String resolvedValue = this.valueResolver.resolveStringValue(strVal);
      // Return original String if not modified.
      return (strVal.equals(resolvedValue) ? strVal : resolvedValue);
   }

}
