package foundation.polar.gratify.artifacts.factory.config;


import foundation.polar.gratify.artifacts.ArtifactMetadataElement;

/**
 * Interface that exposes a reference to a bean name in an abstract fashion.
 * This interface does not necessarily imply a reference to an actual bean
 * instance; it just expresses a logical reference to the name of a bean.
 *
 * <p>Serves as common interface implemented by any kind of bean reference
 * holder, such as {@link RuntimeArtifactReference RuntimeArtifactReference} and
 * {@link RuntimeArtifactNameReference RuntimeArtifactNameReference}.
 *
 * @author Juergen Hoeller
 */
public interface ArtifactReference extends ArtifactMetadataElement {
   /**
    * Return the target bean name that this reference points to (never {@code null}).
    */
   String getArtifactName();
}
