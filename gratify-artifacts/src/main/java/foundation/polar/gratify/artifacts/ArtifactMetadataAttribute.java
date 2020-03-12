package foundation.polar.gratify.artifacts;


import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Holder for a key-value style attribute that is part of a bean definition.
 * Keeps track of the definition source in addition to the key-value pair.
 *
 * @author Juergen Hoeller
 */
public class ArtifactMetadataAttribute implements ArtifactMetadataElement {
   private final String name;

   @Nullable
   private final Object value;

   @Nullable
   private Object source;


   /**
    * Create a new AttributeValue instance.
    * @param name the name of the attribute (never {@code null})
    * @param value the value of the attribute (possibly before type conversion)
    */
   public ArtifactMetadataAttribute(String name, @Nullable Object value) {
      AssertUtils.notNull(name, "Name must not be null");
      this.name = name;
      this.value = value;
   }


   /**
    * Return the name of the attribute.
    */
   public String getName() {
      return this.name;
   }

   /**
    * Return the value of the attribute.
    */
   @Nullable
   public Object getValue() {
      return this.value;
   }

   /**
    * Set the configuration source {@code Object} for this metadata element.
    * <p>The exact type of the object will depend on the configuration mechanism used.
    */
   public void setSource(@Nullable Object source) {
      this.source = source;
   }

   @Override
   @Nullable
   public Object getSource() {
      return this.source;
   }


   @Override
   public boolean equals(@Nullable Object other) {
      if (this == other) {
         return true;
      }
      if (!(other instanceof ArtifactMetadataAttribute)) {
         return false;
      }
      ArtifactMetadataAttribute otherMa = (ArtifactMetadataAttribute) other;
      return (this.name.equals(otherMa.name) &&
         ObjectUtils.nullSafeEquals(this.value, otherMa.value) &&
         ObjectUtils.nullSafeEquals(this.source, otherMa.source));
   }

   @Override
   public int hashCode() {
      return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
   }

   @Override
   public String toString() {
      return "metadata attribute '" + this.name + "'";
   }
}
