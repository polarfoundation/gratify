package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Context object for evaluating an expression within a bean definition.
 *
 * @author Juergen Hoeller
 */
public class ArtifactExpressionContext {
   private final ConfigurableArtifactFactory beanFactory;

   @Nullable
   private final Scope scope;

   public ArtifactExpressionContext(ConfigurableArtifactFactory beanFactory, @Nullable Scope scope) {
      AssertUtils.notNull(beanFactory, "ArtifactFactory must not be null");
      this.beanFactory = beanFactory;
      this.scope = scope;
   }

   public final ConfigurableArtifactFactory getArtifactFactory() {
      return this.beanFactory;
   }

   @Nullable
   public final Scope getScope() {
      return this.scope;
   }

   public boolean containsObject(String key) {
      return (this.beanFactory.containsArtifact(key) ||
         (this.scope != null && this.scope.resolveContextualObject(key) != null));
   }

   @Nullable
   public Object getObject(String key) {
      if (this.beanFactory.containsArtifact(key)) {
         return this.beanFactory.getArtifact(key);
      }
      else if (this.scope != null) {
         return this.scope.resolveContextualObject(key);
      }
      else {
         return null;
      }
   }

   @Override
   public boolean equals(@Nullable Object other) {
      if (this == other) {
         return true;
      }
      if (!(other instanceof ArtifactExpressionContext)) {
         return false;
      }
      ArtifactExpressionContext otherContext = (ArtifactExpressionContext) other;
      return (this.beanFactory == otherContext.beanFactory && this.scope == otherContext.scope);
   }

   @Override
   public int hashCode() {
      return this.beanFactory.hashCode();
   }
}
