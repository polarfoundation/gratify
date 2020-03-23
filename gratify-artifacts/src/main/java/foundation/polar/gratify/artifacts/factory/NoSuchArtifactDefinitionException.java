package foundation.polar.gratify.artifacts.factory;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.core.ResolvableType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Exception thrown when a {@code ArtifactFactory} is asked for a bean instance for which it
 * cannot find a definition. This may point to a non-existing bean, a non-unique bean,
 * or a manually registered singleton instance without an associated bean definition.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see ArtifactFactory#getArtifact(String)
 * @see ArtifactFactory#getArtifact(Class)
 * @see NoUniqueArtifactDefinitionException
 */
@SuppressWarnings("serial")
public class NoSuchArtifactDefinitionException extends ArtifactsException {
   @Nullable
   private final String artifactName;

   @Nullable
   private final ResolvableType resolvableType;

   /**
    * Create a new {@code NoSuchArtifactDefinitionException}.
    * @param name the name of the missing bean
    */
   public NoSuchArtifactDefinitionException(String name) {
      super("No artifact named '" + name + "' available");
      this.artifactName = name;
      this.resolvableType = null;
   }

   /**
    * Create a new {@code NoSuchArtifactDefinitionException}.
    * @param name the name of the missing bean
    * @param message detailed message describing the problem
    */
   public NoSuchArtifactDefinitionException(String name, String message) {
      super("No artifact named '" + name + "' available: " + message);
      this.artifactName = name;
      this.resolvableType = null;
   }

   /**
    * Create a new {@code NoSuchArtifactDefinitionException}.
    * @param type required type of the missing bean
    */
   public NoSuchArtifactDefinitionException(Class<?> type) {
      this(ResolvableType.forClass(type));
   }

   /**
    * Create a new {@code NoSuchArtifactDefinitionException}.
    * @param type required type of the missing bean
    * @param message detailed message describing the problem
    */
   public NoSuchArtifactDefinitionException(Class<?> type, String message) {
      this(ResolvableType.forClass(type), message);
   }

   /**
    * Create a new {@code NoSuchArtifactDefinitionException}.
    * @param type full type declaration of the missing bean
    */
   public NoSuchArtifactDefinitionException(ResolvableType type) {
      super("No qualifying bean of type '" + type + "' available");
      this.artifactName = null;
      this.resolvableType = type;
   }

   /**
    * Create a new {@code NoSuchArtifactDefinitionException}.
    * @param type full type declaration of the missing bean
    * @param message detailed message describing the problem
    */
   public NoSuchArtifactDefinitionException(ResolvableType type, String message) {
      super("No qualifying bean of type '" + type + "' available: " + message);
      this.artifactName = null;
      this.resolvableType = type;
   }

   /**
    * Return the name of the missing bean, if it was a lookup <em>by name</em> that failed.
    */
   @Nullable
   public String getArtifactName() {
      return this.artifactName;
   }

   /**
    * Return the required type of the missing bean, if it was a lookup <em>by type</em>
    * that failed.
    */
   @Nullable
   public Class<?> getArtifactType() {
      return (this.resolvableType != null ? this.resolvableType.resolve() : null);
   }

   /**
    * Return the required {@link ResolvableType} of the missing bean, if it was a lookup
    * <em>by type</em> that failed.
    */
   @Nullable
   public ResolvableType getResolvableType() {
      return this.resolvableType;
   }

   /**
    * Return the number of beans found when only one matching bean was expected.
    * For a regular NoSuchArtifactDefinitionException, this will always be 0.
    * @see NoUniqueArtifactDefinitionException
    */
   public int getNumberOfArtifactsFound() {
      return 0;
   }
}
