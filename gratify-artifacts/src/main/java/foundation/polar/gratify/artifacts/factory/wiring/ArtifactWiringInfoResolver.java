package foundation.polar.gratify.artifacts.factory.wiring;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Strategy interface to be implemented by objects than can resolve bean name
 * information, given a newly instantiated bean object. Invocations to the
 * {@link #resolveWiringInfo} method on this interface will be driven by
 * the AspectJ pointcut in the relevant concrete aspect.
 *
 * <p>Metadata resolution strategy can be pluggable. A good default is
 * {@link ClassNameArtifactWiringInfoResolver}, which uses the fully-qualified
 * class name as bean name.
 *
 * @author Rod Johnson
 *
 * @see ArtifactWiringInfo
 * @see ClassNameArtifactWiringInfoResolver
 * @see foundation.polar.gratify.artifacts.factory.annotation.AnnotationArtifactWiringInfoResolver
 */
public interface ArtifactWiringInfoResolver {
   /**
    * Resolve the ArtifactWiringInfo for the given bean instance.
    * @param beanInstance the bean instance to resolve info for
    * @return the ArtifactWiringInfo, or {@code null} if not found
    */
   @Nullable
   ArtifactWiringInfo resolveWiringInfo(Object beanInstance);
}
