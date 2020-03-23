package foundation.polar.gratify.artifacts.factory;


import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Collection;

/**
 * Exception thrown when a {@code ArtifactFactory} is asked for a bean instance for which
 * multiple matching candidates have been found when only one matching bean was expected.
 *
 * @author Juergen Hoeller
 *
 * @see ArtifactFactory#getArtifact(Class)
 */
@SuppressWarnings("serial")
public class NoUniqueArtifactDefinitionException extends NoSuchArtifactDefinitionException {
   private final int numberOfArtifactsFound;

   @Nullable
   private final Collection<String> beanNamesFound;

   /**
    * Create a new {@code NoUniqueArtifactDefinitionException}.
    * @param type required type of the non-unique bean
    * @param numberOfArtifactsFound the number of matching beans
    * @param message detailed message describing the problem
    */
   public NoUniqueArtifactDefinitionException(Class<?> type, int numberOfArtifactsFound, String message) {
      super(type, message);
      this.numberOfArtifactsFound = numberOfArtifactsFound;
      this.beanNamesFound = null;
   }

   /**
    * Create a new {@code NoUniqueArtifactDefinitionException}.
    * @param type required type of the non-unique bean
    * @param beanNamesFound the names of all matching beans (as a Collection)
    */
   public NoUniqueArtifactDefinitionException(Class<?> type, Collection<String> beanNamesFound) {
      super(type, "expected single matching bean but found " + beanNamesFound.size() + ": " +
         StringUtils.collectionToCommaDelimitedString(beanNamesFound));
      this.numberOfArtifactsFound = beanNamesFound.size();
      this.beanNamesFound = beanNamesFound;
   }

   /**
    * Create a new {@code NoUniqueArtifactDefinitionException}.
    * @param type required type of the non-unique bean
    * @param beanNamesFound the names of all matching beans (as an array)
    */
   public NoUniqueArtifactDefinitionException(Class<?> type, String... beanNamesFound) {
      this(type, Arrays.asList(beanNamesFound));
   }

   /**
    * Create a new {@code NoUniqueArtifactDefinitionException}.
    * @param type required type of the non-unique bean
    * @param beanNamesFound the names of all matching beans (as a Collection)
    */
   public NoUniqueArtifactDefinitionException(ResolvableType type, Collection<String> beanNamesFound) {
      super(type, "expected single matching bean but found " + beanNamesFound.size() + ": " +
         StringUtils.collectionToCommaDelimitedString(beanNamesFound));
      this.numberOfArtifactsFound = beanNamesFound.size();
      this.beanNamesFound = beanNamesFound;
   }

   /**
    * Create a new {@code NoUniqueArtifactDefinitionException}.
    * @param type required type of the non-unique bean
    * @param beanNamesFound the names of all matching beans (as an array)
    */
   public NoUniqueArtifactDefinitionException(ResolvableType type, String... beanNamesFound) {
      this(type, Arrays.asList(beanNamesFound));
   }

   /**
    * Return the number of beans found when only one matching bean was expected.
    * For a NoUniqueArtifactDefinitionException, this will usually be higher than 1.
    * @see #getArtifactType()
    */
   @Override
   public int getNumberOfArtifactsFound() {
      return this.numberOfArtifactsFound;
   }

   /**
    * Return the names of all beans found when only one matching bean was expected.
    * Note that this may be {@code null} if not specified at construction time.
    * @see #getArtifactType()
    */
   @Nullable
   public Collection<String> getArtifactNamesFound() {
      return this.beanNamesFound;
   }
}
