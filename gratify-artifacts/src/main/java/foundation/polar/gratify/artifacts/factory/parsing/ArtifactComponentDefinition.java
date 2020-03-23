package foundation.polar.gratify.artifacts.factory.parsing;

import foundation.polar.gratify.artifacts.PropertyValue;
import foundation.polar.gratify.artifacts.PropertyValues;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinitionHolder;
import foundation.polar.gratify.artifacts.factory.config.ArtifactReference;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * ComponentDefinition based on a standard ArtifactDefinition, exposing the given bean
 * definition as well as inner bean definitions and bean references for the given bean.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class ArtifactComponentDefinition
   extends ArtifactDefinitionHolder implements ComponentDefinition {
   private ArtifactDefinition[] innerArtifactDefinitions;

   private ArtifactReference[] beanReferences;

   /**
    * Create a new ArtifactComponentDefinition for the given bean.
    * @param beanDefinition the ArtifactDefinition
    * @param beanName the name of the bean
    */
   public ArtifactComponentDefinition(ArtifactDefinition beanDefinition, String beanName) {
      this(new ArtifactDefinitionHolder(beanDefinition, beanName));
   }

   /**
    * Create a new ArtifactComponentDefinition for the given bean.
    * @param beanDefinition the ArtifactDefinition
    * @param beanName the name of the bean
    * @param aliases alias names for the bean, or {@code null} if none
    */
   public ArtifactComponentDefinition(ArtifactDefinition beanDefinition, String beanName, @Nullable String[] aliases) {
      this(new ArtifactDefinitionHolder(beanDefinition, beanName, aliases));
   }

   /**
    * Create a new ArtifactComponentDefinition for the given bean.
    * @param beanDefinitionHolder the ArtifactDefinitionHolder encapsulating
    * the bean definition as well as the name of the bean
    */
   public ArtifactComponentDefinition(ArtifactDefinitionHolder beanDefinitionHolder) {
      super(beanDefinitionHolder);

      List<ArtifactDefinition> innerArtifacts = new ArrayList<>();
      List<ArtifactReference> references = new ArrayList<>();
      PropertyValues propertyValues = beanDefinitionHolder.getArtifactDefinition().getPropertyValues();
      for (PropertyValue propertyValue : propertyValues.getPropertyValues()) {
         Object value = propertyValue.getValue();
         if (value instanceof ArtifactDefinitionHolder) {
            innerArtifacts.add(((ArtifactDefinitionHolder) value).getArtifactDefinition());
         }
         else if (value instanceof ArtifactDefinition) {
            innerArtifacts.add((ArtifactDefinition) value);
         }
         else if (value instanceof ArtifactReference) {
            references.add((ArtifactReference) value);
         }
      }
      this.innerArtifactDefinitions = innerArtifacts.toArray(new ArtifactDefinition[0]);
      this.beanReferences = references.toArray(new ArtifactReference[0]);
   }

   @Override
   public String getName() {
      return getArtifactName();
   }

   @Override
   public String getDescription() {
      return getShortDescription();
   }

   @Override
   public ArtifactDefinition[] getArtifactDefinitions() {
      return new ArtifactDefinition[] {getArtifactDefinition()};
   }

   @Override
   public ArtifactDefinition[] getInnerArtifactDefinitions() {
      return this.innerArtifactDefinitions;
   }

   @Override
   public ArtifactReference[] getArtifactReferences() {
      return this.beanReferences;
   }

   /**
    * This implementation returns this ComponentDefinition's description.
    * @see #getDescription()
    */
   @Override
   public String toString() {
      return getDescription();
   }

   /**
    * This implementations expects the other object to be of type ArtifactComponentDefinition
    * as well, in addition to the superclass's equality requirements.
    */
   @Override
   public boolean equals(@Nullable Object other) {
      return (this == other || (other instanceof ArtifactComponentDefinition && super.equals(other)));
   }
}
