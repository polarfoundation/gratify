package foundation.polar.gratify.artifacts.factory;

import foundation.polar.gratify.artifacts.FatalArtifactException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Exception thrown when the ArtifactFactory cannot load the specified class
 * of a given bean.
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class CannotLoadArtifactClassException extends FatalArtifactException {
   @Nullable
   private final String resourceDescription;

   private final String artifactName;

   @Nullable
   private final String artifactClassName;


   /**
    * Create a new CannotLoadArtifactClassException.
    * @param resourceDescription description of the resource
    * that the bean definition came from
    * @param artifactName the name of the bean requested
    * @param artifactClassName the name of the bean class
    * @param cause the root cause
    */
   public CannotLoadArtifactClassException(@Nullable String resourceDescription, String artifactName,
                                       @Nullable String artifactClassName, ClassNotFoundException cause) {

      super("Cannot find class [" + artifactClassName + "] for bean with name '" + artifactName + "'" +
         (resourceDescription != null ? " defined in " + resourceDescription : ""), cause);
      this.resourceDescription = resourceDescription;
      this.artifactName = artifactName;
      this.artifactClassName = artifactClassName;
   }

   /**
    * Create a new CannotLoadArtifactClassException.
    * @param resourceDescription description of the resource
    * that the bean definition came from
    * @param artifactName the name of the bean requested
    * @param artifactClassName the name of the bean class
    * @param cause the root cause
    */
   public CannotLoadArtifactClassException(@Nullable String resourceDescription, String artifactName,
                                       @Nullable String artifactClassName, LinkageError cause) {

      super("Error loading class [" + artifactClassName + "] for bean with name '" + artifactName + "'" +
         (resourceDescription != null ? " defined in " + resourceDescription : "") +
         ": problem with class file or dependent class", cause);
      this.resourceDescription = resourceDescription;
      this.artifactName = artifactName;
      this.artifactClassName = artifactClassName;
   }


   /**
    * Return the description of the resource that the bean
    * definition came from.
    */
   @Nullable
   public String getResourceDescription() {
      return this.resourceDescription;
   }

   /**
    * Return the name of the bean requested.
    */
   public String getArtifactName() {
      return this.artifactName;
   }

   /**
    * Return the name of the class we were trying to load.
    */
   @Nullable
   public String getArtifactClassName() {
      return this.artifactClassName;
   }
}
