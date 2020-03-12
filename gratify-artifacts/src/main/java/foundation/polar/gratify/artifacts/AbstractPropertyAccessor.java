package foundation.polar.gratify.artifacts;


import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Abstract implementation of the {@link PropertyAccessor} interface.
 * Provides base implementations of all convenience methods, with the
 * implementation of actual property access left to subclasses.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 *
 * @see #getPropertyValue
 * @see #setPropertyValue
 */
public abstract class AbstractPropertyAccessor extends TypeConverterSupport implements ConfigurablePropertyAccessor {
   private boolean extractOldValueForEditor = false;

   private boolean autoGrowNestedPaths = false;


   @Override
   public void setExtractOldValueForEditor(boolean extractOldValueForEditor) {
      this.extractOldValueForEditor = extractOldValueForEditor;
   }

   @Override
   public boolean isExtractOldValueForEditor() {
      return this.extractOldValueForEditor;
   }

   @Override
   public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
      this.autoGrowNestedPaths = autoGrowNestedPaths;
   }

   @Override
   public boolean isAutoGrowNestedPaths() {
      return this.autoGrowNestedPaths;
   }


   @Override
   public void setPropertyValue(PropertyValue pv) throws ArtifactsException {
      setPropertyValue(pv.getName(), pv.getValue());
   }

   @Override
   public void setPropertyValues(Map<?, ?> map) throws ArtifactsException {
      setPropertyValues(new MutablePropertyValues(map));
   }

   @Override
   public void setPropertyValues(PropertyValues pvs) throws ArtifactsException {
      setPropertyValues(pvs, false, false);
   }

   @Override
   public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown) throws ArtifactsException {
      setPropertyValues(pvs, ignoreUnknown, false);
   }

   @Override
   public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid)
      throws ArtifactsException {

      List<PropertyAccessException> propertyAccessExceptions = null;
      List<PropertyValue> propertyValues = (pvs instanceof MutablePropertyValues ?
         ((MutablePropertyValues) pvs).getPropertyValueList() : Arrays.asList(pvs.getPropertyValues()));
      for (PropertyValue pv : propertyValues) {
         try {
            // This method may throw any ArtifactsException, which won't be caught
            // here, if there is a critical failure such as no matching field.
            // We can attempt to deal only with less serious exceptions.
            setPropertyValue(pv);
         }
         catch (NotWritablePropertyException ex) {
            if (!ignoreUnknown) {
               throw ex;
            }
            // Otherwise, just ignore it and continue...
         }
         catch (NullValueInNestedPathException ex) {
            if (!ignoreInvalid) {
               throw ex;
            }
            // Otherwise, just ignore it and continue...
         }
         catch (PropertyAccessException ex) {
            if (propertyAccessExceptions == null) {
               propertyAccessExceptions = new ArrayList<>();
            }
            propertyAccessExceptions.add(ex);
         }
      }

      // If we encountered individual exceptions, throw the composite exception.
      if (propertyAccessExceptions != null) {
         PropertyAccessException[] paeArray = propertyAccessExceptions.toArray(new PropertyAccessException[0]);
         throw new PropertyBatchUpdateException(paeArray);
      }
   }


   // Redefined with public visibility.
   @Override
   @Nullable
   public Class<?> getPropertyType(String propertyPath) {
      return null;
   }

   /**
    * Actually get the value of a property.
    * @param propertyName name of the property to get the value of
    * @return the value of the property
    * @throws InvalidPropertyException if there is no such property or
    * if the property isn't readable
    * @throws PropertyAccessException if the property was valid but the
    * accessor method failed
    */
   @Override
   @Nullable
   public abstract Object getPropertyValue(String propertyName) throws ArtifactsException;

   /**
    * Actually set a property value.
    * @param propertyName name of the property to set value of
    * @param value the new value
    * @throws InvalidPropertyException if there is no such property or
    * if the property isn't writable
    * @throws PropertyAccessException if the property was valid but the
    * accessor method failed or a type mismatch occurred
    */
   @Override
   public abstract void setPropertyValue(String propertyName, @Nullable Object value) throws ArtifactsException;
}
