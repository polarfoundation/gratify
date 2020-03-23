package foundation.polar.gratify.artifacts.factory.parsing;

import foundation.polar.gratify.artifacts.ArtifactMetadataElement;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.artifacts.factory.config.ArtifactReference;

/**
 * Interface that describes the logical view of a set of {@link ArtifactDefinition ArtifactDefinitions}
 * and {@link ArtifactReference ArtifactReferences} as presented in some configuration context.
 *
 * <p>With the introduction of {@link foundation.polar.gratify.artifacts.factory.xml.NamespaceHandler pluggable custom XML tags},
 * it is now possible for a single logical configuration entity, in this case an XML tag, to
 * create multiple {@link ArtifactDefinition ArtifactDefinitions} and {@link ArtifactReference RuntimeArtifactReferences}
 * in order to provide more succinct configuration and greater convenience to end users. As such, it can
 * no longer be assumed that each configuration entity (e.g. XML tag) maps to one {@link ArtifactDefinition}.
 * For tool vendors and other users who wish to present visualization or support for configuring Gratify
 * applications it is important that there is some mechanism in place to tie the {@link ArtifactDefinition ArtifactDefinitions}
 * in the {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory} back to the configuration data in a way
 * that has concrete meaning to the end user. As such, {@link foundation.polar.gratify.artifacts.factory.xml.NamespaceHandler}
 * implementations are able to publish events in the form of a {@code ComponentDefinition} for each
 * logical entity being configured. Third parties can then {@link ReaderEventListener subscribe to these events},
 * allowing for a user-centric view of the bean metadata.
 *
 * <p>Each {@code ComponentDefinition} has a {@link #getSource source object} which is configuration-specific.
 * In the case of XML-based configuration this is typically the {@link org.w3c.dom.Node} which contains the user
 * supplied configuration information. In addition to this, each {@link ArtifactDefinition} enclosed in a
 * {@code ComponentDefinition} has its own {@link ArtifactDefinition#getSource() source object} which may point
 * to a different, more specific, set of configuration data. Beyond this, individual pieces of bean metadata such
 * as the {@link foundation.polar.gratify.artifacts.PropertyValue PropertyValues} may also have a source object giving an
 * even greater level of detail. Source object extraction is handled through the
 * {@link SourceExtractor} which can be customized as required.
 *
 * <p>Whilst direct access to important {@link ArtifactReference ArtifactReferences} is provided through
 * {@link #getArtifactReferences}, tools may wish to inspect all {@link ArtifactDefinition ArtifactDefinitions} to gather
 * the full set of {@link ArtifactReference ArtifactReferences}. Implementations are required to provide
 * all {@link ArtifactReference ArtifactReferences} that are required to validate the configuration of the
 * overall logical entity as well as those required to provide full user visualisation of the configuration.
 * It is expected that certain {@link ArtifactReference ArtifactReferences} will not be important to
 * validation or to the user view of the configuration and as such these may be omitted. A tool may wish to
 * display any additional {@link ArtifactReference ArtifactReferences} sourced through the supplied
 * {@link ArtifactDefinition ArtifactDefinitions} but this is not considered to be a typical case.
 *
 * <p>Tools can determine the important of contained {@link ArtifactDefinition ArtifactDefinitions} by checking the
 * {@link ArtifactDefinition#getRole role identifier}. The role is essentially a hint to the tool as to how
 * important the configuration provider believes a {@link ArtifactDefinition} is to the end user. It is expected
 * that tools will <strong>not</strong> display all {@link ArtifactDefinition ArtifactDefinitions} for a given
 * {@code ComponentDefinition} choosing instead to filter based on the role. Tools may choose to make
 * this filtering user configurable. Particular notice should be given to the
 * {@link ArtifactDefinition#ROLE_INFRASTRUCTURE INFRASTRUCTURE role identifier}. {@link ArtifactDefinition ArtifactDefinitions}
 * classified with this role are completely unimportant to the end user and are required only for
 * internal implementation reasons.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 *
 * @see AbstractComponentDefinition
 * @see CompositeComponentDefinition
 * @see ArtifactComponentDefinition
 * @see ReaderEventListener#componentRegistered(ComponentDefinition)
 */
public interface ComponentDefinition extends ArtifactMetadataElement {
   /**
    * Get the user-visible name of this {@code ComponentDefinition}.
    * <p>This should link back directly to the corresponding configuration data
    * for this component in a given context.
    */
   String getName();

   /**
    * Return a friendly description of the described component.
    * <p>Implementations are encouraged to return the same value from
    * {@code toString()}.
    */
   String getDescription();

   /**
    * Return the {@link ArtifactDefinition ArtifactDefinitions} that were registered
    * to form this {@code ComponentDefinition}.
    * <p>It should be noted that a {@code ComponentDefinition} may well be related with
    * other {@link ArtifactDefinition ArtifactDefinitions} via {@link ArtifactReference references},
    * however these are <strong>not</strong> included as they may be not available immediately.
    * Important {@link ArtifactReference ArtifactReferences} are available from {@link #getArtifactReferences()}.
    * @return the array of ArtifactDefinitions, or an empty array if none
    */
   ArtifactDefinition[] getArtifactDefinitions();

   /**
    * Return the {@link ArtifactDefinition ArtifactDefinitions} that represent all relevant
    * inner beans within this component.
    * <p>Other inner beans may exist within the associated {@link ArtifactDefinition ArtifactDefinitions},
    * however these are not considered to be needed for validation or for user visualization.
    * @return the array of ArtifactDefinitions, or an empty array if none
    */
   ArtifactDefinition[] getInnerArtifactDefinitions();

   /**
    * Return the set of {@link ArtifactReference ArtifactReferences} that are considered
    * to be important to this {@code ComponentDefinition}.
    * <p>Other {@link ArtifactReference ArtifactReferences} may exist within the associated
    * {@link ArtifactDefinition ArtifactDefinitions}, however these are not considered
    * to be needed for validation or for user visualization.
    * @return the array of ArtifactReferences, or an empty array if none
    */
   ArtifactReference[] getArtifactReferences();
}
