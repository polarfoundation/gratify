package foundation.polar.gratify.artifacts.factory.support;


import org.checkerframework.checker.nullness.qual.Nullable;

import java.security.AccessControlContext;
import java.security.AccessController;

/**
 * Simple {@link SecurityContextProvider} implementation.
 *
 * @author Costin Leau
 */
public class SimpleSecurityContextProvider implements SecurityContextProvider {

   @Nullable
   private final AccessControlContext acc;

   /**
    * Construct a new {@code SimpleSecurityContextProvider} instance.
    * <p>The security context will be retrieved on each call from the current
    * thread.
    */
   public SimpleSecurityContextProvider() {
      this(null);
   }

   /**
    * Construct a new {@code SimpleSecurityContextProvider} instance.
    * <p>If the given control context is null, the security context will be
    * retrieved on each call from the current thread.
    * @param acc access control context (can be {@code null})
    * @see AccessController#getContext()
    */
   public SimpleSecurityContextProvider(@Nullable AccessControlContext acc) {
      this.acc = acc;
   }

   @Override
   public AccessControlContext getAccessControlContext() {
      return (this.acc != null ? this.acc : AccessController.getContext());
   }

}
