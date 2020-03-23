package foundation.polar.gratify.artifacts;

import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;

/**
 * Object to hold information and value for an individual bean property.
 * Using an object here, rather than just storing all properties in
 * a map keyed by property name, allows for more flexibility, and the
 * ability to handle indexed properties etc in an optimized way.
 *
 * <p>Note that the value doesn't need to be the final required type:
 * A {@link ArtifactWrapper} implementation should handle any necessary conversion,
 * as this object doesn't know anything about the objects it will be applied to.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see PropertyValues
 * @see ArtifactWrapper
 */
@SuppressWarnings("serial")
public class PropertyValue extends ArtifactMetadataAttributeAccessor implements Serializable {

   private final String name;

   @Nullable
   private final Object value;

   private boolean optional = false;

   private boolean converted = false;

   @Nullable
   private Object convertedValue;

   /** Package-visible field that indicates whether conversion is necessary. */
   @Nullable
   volatile Boolean conversionNecessary;

   /** Package-visible field for caching the resolved property path tokens. */
   @Nullable
   transient volatile Object resolvedTokens;

   /**
    * Create a new PropertyValue instance.
    * @param name the name of the property (never {@code null})
    * @param value the value of the property (possibly before type conversion)
    */
   public PropertyValue(String name, @Nullable Object value) {
      AssertUtils.notNull(name, "Name must not be null");
      this.name = name;
      this.value = value;
   }

   /**
    * Copy constructor.
    * @param original the PropertyValue to copy (never {@code null})
    */
   public PropertyValue(PropertyValue original) {
      AssertUtils.notNull(original, "Original must not be null");
      this.name = original.getName();
      this.value = original.getValue();
      this.optional = original.isOptional();
      this.converted = original.converted;
      this.convertedValue = original.convertedValue;
      this.conversionNecessary = original.conversionNecessary;
      this.resolvedTokens = original.resolvedTokens;
      setSource(original.getSource());
      copyAttributesFrom(original);
   }

   /**
    * Constructor that exposes a new value for an original value holder.
    * The original holder will be exposed as source of the new holder.
    * @param original the PropertyValue to link to (never {@code null})
    * @param newValue the new value to apply
    */
   public PropertyValue(PropertyValue original, @Nullable Object newValue) {
      AssertUtils.notNull(original, "Original must not be null");
      this.name = original.getName();
      this.value = newValue;
      this.optional = original.isOptional();
      this.conversionNecessary = original.conversionNecessary;
      this.resolvedTokens = original.resolvedTokens;
      setSource(original);
      copyAttributesFrom(original);
   }

   /**
    * Return the name of the property.
    */
   public String getName() {
      return this.name;
   }

   /**
    * Return the value of the property.
    * <p>Note that type conversion will <i>not</i> have occurred here.
    * It is the responsibility of the ArtifactWrapper implementation to
    * perform type conversion.
    */
   @Nullable
   public Object getValue() {
      return this.value;
   }

   /**
    * Return the original PropertyValue instance for this value holder.
    * @return the original PropertyValue (either a source of this
    * value holder or this value holder itself).
    */
   public PropertyValue getOriginalPropertyValue() {
      PropertyValue original = this;
      Object source = getSource();
      while (source instanceof PropertyValue && source != original) {
         original = (PropertyValue) source;
         source = original.getSource();
      }
      return original;
   }

   /**
    * Set whether this is an optional value, that is, to be ignored
    * when no corresponding property exists on the target class.
    */
   public void setOptional(boolean optional) {
      this.optional = optional;
   }

   /**
    * Return whether this is an optional value, that is, to be ignored
    * when no corresponding property exists on the target class.
    */
   public boolean isOptional() {
      return this.optional;
   }

   /**
    * Return whether this holder contains a converted value already ({@code true}),
    * or whether the value still needs to be converted ({@code false}).
    */
   public synchronized boolean isConverted() {
      return this.converted;
   }

   /**
    * Set the converted value of this property value,
    * after processed type conversion.
    */
   public synchronized void setConvertedValue(@Nullable Object value) {
      this.converted = true;
      this.convertedValue = value;
   }

   /**
    * Return the converted value of this property value,
    * after processed type conversion.
    */
   @Nullable
   public synchronized Object getConvertedValue() {
      return this.convertedValue;
   }

   @Override
   public boolean equals(@Nullable Object other) {
      if (this == other) {
         return true;
      }
      if (!(other instanceof PropertyValue)) {
         return false;
      }
      PropertyValue otherPv = (PropertyValue) other;
      return (this.name.equals(otherPv.name) &&
         ObjectUtils.nullSafeEquals(this.value, otherPv.value) &&
         ObjectUtils.nullSafeEquals(getSource(), otherPv.getSource()));
   }

   @Override
   public int hashCode() {
      return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
   }

   @Override
   public String toString() {
      return "bean property '" + this.name + "'";
   }

}
