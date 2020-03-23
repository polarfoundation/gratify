package foundation.polar.gratify.artifacts.factory.parsing;

import foundation.polar.gratify.artifacts.ArtifactMetadataElement;

/**
 * Marker interface for a defaults definition,
 * extending ArtifactMetadataElement to inherit source exposure.
 *
 * <p>Concrete implementations are typically based on 'document defaults',
 * for example specified at the root tag level within an XML document.
 *
 * @author Juergen Hoeller
 *
 * @see foundation.polar.gratify.artifacts.factory.xml.DocumentDefaultsDefinition
 * @see ReaderEventListener#defaultsRegistered(DefaultsDefinition)
 */
public interface DefaultsDefinition extends ArtifactMetadataElement {
}
