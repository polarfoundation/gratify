package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.factory.ArtifactDefinitionStoreException;
import foundation.polar.gratify.artifacts.factory.NoSuchArtifactDefinitionException;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.core.AliasRegistry;

/**
 * Interface for registries that hold bean definitions, for example RootArtifactDefinition
 * and ChildArtifactDefinition instances. Typically implemented by ArtifactFactories that
 * internally work with the AbstractArtifactDefinition hierarchy.
 *
 * <p>This is the only interface in Gratify's bean factory packages that encapsulates
 * <i>registration</i> of bean definitions. The standard ArtifactFactory interfaces
 * only cover access to a <i>fully configured factory instance</i>.
 *
 * <p>Gratify's bean definition readers expect to work on an implementation of this
 * interface. Known implementors within the Gratify core are DefaultListableArtifactFactory
 * and GenericApplicationContext.
 *
 * @author Juergen Hoeller
 *
 * @see foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition
 * @see AbstractArtifactDefinition
 * @see RootArtifactDefinition
 * @see ChildArtifactDefinition
 * @see DefaultListableArtifactFactory
 * @see foundation.polar.gratify.context.support.GenericApplicationContext
 * @see foundation.polar.gratify.artifacts.factory.xml.XmlArtifactDefinitionReader
 * @see PropertiesArtifactDefinitionReader
 */
public interface ArtifactDefinitionRegistry extends AliasRegistry {
   /**
    * Register a new bean definition with this registry.
    * Must support RootArtifactDefinition and ChildArtifactDefinition.
    * @param artifactName the name of the bean instance to register
    * @param beanDefinition definition of the bean instance to register
    * @throws ArtifactDefinitionStoreException if the ArtifactDefinition is invalid
    * @throws ArtifactDefinitionOverrideException if there is already a ArtifactDefinition
    * for the specified bean name and we are not allowed to override it
    * @see GenericArtifactDefinition
    * @see RootArtifactDefinition
    * @see ChildArtifactDefinition
    */
   void registerArtifactDefinition(String artifactName, ArtifactDefinition beanDefinition)
      throws ArtifactDefinitionStoreException;

   /**
    * Remove the ArtifactDefinition for the given name.
    * @param artifactName the name of the bean instance to register
    * @throws NoSuchArtifactDefinitionException if there is no such bean definition
    */
   void removeArtifactDefinition(String artifactName) throws NoSuchArtifactDefinitionException;

   /**
    * Return the ArtifactDefinition for the given bean name.
    * @param artifactName name of the bean to find a definition for
    * @return the ArtifactDefinition for the given name (never {@code null})
    * @throws NoSuchArtifactDefinitionException if there is no such bean definition
    */
   ArtifactDefinition getArtifactDefinition(String artifactName) throws NoSuchArtifactDefinitionException;

   /**
    * Check if this registry contains a bean definition with the given name.
    * @param artifactName the name of the bean to look for
    * @return if this registry contains a bean definition with the given name
    */
   boolean containsArtifactDefinition(String artifactName);

   /**
    * Return the names of all beans defined in this registry.
    * @return the names of all beans defined in this registry,
    * or an empty array if none defined
    */
   String[] getArtifactDefinitionNames();

   /**
    * Return the number of beans defined in the registry.
    * @return the number of beans defined in the registry
    */
   int getArtifactDefinitionCount();

   /**
    * Determine whether the given bean name is already in use within this registry,
    * i.e. whether there is a local bean or alias registered under this name.
    * @param artifactName the name to check
    * @return whether the given bean name is already in use
    */
   boolean isArtifactNameInUse(String artifactName);
}
