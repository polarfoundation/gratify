package foundation.polar.gratify.artifacts.factory.config;


import foundation.polar.gratify.artifacts.ArtifactsException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Strategy interface for resolving a value through evaluating it
 * as an expression, if applicable.
 *
 * <p>A raw {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory} does not
 * contain a default implementation of this strategy. However,
 * {@link foundation.polar.gratify.context.ApplicationContext} implementations
 * will provide expression support out of the box.
 *
 * @author Juergen Hoeller
 */
public interface ArtifactExpressionResolver {
   /**
    * Evaluate the given value as an expression, if applicable;
    * return the value as-is otherwise.
    * @param value the value to check
    * @param evalContext the evaluation context
    * @return the resolved value (potentially the given value as-is)
    * @throws ArtifactsException if evaluation failed
    */
   @Nullable
   Object evaluate(@Nullable String value, ArtifactExpressionContext evalContext) throws ArtifactsException;
}
