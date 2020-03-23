package foundation.polar.gratify.artifacts.factory.config;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;

/**
 * Simple marker class for an individually autowired property value, to be added
 * to {@link ArtifactDefinition#getPropertyValues()} for a specific bean property.
 *
 * <p>At runtime, this will be replaced with a {@link DependencyDescriptor}
 * for the corresponding bean property's write method, eventually to be resolved
 * through a {@link AutowireCapableArtifactFactory#resolveDependency} step.
 *
 * @author Juergen Hoeller
 *
 * @see AutowireCapableArtifactFactory#resolveDependency
 * @see ArtifactDefinition#getPropertyValues()
 * @see foundation.polar.gratify.artifacts.factory.support.ArtifactDefinitionBuilder#addAutowiredProperty
 */
@SuppressWarnings("serial")
public final class AutowiredPropertyMarker implements Serializable {

   /**
    * The canonical instance for the autowired marker value.
    */
   public static final Object INSTANCE = new AutowiredPropertyMarker();

   private AutowiredPropertyMarker() {
   }

   private Object readResolve() {
      return INSTANCE;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      return (this == obj);
   }

   @Override
   public int hashCode() {
      return AutowiredPropertyMarker.class.hashCode();
   }

   @Override
   public String toString() {
      return "(autowired)";
   }

}
