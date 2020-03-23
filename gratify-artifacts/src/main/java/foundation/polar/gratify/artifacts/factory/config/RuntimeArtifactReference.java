package foundation.polar.gratify.artifacts.factory.config;


import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Immutable placeholder class used for a property value object when it's
 * a reference to another bean in the factory, to be resolved at runtime.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ArtifactDefinition#getPropertyValues()
 * @see foundation.polar.gratify.artifacts.factory.ArtifactFactory#getArtifact(String)
 * @see foundation.polar.gratify.artifacts.factory.ArtifactFactory#getArtifact(Class)
 */
public class RuntimeArtifactReference implements ArtifactReference {
   private final String artifactName;

   @Nullable
   private final Class<?> beanType;

   private final boolean toParent;

   @Nullable
   private Object source;


   /**
    * Create a new RuntimeArtifactReference to the given bean name.
    * @param artifactName name of the target bean
    */
   public RuntimeArtifactReference(String artifactName) {
      this(artifactName, false);
   }

   /**
    * Create a new RuntimeArtifactReference to the given bean name,
    * with the option to mark it as reference to a bean in the parent factory.
    * @param artifactName name of the target bean
    * @param toParent whether this is an explicit reference to a bean in the
    * parent factory
    */
   public RuntimeArtifactReference(String artifactName, boolean toParent) {
      AssertUtils.hasText(artifactName, "'artifactName' must not be empty");
      this.artifactName = artifactName;
      this.beanType = null;
      this.toParent = toParent;
   }

   /**
    * Create a new RuntimeArtifactReference to a bean of the given type.
    * @param beanType type of the target bean
    */
   public RuntimeArtifactReference(Class<?> beanType) {
      this(beanType, false);
   }

   /**
    * Create a new RuntimeArtifactReference to a bean of the given type,
    * with the option to mark it as reference to a bean in the parent factory.
    * @param beanType type of the target bean
    * @param toParent whether this is an explicit reference to a bean in the
    * parent factory
    */
   public RuntimeArtifactReference(Class<?> beanType, boolean toParent) {
      AssertUtils.notNull(beanType, "'beanType' must not be empty");
      this.artifactName = beanType.getName();
      this.beanType = beanType;
      this.toParent = toParent;
   }


   /**
    * Return the requested bean name, or the fully-qualified type name
    * in case of by-type resolution.
    * @see #getArtifactType()
    */
   @Override
   public String getArtifactName() {
      return this.artifactName;
   }

   /**
    * Return the requested bean type if resolution by type is demanded.
    */
   @Nullable
   public Class<?> getArtifactType() {
      return this.beanType;
   }

   /**
    * Return whether this is an explicit reference to a bean in the parent factory.
    */
   public boolean isToParent() {
      return this.toParent;
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
      if (!(other instanceof RuntimeArtifactReference)) {
         return false;
      }
      RuntimeArtifactReference that = (RuntimeArtifactReference) other;
      return (this.artifactName.equals(that.artifactName) && this.beanType == that.beanType &&
         this.toParent == that.toParent);
   }

   @Override
   public int hashCode() {
      int result = this.artifactName.hashCode();
      result = 29 * result + (this.toParent ? 1 : 0);
      return result;
   }

   @Override
   public String toString() {
      return '<' + getArtifactName() + '>';
   }
}
