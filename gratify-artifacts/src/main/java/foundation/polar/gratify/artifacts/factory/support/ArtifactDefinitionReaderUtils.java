package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.factory.ArtifactDefinitionStoreException;
import foundation.polar.gratify.artifacts.factory.ArtifactFactoryUtils;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinitionHolder;
import foundation.polar.gratify.utils.ClassUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility methods that are useful for artifact definition reader implementations.
 * Mainly intended for internal use.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see PropertiesArtifactDefinitionReader
 * @see foundation.polar.gratify.artifacts.factory.xml.DefaultArtifactDefinitionDocumentReader
 */
public abstract class ArtifactDefinitionReaderUtils {
   /**
    * Separator for generated artifact names. If a class name or parent name is not
    * unique, "#1", "#2" etc will be appended, until the name becomes unique.
    */
   public static final String GENERATED_BEAN_NAME_SEPARATOR = ArtifactFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;


   /**
    * Create a new GenericArtifactDefinition for the given parent name and class name,
    * eagerly loading the artifact class if a ClassLoader has been specified.
    * @param parentName the name of the parent artifact, if any
    * @param className the name of the artifact class, if any
    * @param classLoader the ClassLoader to use for loading artifact classes
    * (can be {@code null} to just register artifact classes by name)
    * @return the artifact definition
    * @throws ClassNotFoundException if the artifact class could not be loaded
    */
   public static AbstractArtifactDefinition createArtifactDefinition(
      @Nullable String parentName, @Nullable String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {

      GenericArtifactDefinition bd = new GenericArtifactDefinition();
      bd.setParentName(parentName);
      if (className != null) {
         if (classLoader != null) {
            bd.setArtifactClass(ClassUtils.forName(className, classLoader));
         }
         else {
            bd.setArtifactClassName(className);
         }
      }
      return bd;
   }

   /**
    * Generate a artifact name for the given top-level artifact definition,
    * unique within the given artifact factory.
    * @param artifactDefinition the artifact definition to generate a artifact name for
    * @param registry the artifact factory that the definition is going to be
    * registered with (to check for existing artifact names)
    * @return the generated artifact name
    * @throws ArtifactDefinitionStoreException if no unique name can be generated
    * for the given artifact definition
    * @see #generateArtifactName(ArtifactDefinition, ArtifactDefinitionRegistry, boolean)
    */
   public static String generateArtifactName(ArtifactDefinition artifactDefinition, ArtifactDefinitionRegistry registry)
      throws ArtifactDefinitionStoreException {

      return generateArtifactName(artifactDefinition, registry, false);
   }

   /**
    * Generate a artifact name for the given artifact definition, unique within the
    * given artifact factory.
    * @param definition the artifact definition to generate a artifact name for
    * @param registry the artifact factory that the definition is going to be
    * registered with (to check for existing artifact names)
    * @param isInnerArtifact whether the given artifact definition will be registered
    * as inner artifact or as top-level artifact (allowing for special name generation
    * for inner artifacts versus top-level artifacts)
    * @return the generated artifact name
    * @throws ArtifactDefinitionStoreException if no unique name can be generated
    * for the given artifact definition
    */
   public static String generateArtifactName(
      ArtifactDefinition definition, ArtifactDefinitionRegistry registry, boolean isInnerArtifact)
      throws ArtifactDefinitionStoreException {

      String generatedArtifactName = definition.getArtifactClassName();
      if (generatedArtifactName == null) {
         if (definition.getParentName() != null) {
            generatedArtifactName = definition.getParentName() + "$child";
         }
         else if (definition.getFactoryArtifactName() != null) {
            generatedArtifactName = definition.getFactoryArtifactName() + "$created";
         }
      }
      if (!StringUtils.hasText(generatedArtifactName)) {
         throw new ArtifactDefinitionStoreException("Unnamed artifact definition specifies neither " +
            "'class' nor 'parent' nor 'factory-artifact' - can't generate artifact name");
      }

      String id = generatedArtifactName;
      if (isInnerArtifact) {
         // Inner artifact: generate identity hashcode suffix.
         id = generatedArtifactName + GENERATED_BEAN_NAME_SEPARATOR + ObjectUtils.getIdentityHexString(definition);
      }
      else {
         // Top-level artifact: use plain class name with unique suffix if necessary.
         return uniqueArtifactName(generatedArtifactName, registry);
      }
      return id;
   }

   /**
    * Turn the given artifact name into a unique artifact name for the given artifact factory,
    * appending a unique counter as suffix if necessary.
    * @param artifactName the original artifact name
    * @param registry the artifact factory that the definition is going to be
    * registered with (to check for existing artifact names)
    * @return the unique artifact name to use
    */
   public static String uniqueArtifactName(String artifactName, ArtifactDefinitionRegistry registry) {
      String id = artifactName;
      int counter = -1;

      // Increase counter until the id is unique.
      String prefix = artifactName + GENERATED_BEAN_NAME_SEPARATOR;
      while (counter == -1 || registry.containsArtifactDefinition(id)) {
         counter++;
         id = prefix + counter;
      }
      return id;
   }

   /**
    * Register the given artifact definition with the given artifact factory.
    * @param definitionHolder the artifact definition including name and aliases
    * @param registry the artifact factory to register with
    * @throws ArtifactDefinitionStoreException if registration failed
    */
   public static void registerArtifactDefinition(
      ArtifactDefinitionHolder definitionHolder, ArtifactDefinitionRegistry registry)
      throws ArtifactDefinitionStoreException {

      // Register artifact definition under primary name.
      String artifactName = definitionHolder.getArtifactName();
      registry.registerArtifactDefinition(artifactName, definitionHolder.getArtifactDefinition());

      // Register aliases for artifact name, if any.
      String[] aliases = definitionHolder.getAliases();
      if (aliases != null) {
         for (String alias : aliases) {
            registry.registerAlias(artifactName, alias);
         }
      }
   }

   /**
    * Register the given artifact definition with a generated name,
    * unique within the given artifact factory.
    * @param definition the artifact definition to generate a artifact name for
    * @param registry the artifact factory to register with
    * @return the generated artifact name
    * @throws ArtifactDefinitionStoreException if no unique name can be generated
    * for the given artifact definition or the definition cannot be registered
    */
   public static String registerWithGeneratedName(
      AbstractArtifactDefinition definition, ArtifactDefinitionRegistry registry)
      throws ArtifactDefinitionStoreException {

      String generatedName = generateArtifactName(definition, registry, false);
      registry.registerArtifactDefinition(generatedName, definition);
      return generatedName;
   }
}
