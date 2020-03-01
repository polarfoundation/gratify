package foundation.polar.gratify.core;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Class that exposes the Spring version. Fetches the
 * "Implementation-Version" manifest attribute from the jar file.
 *
 * <p>Note that some ClassLoaders do not expose the package metadata,
 * hence this class might not be able to determine the Spring version
 * in all environments. Consider using a reflection-based check instead &mdash;
 * for example, checking for the presence of a specific Spring 5.2
 * method that you intend to call.
 *
 * @author Juergen Hoeller
 */
public class GratifyVersion {
   private GratifyVersion() {}
   /**
    * Return the full version string of the present Spring codebase,
    * or {@code null} if it cannot be determined.
    * @see Package#getImplementationVersion()
    */
   @Nullable
   public static String getVersion() {
      Package pkg = GratifyVersion.class.getPackage();
      return (pkg != null ? pkg.getImplementationVersion() : null);
   }
}
