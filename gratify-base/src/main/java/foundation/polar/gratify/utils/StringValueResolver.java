package foundation.polar.gratify.utils;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Simple strategy interface for resolving a String value.
 * Used by {@link foundation.polar.gratify.beans.factory.config.ConfigurableBeanFactory}.
 *
 * @author Juergen Hoeller
 * @see foundation.polar.gratify.beans.factory.config.ConfigurableBeanFactory#resolveAliases
 * @see foundation.polar.gratify.beans.factory.config.BeanDefinitionVisitor#BeanDefinitionVisitor(StringValueResolver)
 * @see foundation.polar.gratify.beans.factory.config.PropertyPlaceholderConfigurer
 */
@FunctionalInterface
public interface StringValueResolver {
   /**
    * Resolve the given String value, for example parsing placeholders.
    * @param strVal the original String value (never {@code null})
    * @return the resolved String value (may be {@code null} when resolved to a null
    * value), possibly the original String value itself (in case of no placeholders
    * to resolve or when ignoring unresolvable placeholders)
    * @throws IllegalArgumentException in case of an unresolvable String value
    */
   @Nullable
   String resolveStringValue(String strVal);
}
