package foundation.polar.gratify.artifacts.factory.config;


import foundation.polar.gratify.artifacts.ArtifactMetadataElement;
import foundation.polar.gratify.artifacts.factory.ArtifactFactoryUtils;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Holder for a ArtifactDefinition with name and aliases.
 * Can be registered as a placeholder for an inner Artifact.
 *
 * <p>Can also be used for programmatic registration of inner Artifact
 * definitions. If you don't care about artifactNameAware and the like,
 * registering RootArtifactDefinition or ChildArtifactDefinition is good enough.
 *
 * @author Juergen Hoeller
 *
 * @see foundation.polar.gratify.artifacts.factory.ArtifactNameAware
 * @see foundation.polar.gratify.artifacts.factory.support.RootArtifactDefinition
 * @see foundation.polar.gratify.artifacts.factory.support.ChildArtifactDefinition
 */
public class ArtifactDefinitionHolder implements ArtifactMetadataElement {

   private final ArtifactDefinition ArtifactDefinition;

   private final String artifactName;

   @Nullable
   private final String[] aliases;


   /**
    * Create a new ArtifactDefinitionHolder.
    * @param ArtifactDefinition the ArtifactDefinition to wrap
    * @param artifactName the name of the Artifact, as specified for the Artifact definition
    */
   public ArtifactDefinitionHolder(ArtifactDefinition ArtifactDefinition, String artifactName) {
      this(ArtifactDefinition, artifactName, null);
   }

   /**
    * Create a new ArtifactDefinitionHolder.
    * @param ArtifactDefinition the ArtifactDefinition to wrap
    * @param artifactName the name of the Artifact, as specified for the Artifact definition
    * @param aliases alias names for the Artifact, or {@code null} if none
    */
   public ArtifactDefinitionHolder(ArtifactDefinition ArtifactDefinition, String artifactName, @Nullable String[] aliases) {
      AssertUtils.notNull(ArtifactDefinition, "ArtifactDefinition must not be null");
      AssertUtils.notNull(artifactName, "Artifact name must not be null");
      this.ArtifactDefinition = ArtifactDefinition;
      this.artifactName = artifactName;
      this.aliases = aliases;
   }

   /**
    * Copy constructor: Create a new ArtifactDefinitionHolder with the
    * same contents as the given ArtifactDefinitionHolder instance.
    * <p>Note: The wrapped ArtifactDefinition reference is taken as-is;
    * it is {@code not} deeply copied.
    * @param ArtifactDefinitionHolder the ArtifactDefinitionHolder to copy
    */
   public ArtifactDefinitionHolder(ArtifactDefinitionHolder ArtifactDefinitionHolder) {
      AssertUtils.notNull(ArtifactDefinitionHolder, "ArtifactDefinitionHolder must not be null");
      this.ArtifactDefinition = ArtifactDefinitionHolder.getArtifactDefinition();
      this.artifactName = ArtifactDefinitionHolder.getArtifactName();
      this.aliases = ArtifactDefinitionHolder.getAliases();
   }


   /**
    * Return the wrapped ArtifactDefinition.
    */
   public ArtifactDefinition getArtifactDefinition() {
      return this.ArtifactDefinition;
   }

   /**
    * Return the primary name of the Artifact, as specified for the Artifact definition.
    */
   public String getArtifactName() {
      return this.artifactName;
   }

   /**
    * Return the alias names for the Artifact, as specified directly for the Artifact definition.
    * @return the array of alias names, or {@code null} if none
    */
   @Nullable
   public String[] getAliases() {
      return this.aliases;
   }

   /**
    * Expose the Artifact definition's source object.
    * @see ArtifactDefinition#getSource()
    */
   @Override
   @Nullable
   public Object getSource() {
      return this.ArtifactDefinition.getSource();
   }

   /**
    * Determine whether the given candidate name matches the Artifact name
    * or the aliases stored in this Artifact definition.
    */
   public boolean matchesName(@Nullable String candidateName) {
      return (candidateName != null && (candidateName.equals(this.artifactName) ||
         candidateName.equals(ArtifactFactoryUtils.transformedArtifactName(this.artifactName)) ||
         ObjectUtils.containsElement(this.aliases, candidateName)));
   }


   /**
    * Return a friendly, short description for the Artifact, stating name and aliases.
    * @see #getArtifactName()
    * @see #getAliases()
    */
   public String getShortDescription() {
      if (this.aliases == null) {
         return "Artifact definition with name '" + this.artifactName + "'";
      }
      return "Artifact definition with name '" + this.artifactName + "' and aliases [" + StringUtils.arrayToCommaDelimitedString(this.aliases) + ']';
   }

   /**
    * Return a long description for the Artifact, including name and aliases
    * as well as a description of the contained {@link ArtifactDefinition}.
    * @see #getShortDescription()
    * @see #getArtifactDefinition()
    */
   public String getLongDescription() {
      return getShortDescription() + ": " + this.ArtifactDefinition;
   }

   /**
    * This implementation returns the long description. Can be overridden
    * to return the short description or any kind of custom description instead.
    * @see #getLongDescription()
    * @see #getShortDescription()
    */
   @Override
   public String toString() {
      return getLongDescription();
   }


   @Override
   public boolean equals(@Nullable Object other) {
      if (this == other) {
         return true;
      }
      if (!(other instanceof ArtifactDefinitionHolder)) {
         return false;
      }
      ArtifactDefinitionHolder otherHolder = (ArtifactDefinitionHolder) other;
      return this.ArtifactDefinition.equals(otherHolder.ArtifactDefinition) &&
         this.artifactName.equals(otherHolder.artifactName) &&
         ObjectUtils.nullSafeEquals(this.aliases, otherHolder.aliases);
   }

   @Override
   public int hashCode() {
      int hashCode = this.ArtifactDefinition.hashCode();
      hashCode = 29 * hashCode + this.artifactName.hashCode();
      hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.aliases);
      return hashCode;
   }

}
