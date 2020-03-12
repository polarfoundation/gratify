package foundation.polar.gratify.core.io.support;

import foundation.polar.gratify.env.PropertySource;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;

/**
 * The default implementation for {@link PropertySourceFactory},
 * wrapping every resource in a {@link ResourcePropertySource}.
 *
 * @author Juergen Hoeller
 * @see PropertySourceFactory
 * @see ResourcePropertySource
 */
public class DefaultPropertySourceFactory implements PropertySourceFactory {
   @Override
   public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException {
      return (name != null ? new ResourcePropertySource(name, resource) : new ResourcePropertySource(resource));
   }
}
