package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RuntimeArtifactNameReference implements ArtifactReference {
   private final String artifactName;

   @Nullable
   private Object source;

   /**
    * Create a new RuntimeArtifactNameReference to the given bean name.
    * @param artifactName name of the target bean
    */
   public RuntimeArtifactNameReference(String artifactName) {
      AssertUtils.hasText(artifactName, "'artifactName' must not be empty");
      this.artifactName = artifactName;
   }

   @Override
   public String getArtifactName() {
      return this.artifactName;
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
      if (!(other instanceof RuntimeArtifactNameReference)) {
         return false;
      }
      RuntimeArtifactNameReference that = (RuntimeArtifactNameReference) other;
      return this.artifactName.equals(that.artifactName);
   }

   @Override
   public int hashCode() {
      return this.artifactName.hashCode();
   }

   @Override
   public String toString() {
      return '<' + getArtifactName() + '>';
   }
}
