package foundation.polar.gratify.artifacts.factory;

import foundation.polar.gratify.artifacts.FatalArtifactException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Exception thrown when a ArtifactFactory encounters an invalid bean definition:
 * e.g. in case of incomplete or contradictory bean metadata.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 */
@SuppressWarnings("serial")
public class ArtifactDefinitionStoreException extends FatalArtifactException {

   @Nullable
   private final String resourceDescription;

   @Nullable
   private final String artifactName;

   /**
    * Create a new ArtifactDefinitionStoreException.
    * @param msg the detail message (used as exception message as-is)
    */
   public ArtifactDefinitionStoreException(String msg) {
      super(msg);
      this.resourceDescription = null;
      this.artifactName = null;
   }

   /**
    * Create a new ArtifactDefinitionStoreException.
    * @param msg the detail message (used as exception message as-is)
    * @param cause the root cause (may be {@code null})
    */
   public ArtifactDefinitionStoreException(String msg, @Nullable Throwable cause) {
      super(msg, cause);
      this.resourceDescription = null;
      this.artifactName = null;
   }

   /**
    * Create a new ArtifactDefinitionStoreException.
    * @param resourceDescription description of the resource that the bean definition came from
    * @param msg the detail message (used as exception message as-is)
    */
   public ArtifactDefinitionStoreException(@Nullable String resourceDescription, String msg) {
      super(msg);
      this.resourceDescription = resourceDescription;
      this.artifactName = null;
   }

   /**
    * Create a new ArtifactDefinitionStoreException.
    * @param resourceDescription description of the resource that the bean definition came from
    * @param msg the detail message (used as exception message as-is)
    * @param cause the root cause (may be {@code null})
    */
   public ArtifactDefinitionStoreException(@Nullable String resourceDescription, String msg, @Nullable Throwable cause) {
      super(msg, cause);
      this.resourceDescription = resourceDescription;
      this.artifactName = null;
   }

   /**
    * Create a new ArtifactDefinitionStoreException.
    * @param resourceDescription description of the resource that the bean definition came from
    * @param artifactName the name of the bean
    * @param msg the detail message (appended to an introductory message that indicates
    * the resource and the name of the bean)
    */
   public ArtifactDefinitionStoreException(@Nullable String resourceDescription, String artifactName, String msg) {
      this(resourceDescription, artifactName, msg, null);
   }

   /**
    * Create a new ArtifactDefinitionStoreException.
    * @param resourceDescription description of the resource that the bean definition came from
    * @param artifactName the name of the bean
    * @param msg the detail message (appended to an introductory message that indicates
    * the resource and the name of the bean)
    * @param cause the root cause (may be {@code null})
    */
   public ArtifactDefinitionStoreException(
      @Nullable String resourceDescription, String artifactName, String msg, @Nullable Throwable cause) {

      super("Invalid bean definition with name '" + artifactName + "' defined in " + resourceDescription + ": " + msg,
         cause);
      this.resourceDescription = resourceDescription;
      this.artifactName = artifactName;
   }


   /**
    * Return the description of the resource that the bean definition came from, if available.
    */
   @Nullable
   public String getResourceDescription() {
      return this.resourceDescription;
   }

   /**
    * Return the name of the bean, if available.
    */
   @Nullable
   public String getArtifactName() {
      return this.artifactName;
   }
}
