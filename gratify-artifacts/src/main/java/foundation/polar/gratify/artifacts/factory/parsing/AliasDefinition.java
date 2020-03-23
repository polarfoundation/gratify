package foundation.polar.gratify.artifacts.factory.parsing;

import foundation.polar.gratify.artifacts.ArtifactMetadataElement;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Representation of an alias that has been registered during the parsing process.
 *
 * @author Juergen Hoeller
 * @see ReaderEventListener#aliasRegistered(AliasDefinition)
 */
public class AliasDefinition implements ArtifactMetadataElement {

   private final String beanName;

   private final String alias;

   @Nullable
   private final Object source;

   /**
    * Create a new AliasDefinition.
    * @param beanName the canonical name of the bean
    * @param alias the alias registered for the bean
    */
   public AliasDefinition(String beanName, String alias) {
      this(beanName, alias, null);
   }

   /**
    * Create a new AliasDefinition.
    * @param beanName the canonical name of the bean
    * @param alias the alias registered for the bean
    * @param source the source object (may be {@code null})
    */
   public AliasDefinition(String beanName, String alias, @Nullable Object source) {
      AssertUtils.notNull(beanName, "Artifact name must not be null");
      AssertUtils.notNull(alias, "Alias must not be null");
      this.beanName = beanName;
      this.alias = alias;
      this.source = source;
   }


   /**
    * Return the canonical name of the bean.
    */
   public final String getArtifactName() {
      return this.beanName;
   }

   /**
    * Return the alias registered for the bean.
    */
   public final String getAlias() {
      return this.alias;
   }

   @Override
   @Nullable
   public final Object getSource() {
      return this.source;
   }

}
