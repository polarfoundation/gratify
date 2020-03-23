package foundation.polar.gratify.artifacts.factory.parsing;

public class ArtifactEntry {
   private String artifactDefinitionName;

   /**
    * Creates a new instance of {@link ArtifactEntry} class.
    * @param artifactDefinitionName the name of the associated artifact definition
    */
   public ArtifactEntry(String artifactDefinitionName) {
      this.artifactDefinitionName = artifactDefinitionName;
   }

   @Override
   public String toString() {
      return "Artifact '" + this.artifactDefinitionName + "'";
   }
}
