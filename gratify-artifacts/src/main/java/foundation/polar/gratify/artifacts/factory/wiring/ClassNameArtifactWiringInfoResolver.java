package foundation.polar.gratify.artifacts.factory.wiring;

import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ClassUtils;

/**
 * Simple default implementation of the {@link ArtifactWiringInfoResolver} interface,
 * looking for a bean with the same name as the fully-qualified class name.
 * This matches the default name of the bean in a Gratify XML file if the
 * bean tag's "id" attribute is not used.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class ClassNameArtifactWiringInfoResolver implements ArtifactWiringInfoResolver {
   @Override
   public ArtifactWiringInfo resolveWiringInfo(Object beanInstance) {
      AssertUtils.notNull(beanInstance, "Artifact instance must not be null");
      return new ArtifactWiringInfo(ClassUtils.getUserClass(beanInstance).getName(), true);
   }
}
