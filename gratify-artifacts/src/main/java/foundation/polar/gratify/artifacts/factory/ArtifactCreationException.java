package foundation.polar.gratify.artifacts.factory;

import foundation.polar.gratify.artifacts.FatalArtifactException;
import foundation.polar.gratify.core.NestedRuntimeException;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when a ArtifactFactory encounters an error when
 * attempting to create a bean from a bean definition.
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ArtifactCreationException extends FatalArtifactException {

   @Nullable
   private final String artifactName;

   @Nullable
   private final String resourceDescription;

   @Nullable
   private List<Throwable> relatedCauses;


   /**
    * Create a new ArtifactCreationException.
    * @param msg the detail message
    */
   public ArtifactCreationException(String msg) {
      super(msg);
      this.artifactName = null;
      this.resourceDescription = null;
   }

   /**
    * Create a new ArtifactCreationException.
    * @param msg the detail message
    * @param cause the root cause
    */
   public ArtifactCreationException(String msg, Throwable cause) {
      super(msg, cause);
      this.artifactName = null;
      this.resourceDescription = null;
   }

   /**
    * Create a new ArtifactCreationException.
    * @param artifactName the name of the bean requested
    * @param msg the detail message
    */
   public ArtifactCreationException(String artifactName, String msg) {
      super("Error creating bean with name '" + artifactName + "': " + msg);
      this.artifactName = artifactName;
      this.resourceDescription = null;
   }

   /**
    * Create a new ArtifactCreationException.
    * @param artifactName the name of the bean requested
    * @param msg the detail message
    * @param cause the root cause
    */
   public ArtifactCreationException(String artifactName, String msg, Throwable cause) {
      this(artifactName, msg);
      initCause(cause);
   }

   /**
    * Create a new ArtifactCreationException.
    * @param resourceDescription description of the resource
    * that the bean definition came from
    * @param artifactName the name of the bean requested
    * @param msg the detail message
    */
   public ArtifactCreationException(@Nullable String resourceDescription, @Nullable String artifactName, String msg) {
      super("Error creating bean with name '" + artifactName + "'" +
         (resourceDescription != null ? " defined in " + resourceDescription : "") + ": " + msg);
      this.resourceDescription = resourceDescription;
      this.artifactName = artifactName;
      this.relatedCauses = null;
   }

   /**
    * Create a new ArtifactCreationException.
    * @param resourceDescription description of the resource
    * that the bean definition came from
    * @param artifactName the name of the bean requested
    * @param msg the detail message
    * @param cause the root cause
    */
   public ArtifactCreationException(@Nullable String resourceDescription, String artifactName, String msg, Throwable cause) {
      this(resourceDescription, artifactName, msg);
      initCause(cause);
   }


   /**
    * Return the description of the resource that the bean
    * definition came from, if any.
    */
   @Nullable
   public String getResourceDescription() {
      return this.resourceDescription;
   }

   /**
    * Return the name of the bean requested, if any.
    */
   @Nullable
   public String getArtifactName() {
      return this.artifactName;
   }

   /**
    * Add a related cause to this bean creation exception,
    * not being a direct cause of the failure but having occurred
    * earlier in the creation of the same bean instance.
    * @param ex the related cause to add
    */
   public void addRelatedCause(Throwable ex) {
      if (this.relatedCauses == null) {
         this.relatedCauses = new ArrayList<>();
      }
      this.relatedCauses.add(ex);
   }

   /**
    * Return the related causes, if any.
    * @return the array of related causes, or {@code null} if none
    */
   @Nullable
   public Throwable[] getRelatedCauses() {
      if (this.relatedCauses == null) {
         return null;
      }
      return this.relatedCauses.toArray(new Throwable[0]);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(super.toString());
      if (this.relatedCauses != null) {
         for (Throwable relatedCause : this.relatedCauses) {
            sb.append("\nRelated cause: ");
            sb.append(relatedCause);
         }
      }
      return sb.toString();
   }

   @Override
   public void printStackTrace(PrintStream ps) {
      synchronized (ps) {
         super.printStackTrace(ps);
         if (this.relatedCauses != null) {
            for (Throwable relatedCause : this.relatedCauses) {
               ps.println("Related cause:");
               relatedCause.printStackTrace(ps);
            }
         }
      }
   }

   @Override
   public void printStackTrace(PrintWriter pw) {
      synchronized (pw) {
         super.printStackTrace(pw);
         if (this.relatedCauses != null) {
            for (Throwable relatedCause : this.relatedCauses) {
               pw.println("Related cause:");
               relatedCause.printStackTrace(pw);
            }
         }
      }
   }

   @Override
   public boolean contains(@Nullable Class<?> exClass) {
      if (super.contains(exClass)) {
         return true;
      }
      if (this.relatedCauses != null) {
         for (Throwable relatedCause : this.relatedCauses) {
            if (relatedCause instanceof NestedRuntimeException &&
               ((NestedRuntimeException) relatedCause).contains(exClass)) {
               return true;
            }
         }
      }
      return false;
   }

}
