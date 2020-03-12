package foundation.polar.gratify.core.io;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A resolution strategy for protocol-specific resource handles.
 *
 * <p>Used as an SPI for {@link DefaultResourceLoader}, allowing for
 * custom protocols to be handled without subclassing the loader
 * implementation (or application context implementation).
 *
 * @author Juergen Hoeller
 * @see DefaultResourceLoader#addProtocolResolver
 */
@FunctionalInterface
public interface ProtocolResolver {
   /**
    * Resolve the given location against the given resource loader
    * if this implementation's protocol matches.
    * @param location the user-specified resource location
    * @param resourceLoader the associated resource loader
    * @return a corresponding {@code Resource} handle if the given location
    * matches this resolver's protocol, or {@code null} otherwise
    */
   @Nullable
   Resource resolve(String location, ResourceLoader resourceLoader);
}
