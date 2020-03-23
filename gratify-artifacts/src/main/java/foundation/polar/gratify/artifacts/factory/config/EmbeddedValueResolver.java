package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.utils.StringValueResolver;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link StringValueResolver} adapter for resolving placeholders and
 * expressions against a {@link ConfigurableArtifactFactory}.
 *
 * <p>Note that this adapter resolves expressions as well, in contrast
 * to the {@link ConfigurableArtifactFactory#resolveEmbeddedValue} method.
 * The {@link ArtifactExpressionContext} used is for the plain bean factory,
 * with no scope specified for any contextual objects to access.
 *
 * @author Juergen Hoeller
 *
 * @see ConfigurableArtifactFactory#resolveEmbeddedValue(String)
 * @see ConfigurableArtifactFactory#getArtifactExpressionResolver()
 * @see ArtifactExpressionContext
 */
public class EmbeddedValueResolver implements StringValueResolver {
   private final ArtifactExpressionContext exprContext;

   @Nullable
   private final ArtifactExpressionResolver exprResolver;

   public EmbeddedValueResolver(ConfigurableArtifactFactory beanFactory) {
      this.exprContext = new ArtifactExpressionContext(beanFactory, null);
      this.exprResolver = beanFactory.getArtifactExpressionResolver();
   }

   @Override
   @Nullable
   public String resolveStringValue(String strVal) {
      String value = this.exprContext.getArtifactFactory().resolveEmbeddedValue(strVal);
      if (this.exprResolver != null && value != null) {
         Object evaluated = this.exprResolver.evaluate(value, this.exprContext);
         value = (evaluated != null ? evaluated.toString() : null);
      }
      return value;
   }
}
