package foundation.polar.gratify.core.io.support;

import foundation.polar.gratify.env.PropertySource;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;

/**
 * Strategy interface for creating resource-based {@link PropertySource} wrappers.
 *
 * @author Juergen Hoeller
 * @see DefaultPropertySourceFactory
 */
public interface PropertySourceFactory {
   /**
    * Create a {@link PropertySource} that wraps the given resource.
    * @param name the name of the property source
    * @param resource the resource (potentially encoded) to wrap
    * @return the new {@link PropertySource} (never {@code null})
    * @throws IOException if resource resolution failed
    */
   PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException;
}
