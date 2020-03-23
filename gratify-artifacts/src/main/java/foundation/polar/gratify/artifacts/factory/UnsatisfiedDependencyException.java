package foundation.polar.gratify.artifacts.factory;


import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Exception thrown when a bean depends on other beans or simple properties
 * that were not specified in the bean factory definition, although
 * dependency checking was enabled.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class UnsatisfiedDependencyException extends ArtifactCreationException  {
   @Nullable
   private final InjectionPoint injectionPoint;

   /**
    * Create a new UnsatisfiedDependencyException.
    * @param resourceDescription description of the resource that the bean definition came from
    * @param artifactName the name of the bean requested
    * @param propertyName the name of the bean property that couldn't be satisfied
    * @param msg the detail message
    */
   public UnsatisfiedDependencyException(
      @Nullable String resourceDescription, @Nullable String artifactName, String propertyName, String msg) {

      super(resourceDescription, artifactName,
         "Unsatisfied dependency expressed through bean property '" + propertyName + "'" +
            (StringUtils.hasLength(msg) ? ": " + msg : ""));
      this.injectionPoint = null;
   }

   /**
    * Create a new UnsatisfiedDependencyException.
    * @param resourceDescription description of the resource that the bean definition came from
    * @param artifactName the name of the bean requested
    * @param propertyName the name of the bean property that couldn't be satisfied
    * @param ex the bean creation exception that indicated the unsatisfied dependency
    */
   public UnsatisfiedDependencyException(
      @Nullable String resourceDescription, @Nullable String artifactName, String propertyName, ArtifactsException ex) {

      this(resourceDescription, artifactName, propertyName, "");
      initCause(ex);
   }

   /**
    * Create a new UnsatisfiedDependencyException.
    * @param resourceDescription description of the resource that the bean definition came from
    * @param artifactName the name of the bean requested
    * @param injectionPoint the injection point (field or method/constructor parameter)
    * @param msg the detail message
    */
   public UnsatisfiedDependencyException(
      @Nullable String resourceDescription, @Nullable String artifactName, @Nullable InjectionPoint injectionPoint, String msg) {

      super(resourceDescription, artifactName,
         "Unsatisfied dependency expressed through " + injectionPoint +
            (StringUtils.hasLength(msg) ? ": " + msg : ""));
      this.injectionPoint = injectionPoint;
   }

   /**
    * Create a new UnsatisfiedDependencyException.
    * @param resourceDescription description of the resource that the bean definition came from
    * @param artifactName the name of the bean requested
    * @param injectionPoint the injection point (field or method/constructor parameter)
    * @param ex the bean creation exception that indicated the unsatisfied dependency
    */
   public UnsatisfiedDependencyException(
      @Nullable String resourceDescription, @Nullable String artifactName, @Nullable InjectionPoint injectionPoint, ArtifactsException ex) {

      this(resourceDescription, artifactName, injectionPoint, "");
      initCause(ex);
   }

   /**
    * Return the injection point (field or method/constructor parameter), if known.
    */
   @Nullable
   public InjectionPoint getInjectionPoint() {
      return this.injectionPoint;
   }
}
