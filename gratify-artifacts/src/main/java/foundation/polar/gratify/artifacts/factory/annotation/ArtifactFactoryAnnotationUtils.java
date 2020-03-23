package foundation.polar.gratify.artifacts.factory.annotation;

import foundation.polar.gratify.annotation.AnnotationUtils;
import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.factory.*;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.artifacts.factory.config.ConfigurableArtifactFactory;
import foundation.polar.gratify.artifacts.factory.support.AbstractArtifactDefinition;
import foundation.polar.gratify.artifacts.factory.support.AutowireCandidateQualifier;
import foundation.polar.gratify.artifacts.factory.support.RootArtifactDefinition;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Convenience methods performing artifact lookups related to Gratify-specific annotations,
 * for example Gratify's {@link Qualifier @Qualifier} annotation.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 *
 * @see ArtifactFactoryUtils
 */
public abstract class ArtifactFactoryAnnotationUtils {
   /**
    * Retrieve all artifact of type {@code T} from the given {@code ArtifactFactory} declaring a
    * qualifier (e.g. via {@code <qualifier>} or {@code @Qualifier}) matching the given
    * qualifier, or having a artifact name matching the given qualifier.
    * @param artifactFactory the factory to get the target artifacts from (also searching ancestors)
    * @param artifactType the type of artifacts to retrieve
    * @param qualifier the qualifier for selecting among all type matches
    * @return the matching artifacts of type {@code T}
    * @throws ArtifactsException if any of the matching artifacts could not be created
    *
    * @see ArtifactFactoryUtils#artifactsOfTypeIncludingAncestors(ListableArtifactFactory, Class)
    */
   public static <T> Map<String, T> qualifiedArtifactsOfType(
      ListableArtifactFactory artifactFactory, Class<T> artifactType, String qualifier) throws ArtifactsException {

      String[] candidateArtifacts = ArtifactFactoryUtils.artifactNamesForTypeIncludingAncestors(artifactFactory, artifactType);
      Map<String, T> result = new LinkedHashMap<>(4);
      for (String artifactName : candidateArtifacts) {
         if (isQualifierMatch(qualifier::equals, artifactName, artifactFactory)) {
            result.put(artifactName, artifactFactory.getArtifact(artifactName, artifactType));
         }
      }
      return result;
   }

   /**
    * Obtain a artifact of type {@code T} from the given {@code ArtifactFactory} declaring a
    * qualifier (e.g. via {@code <qualifier>} or {@code @Qualifier}) matching the given
    * qualifier, or having a artifact name matching the given qualifier.
    * @param artifactFactory the factory to get the target artifact from (also searching ancestors)
    * @param artifactType the type of artifact to retrieve
    * @param qualifier the qualifier for selecting between multiple artifact matches
    * @return the matching artifact of type {@code T} (never {@code null})
    * @throws NoUniqueArtifactDefinitionException if multiple matching artifacts of type {@code T} found
    * @throws NoSuchArtifactDefinitionException if no matching artifact of type {@code T} found
    * @throws ArtifactsException if the artifact could not be created
    * @see ArtifactFactoryUtils#artifactOfTypeIncludingAncestors(ListableArtifactFactory, Class)
    */
   public static <T> T qualifiedArtifactOfType(ArtifactFactory artifactFactory, Class<T> artifactType, String qualifier)
      throws ArtifactsException {
      AssertUtils.notNull(artifactFactory, "ArtifactFactory must not be null");
      if (artifactFactory instanceof ListableArtifactFactory) {
         // Full qualifier matching supported.
         return qualifiedArtifactOfType((ListableArtifactFactory) artifactFactory, artifactType, qualifier);
      }
      else if (artifactFactory.containsArtifact(qualifier)) {
         // Fallback: target artifact at least found by artifact name.
         return artifactFactory.getArtifact(qualifier, artifactType);
      }
      else {
         throw new NoSuchArtifactDefinitionException(qualifier, "No matching " + artifactType.getSimpleName() +
            " artifact found for artifact name '" + qualifier +
            "'! (Note: Qualifier matching not supported because given " +
            "ArtifactFactory does not implement ConfigurableListableArtifactFactory.)");
      }
   }

   /**
    * Obtain a artifact of type {@code T} from the given {@code ArtifactFactory} declaring a qualifier
    * (e.g. {@code <qualifier>} or {@code @Qualifier}) matching the given qualifier).
    * @param bf the factory to get the target artifact from
    * @param artifactType the type of artifact to retrieve
    * @param qualifier the qualifier for selecting between multiple artifact matches
    * @return the matching artifact of type {@code T} (never {@code null})
    */
   private static <T> T qualifiedArtifactOfType(ListableArtifactFactory bf, Class<T> artifactType, String qualifier) {
      String[] candidateArtifacts = ArtifactFactoryUtils.artifactNamesForTypeIncludingAncestors(bf, artifactType);
      String matchingArtifact = null;
      for (String artifactName : candidateArtifacts) {
         if (isQualifierMatch(qualifier::equals, artifactName, bf)) {
            if (matchingArtifact != null) {
               throw new NoUniqueArtifactDefinitionException(artifactType, matchingArtifact, artifactName);
            }
            matchingArtifact = artifactName;
         }
      }
      if (matchingArtifact != null) {
         return bf.getArtifact(matchingArtifact, artifactType);
      }
      else if (bf.containsArtifact(qualifier)) {
         // Fallback: target artifact at least found by artifact name - probably a manually registered singleton.
         return bf.getArtifact(qualifier, artifactType);
      }
      else {
         throw new NoSuchArtifactDefinitionException(qualifier, "No matching " + artifactType.getSimpleName() +
            " artifact found for qualifier '" + qualifier + "' - neither qualifier match nor artifact name match!");
      }
   }

   /**
    * Check whether the named artifact declares a qualifier of the given name.
    * @param qualifier the qualifier to match
    * @param artifactName the name of the candidate artifact
    * @param artifactFactory the factory from which to retrieve the named artifact
    * @return {@code true} if either the artifact definition (in the XML case)
    * or the artifact's factory method (in the {@code @Artifact} case) defines a matching
    * qualifier value (through {@code <qualifier>} or {@code @Qualifier})
    */
   public static boolean isQualifierMatch(
      Predicate<String> qualifier, String artifactName, @Nullable ArtifactFactory artifactFactory) {

      // Try quick artifact name or alias match first...
      if (qualifier.test(artifactName)) {
         return true;
      }
      if (artifactFactory != null) {
         for (String alias : artifactFactory.getAliases(artifactName)) {
            if (qualifier.test(alias)) {
               return true;
            }
         }
         try {
            Class<?> artifactType = artifactFactory.getType(artifactName);
            if (artifactFactory instanceof ConfigurableArtifactFactory) {
               ArtifactDefinition bd = ((ConfigurableArtifactFactory) artifactFactory).getMergedArtifactDefinition(artifactName);
               // Explicit qualifier metadata on artifact definition? (typically in XML definition)
               if (bd instanceof AbstractArtifactDefinition) {
                  AbstractArtifactDefinition abd = (AbstractArtifactDefinition) bd;
                  AutowireCandidateQualifier candidate = abd.getQualifier(Qualifier.class.getName());
                  if (candidate != null) {
                     Object value = candidate.getAttribute(AutowireCandidateQualifier.VALUE_KEY);
                     if (value != null && qualifier.test(value.toString())) {
                        return true;
                     }
                  }
               }
               // Corresponding qualifier on factory method? (typically in configuration class)
               if (bd instanceof RootArtifactDefinition) {
                  Method factoryMethod = ((RootArtifactDefinition) bd).getResolvedFactoryMethod();
                  if (factoryMethod != null) {
                     Qualifier targetAnnotation = AnnotationUtils.getAnnotation(factoryMethod, Qualifier.class);
                     if (targetAnnotation != null) {
                        return qualifier.test(targetAnnotation.value());
                     }
                  }
               }
            }
            // Corresponding qualifier on artifact implementation class? (for custom user types)
            if (artifactType != null) {
               Qualifier targetAnnotation = AnnotationUtils.getAnnotation(artifactType, Qualifier.class);
               if (targetAnnotation != null) {
                  return qualifier.test(targetAnnotation.value());
               }
            }
         }
         catch (NoSuchArtifactDefinitionException ex) {
            // Ignore - can't compare qualifiers for a manually registered singleton object
         }
      }
      return false;
   }
}
