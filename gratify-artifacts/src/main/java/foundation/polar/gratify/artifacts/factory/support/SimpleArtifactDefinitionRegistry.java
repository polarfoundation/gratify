package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.factory.ArtifactDefinitionStoreException;
import foundation.polar.gratify.artifacts.factory.NoSuchArtifactDefinitionException;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.core.SimpleAliasRegistry;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of the {@link ArtifactDefinitionRegistry} interface.
 * Provides registry capabilities only, with no factory capabilities built in.
 * Can for example be used for testing artifact definition readers.
 *
 * @author Juergen Hoeller
 */
public class SimpleArtifactDefinitionRegistry extends SimpleAliasRegistry implements ArtifactDefinitionRegistry {

   /** Map of artifact definition objects, keyed by artifact name. */
   private final Map<String, ArtifactDefinition> artifactDefinitionMap = new ConcurrentHashMap<>(64);
   
   @Override
   public void registerArtifactDefinition(String artifactName, ArtifactDefinition artifactDefinition)
      throws ArtifactDefinitionStoreException {

      AssertUtils.hasText(artifactName, "'artifactName' must not be empty");
      AssertUtils.notNull(artifactDefinition, "ArtifactDefinition must not be null");
      this.artifactDefinitionMap.put(artifactName, artifactDefinition);
   }

   @Override
   public void removeArtifactDefinition(String artifactName) throws NoSuchArtifactDefinitionException {
      if (this.artifactDefinitionMap.remove(artifactName) == null) {
         throw new NoSuchArtifactDefinitionException(artifactName);
      }
   }

   @Override
   public ArtifactDefinition getArtifactDefinition(String artifactName) throws NoSuchArtifactDefinitionException {
      ArtifactDefinition bd = this.artifactDefinitionMap.get(artifactName);
      if (bd == null) {
         throw new NoSuchArtifactDefinitionException(artifactName);
      }
      return bd;
   }

   @Override
   public boolean containsArtifactDefinition(String artifactName) {
      return this.artifactDefinitionMap.containsKey(artifactName);
   }

   @Override
   public String[] getArtifactDefinitionNames() {
      return StringUtils.toStringArray(this.artifactDefinitionMap.keySet());
   }

   @Override
   public int getArtifactDefinitionCount() {
      return this.artifactDefinitionMap.size();
   }

   @Override
   public boolean isArtifactNameInUse(String artifactName) {
      return isAlias(artifactName) || containsArtifactDefinition(artifactName);
   }
}
