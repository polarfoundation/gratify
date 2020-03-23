package foundation.polar.gratify.artifacts;

/**
 * Interface to be implemented by bean metadata elements
 * that carry a configuration source object.
 *
 * @author Juergen Hoeller
 */
public interface ArtifactMetadataElement {
   /**
    * Return the configuration source {@code Object} for this metadata element
    * (may be {@code null}).
    */
   default Object getSource() {
      return null;
   }
}
