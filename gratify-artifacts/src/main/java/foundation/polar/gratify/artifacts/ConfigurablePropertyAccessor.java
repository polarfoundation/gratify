package foundation.polar.gratify.artifacts;

import foundation.polar.gratify.core.convert.ConversionService;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface that encapsulates configuration methods for a PropertyAccessor.
 * Also extends the PropertyEditorRegistry interface, which defines methods
 * for PropertyEditor management.
 *
 * <p>Serves as base interface for {@link ArtifactWrapper}.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see ArtifactWrapper
 */
public interface ConfigurablePropertyAccessor extends PropertyAccessor, PropertyEditorRegistry, TypeConverter {
   /**
    * Specify a ConversionService to use for converting
    * property values, as an alternative to JavaBeans PropertyEditors.
    */
   void setConversionService(@Nullable ConversionService conversionService);

   /**
    * Return the associated ConversionService, if any.
    */
   @Nullable
   ConversionService getConversionService();

   /**
    * Set whether to extract the old property value when applying a
    * property editor to a new value for a property.
    */
   void setExtractOldValueForEditor(boolean extractOldValueForEditor);

   /**
    * Return whether to extract the old property value when applying a
    * property editor to a new value for a property.
    */
   boolean isExtractOldValueForEditor();

   /**
    * Set whether this instance should attempt to "auto-grow" a
    * nested path that contains a {@code null} value.
    * <p>If {@code true}, a {@code null} path location will be populated
    * with a default object value and traversed instead of resulting in a
    * {@link NullValueInNestedPathException}.
    * <p>Default is {@code false} on a plain PropertyAccessor instance.
    */
   void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

   /**
    * Return whether "auto-growing" of nested paths has been activated.
    */
   boolean isAutoGrowNestedPaths();
}
