package foundation.polar.gratify.artifacts.factory.support;

import java.security.AccessControlContext;

/**
 * Provider of the security context of the code running inside the bean factory.
 *
 * @author Costin Leau
 */
public interface SecurityContextProvider {
   /**
    * Provides a security access control context relevant to a bean factory.
    * @return artifact factory security control context
    */
   AccessControlContext getAccessControlContext();
}
