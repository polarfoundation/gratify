package foundation.polar.gratify.artifacts.factory.support;


import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.utils.ObjectUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * GenericArtifactDefinition is a one-stop shop for standard bean definition purposes.
 * Like any bean definition, it allows for specifying a class plus optionally
 * constructor argument values and property values. Additionally, deriving from a
 * parent bean definition can be flexibly configured through the "parentName" property.
 *
 * <p>In general, use this {@code GenericArtifactDefinition} class for the purpose of
 * registering user-visible bean definitions (which a post-processor might operate on,
 * potentially even reconfiguring the parent name). Use {@code RootArtifactDefinition} /
 * {@code ChildArtifactDefinition} where parent/child relationships happen to be pre-determined.
 *
 * @author Juergen Hoeller
 *
 * @see #setParentName
 * @see RootArtifactDefinition
 * @see ChildArtifactDefinition
 */
@SuppressWarnings("serial")
public class GenericArtifactDefinition extends AbstractArtifactDefinition {

   @Nullable
   private String parentName;
   
   /**
    * Create a new GenericArtifactDefinition, to be configured through its bean
    * properties and configuration methods.
    * @see #setArtifactClass
    * @see #setScope
    * @see #setConstructorArgumentValues
    * @see #setPropertyValues
    */
   public GenericArtifactDefinition() {
      super();
   }

   /**
    * Create a new GenericArtifactDefinition as deep copy of the given
    * bean definition.
    * @param original the original bean definition to copy from
    */
   public GenericArtifactDefinition(ArtifactDefinition original) {
      super(original);
   }

   @Override
   public void setParentName(@Nullable String parentName) {
      this.parentName = parentName;
   }

   @Override
   @Nullable
   public String getParentName() {
      return this.parentName;
   }

   @Override
   public AbstractArtifactDefinition cloneArtifactDefinition() {
      return new GenericArtifactDefinition(this);
   }

   @Override
   public boolean equals(@Nullable Object other) {
      if (this == other) {
         return true;
      }
      if (!(other instanceof GenericArtifactDefinition)) {
         return false;
      }
      GenericArtifactDefinition that = (GenericArtifactDefinition) other;
      return (ObjectUtils.nullSafeEquals(this.parentName, that.parentName) && super.equals(other));
   }

   @Override
   public String toString() {
      if (this.parentName != null) {
         return "Generic bean with parent '" + this.parentName + "': " + super.toString();
      }
      return "Generic bean: " + super.toString();
   }
}
