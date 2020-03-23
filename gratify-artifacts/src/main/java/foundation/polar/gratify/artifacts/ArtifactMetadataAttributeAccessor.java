package foundation.polar.gratify.artifacts;

import foundation.polar.gratify.core.AttributeAccessorSupport;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Extension of {@link foundation.polar.gratify.core.AttributeAccessorSupport},
 * holding attributes as {@link ArtifactMetadataAttribute} objects in order
 * to keep track of the definition source.
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ArtifactMetadataAttributeAccessor extends AttributeAccessorSupport implements ArtifactMetadataElement {

   @Nullable
   private Object source;


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


   /**
    * Add the given ArtifactMetadataAttribute to this accessor's set of attributes.
    * @param attribute the ArtifactMetadataAttribute object to register
    */
   public void addMetadataAttribute(ArtifactMetadataAttribute attribute) {
      super.setAttribute(attribute.getName(), attribute);
   }

   /**
    * Look up the given ArtifactMetadataAttribute in this accessor's set of attributes.
    * @param name the name of the attribute
    * @return the corresponding ArtifactMetadataAttribute object,
    * or {@code null} if no such attribute defined
    */
   @Nullable
   public ArtifactMetadataAttribute getMetadataAttribute(String name) {
      return (ArtifactMetadataAttribute) super.getAttribute(name);
   }

   @Override
   public void setAttribute(String name, @Nullable Object value) {
      super.setAttribute(name, new ArtifactMetadataAttribute(name, value));
   }

   @Override
   @Nullable
   public Object getAttribute(String name) {
      ArtifactMetadataAttribute attribute = (ArtifactMetadataAttribute) super.getAttribute(name);
      return (attribute != null ? attribute.getValue() : null);
   }

   @Override
   @Nullable
   public Object removeAttribute(String name) {
      ArtifactMetadataAttribute attribute = (ArtifactMetadataAttribute) super.removeAttribute(name);
      return (attribute != null ? attribute.getValue() : null);
   }

}