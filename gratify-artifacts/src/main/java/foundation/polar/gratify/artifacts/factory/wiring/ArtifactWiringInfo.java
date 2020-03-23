package foundation.polar.gratify.artifacts.factory.wiring;

import foundation.polar.gratify.artifacts.factory.config.AutowireCapableArtifactFactory;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Holder for bean wiring metadata information about a particular class. Used in
 * conjunction with the {@link foundation.polar.gratify.artifacts.factory.annotation.Configurable}
 * annotation and the AspectJ {@code AnnotationArtifactConfigurerAspect}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 *
 * @see ArtifactWiringInfoResolver
 * @see foundation.polar.gratify.artifacts.factory.config.AutowireCapableArtifactFactory
 * @see foundation.polar.gratify.artifacts.factory.annotation.Configurable
 */
public class ArtifactWiringInfo {
   /**
    * Constant that indicates autowiring bean properties by name.
    * @see #ArtifactWiringInfo(int, boolean)
    * @see foundation.polar.gratify.artifacts.factory.config.AutowireCapableArtifactFactory#AUTOWIRE_BY_NAME
    */
   public static final int AUTOWIRE_BY_NAME = AutowireCapableArtifactFactory.AUTOWIRE_BY_NAME;

   /**
    * Constant that indicates autowiring bean properties by type.
    * @see #ArtifactWiringInfo(int, boolean)
    * @see foundation.polar.gratify.artifacts.factory.config.AutowireCapableArtifactFactory#AUTOWIRE_BY_TYPE
    */
   public static final int AUTOWIRE_BY_TYPE = AutowireCapableArtifactFactory.AUTOWIRE_BY_TYPE;

   @Nullable
   private String artifactName;

   private boolean isDefaultArtifactName = false;

   private int autowireMode = AutowireCapableArtifactFactory.AUTOWIRE_NO;

   private boolean dependencyCheck = false;


   /**
    * Create a default ArtifactWiringInfo that suggests plain initialization of
    * factory and post-processor callbacks that the bean class may expect.
    */
   public ArtifactWiringInfo() {
   }

   /**
    * Create a new ArtifactWiringInfo that points to the given bean name.
    * @param artifactName the name of the bean definition to take the property values from
    * @throws IllegalArgumentException if the supplied artifactName is {@code null},
    * is empty, or consists wholly of whitespace
    */
   public ArtifactWiringInfo(String artifactName) {
      this(artifactName, false);
   }

   /**
    * Create a new ArtifactWiringInfo that points to the given bean name.
    * @param artifactName the name of the bean definition to take the property values from
    * @param isDefaultArtifactName whether the given bean name is a suggested
    * default bean name, not necessarily matching an actual bean definition
    * @throws IllegalArgumentException if the supplied artifactName is {@code null},
    * is empty, or consists wholly of whitespace
    */
   public ArtifactWiringInfo(String artifactName, boolean isDefaultArtifactName) {
      AssertUtils.hasText(artifactName, "'artifactName' must not be empty");
      this.artifactName = artifactName;
      this.isDefaultArtifactName = isDefaultArtifactName;
   }

   /**
    * Create a new ArtifactWiringInfo that indicates autowiring.
    * @param autowireMode one of the constants {@link #AUTOWIRE_BY_NAME} /
    * {@link #AUTOWIRE_BY_TYPE}
    * @param dependencyCheck whether to perform a dependency check for object
    * references in the bean instance (after autowiring)
    * @throws IllegalArgumentException if the supplied {@code autowireMode}
    * is not one of the allowed values
    * @see #AUTOWIRE_BY_NAME
    * @see #AUTOWIRE_BY_TYPE
    */
   public ArtifactWiringInfo(int autowireMode, boolean dependencyCheck) {
      if (autowireMode != AUTOWIRE_BY_NAME && autowireMode != AUTOWIRE_BY_TYPE) {
         throw new IllegalArgumentException("Only constants AUTOWIRE_BY_NAME and AUTOWIRE_BY_TYPE supported");
      }
      this.autowireMode = autowireMode;
      this.dependencyCheck = dependencyCheck;
   }
   
   /**
    * Return whether this ArtifactWiringInfo indicates autowiring.
    */
   public boolean indicatesAutowiring() {
      return (this.artifactName == null);
   }

   /**
    * Return the specific bean name that this ArtifactWiringInfo points to, if any.
    */
   @Nullable
   public String getArtifactName() {
      return this.artifactName;
   }

   /**
    * Return whether the specific bean name is a suggested default bean name,
    * not necessarily matching an actual bean definition in the factory.
    */
   public boolean isDefaultArtifactName() {
      return this.isDefaultArtifactName;
   }

   /**
    * Return one of the constants {@link #AUTOWIRE_BY_NAME} /
    * {@link #AUTOWIRE_BY_TYPE}, if autowiring is indicated.
    */
   public int getAutowireMode() {
      return this.autowireMode;
   }

   /**
    * Return whether to perform a dependency check for object references
    * in the bean instance (after autowiring).
    */
   public boolean getDependencyCheck() {
      return this.dependencyCheck;
   }
}
