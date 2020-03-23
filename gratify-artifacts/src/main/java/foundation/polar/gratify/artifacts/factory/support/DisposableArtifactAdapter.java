package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.ArtifactUtils;
import foundation.polar.gratify.artifacts.factory.config.ArtifactPostProcessor;
import foundation.polar.gratify.artifacts.factory.config.DestructionAwareArtifactPostProcessor;
import foundation.polar.gratify.utils.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that implements the {@link DisposableArtifact} and {@link Runnable}
 * interfaces performing various destruction steps on a given artifact instance:
 * <ul>
 * <li>DestructionAwareArtifactPostProcessors;
 * <li>the artifact implementing DisposableArtifact itself;
 * <li>a custom destroy method specified on the artifact definition.
 * </ul>
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Stephane Nicoll
 *
 * @see AbstractArtifactFactory
 * @see foundation.polar.gratify.artifacts.factory.DisposableArtifact
 * @see foundation.polar.gratify.artifacts.factory.config.DestructionAwareArtifactPostProcessor
 * @see AbstractArtifactDefinition#getDestroyMethodName()
 */
@SuppressWarnings("serial")
public class DisposableArtifactAdapter implements DisposableArtifact, Runnable, Serializable  {
   private static final String CLOSE_METHOD_NAME = "close";

   private static final String SHUTDOWN_METHOD_NAME = "shutdown";

   private static final Log logger = LogFactory.getLog(DisposableArtifactAdapter.class);

   private final Object artifact;

   private final String artifactName;

   private final boolean invokeDisposableArtifact;

   private final boolean nonPublicAccessAllowed;

   @Nullable
   private final AccessControlContext acc;

   @Nullable
   private String destroyMethodName;

   @Nullable
   private transient Method destroyMethod;

   @Nullable
   private List<DestructionAwareArtifactPostProcessor> artifactPostProcessors;

   /**
    * Create a new DisposableArtifactAdapter for the given artifact.
    * @param artifact the artifact instance (never {@code null})
    * @param artifactName the name of the artifact
    * @param artifactDefinition the merged artifact definition
    * @param postProcessors the List of ArtifactPostProcessors
    * (potentially DestructionAwareArtifactPostProcessor), if any
    */
   public DisposableArtifactAdapter(Object artifact, String artifactName, RootArtifactDefinition artifactDefinition,
                                List<ArtifactPostProcessor> postProcessors, @Nullable AccessControlContext acc) {

      AssertUtils.notNull(artifact, "Disposable artifact must not be null");
      this.artifact = artifact;
      this.artifactName = artifactName;
      this.invokeDisposableArtifact =
         (this.artifact instanceof DisposableArtifact && !artifactDefinition.isExternallyManagedDestroyMethod("destroy"));
      this.nonPublicAccessAllowed = artifactDefinition.isNonPublicAccessAllowed();
      this.acc = acc;
      String destroyMethodName = inferDestroyMethodIfNecessary(artifact, artifactDefinition);
      if (destroyMethodName != null && !(this.invokeDisposableArtifact && "destroy".equals(destroyMethodName)) &&
         !artifactDefinition.isExternallyManagedDestroyMethod(destroyMethodName)) {
         this.destroyMethodName = destroyMethodName;
         Method destroyMethod = determineDestroyMethod(destroyMethodName);
         if (destroyMethod == null) {
            if (artifactDefinition.isEnforceDestroyMethod()) {
               throw new ArtifactDefinitionValidationException("Could not find a destroy method named '" +
                  destroyMethodName + "' on artifact with name '" + artifactName + "'");
            }
         }
         else {
            Class<?>[] paramTypes = destroyMethod.getParameterTypes();
            if (paramTypes.length > 1) {
               throw new ArtifactDefinitionValidationException("Method '" + destroyMethodName + "' of artifact '" +
                  artifactName + "' has more than one parameter - not supported as destroy method");
            }
            else if (paramTypes.length == 1 && boolean.class != paramTypes[0]) {
               throw new ArtifactDefinitionValidationException("Method '" + destroyMethodName + "' of artifact '" +
                  artifactName + "' has a non-boolean parameter - not supported as destroy method");
            }
            destroyMethod = ClassUtils.getInterfaceMethodIfPossible(destroyMethod);
         }
         this.destroyMethod = destroyMethod;
      }
      this.artifactPostProcessors = filterPostProcessors(postProcessors, artifact);
   }

   /**
    * Create a new DisposableArtifactAdapter for the given artifact.
    * @param artifact the artifact instance (never {@code null})
    * @param postProcessors the List of ArtifactPostProcessors
    * (potentially DestructionAwareArtifactPostProcessor), if any
    */
   public DisposableArtifactAdapter(Object artifact, List<ArtifactPostProcessor> postProcessors, AccessControlContext acc) {
      AssertUtils.notNull(artifact, "Disposable artifact must not be null");
      this.artifact = artifact;
      this.artifactName = artifact.getClass().getName();
      this.invokeDisposableArtifact = (this.artifact instanceof DisposableArtifact);
      this.nonPublicAccessAllowed = true;
      this.acc = acc;
      this.artifactPostProcessors = filterPostProcessors(postProcessors, artifact);
   }

   /**
    * Create a new DisposableArtifactAdapter for the given artifact.
    */
   private DisposableArtifactAdapter(Object artifact, String artifactName, boolean invokeDisposableArtifact,
                                 boolean nonPublicAccessAllowed, @Nullable String destroyMethodName,
                                 @Nullable List<DestructionAwareArtifactPostProcessor> postProcessors) {

      this.artifact = artifact;
      this.artifactName = artifactName;
      this.invokeDisposableArtifact = invokeDisposableArtifact;
      this.nonPublicAccessAllowed = nonPublicAccessAllowed;
      this.acc = null;
      this.destroyMethodName = destroyMethodName;
      this.artifactPostProcessors = postProcessors;
   }

   /**
    * If the current value of the given artifactDefinition's "destroyMethodName" property is
    * {@link AbstractArtifactDefinition#INFER_METHOD}, then attempt to infer a destroy method.
    * Candidate methods are currently limited to public, no-arg methods named "close" or
    * "shutdown" (whether declared locally or inherited). The given ArtifactDefinition's
    * "destroyMethodName" is updated to be null if no such method is found, otherwise set
    * to the name of the inferred method. This constant serves as the default for the
    * {@code @Artifact#destroyMethod} attribute and the value of the constant may also be
    * used in XML within the {@code <artifact destroy-method="">} or {@code
    * <artifacts default-destroy-method="">} attributes.
    * <p>Also processes the {@link java.io.Closeable} and {@link java.lang.AutoCloseable}
    * interfaces, reflectively calling the "close" method on implementing artifacts as well.
    */
   @Nullable
   private String inferDestroyMethodIfNecessary(Object artifact, RootArtifactDefinition artifactDefinition) {
      String destroyMethodName = artifactDefinition.getDestroyMethodName();
      if (AbstractArtifactDefinition.INFER_METHOD.equals(destroyMethodName) ||
         (destroyMethodName == null && artifact instanceof AutoCloseable)) {
         // Only perform destroy method inference or Closeable detection
         // in case of the artifact not explicitly implementing DisposableArtifact
         if (!(artifact instanceof DisposableArtifact)) {
            try {
               return artifact.getClass().getMethod(CLOSE_METHOD_NAME).getName();
            }
            catch (NoSuchMethodException ex) {
               try {
                  return artifact.getClass().getMethod(SHUTDOWN_METHOD_NAME).getName();
               }
               catch (NoSuchMethodException ex2) {
                  // no candidate destroy method found
               }
            }
         }
         return null;
      }
      return (StringUtils.hasLength(destroyMethodName) ? destroyMethodName : null);
   }

   /**
    * Search for all DestructionAwareArtifactPostProcessors in the List.
    * @param processors the List to search
    * @return the filtered List of DestructionAwareArtifactPostProcessors
    */
   @Nullable
   private List<DestructionAwareArtifactPostProcessor> filterPostProcessors(List<ArtifactPostProcessor> processors, Object artifact) {
      List<DestructionAwareArtifactPostProcessor> filteredPostProcessors = null;
      if (!CollectionUtils.isEmpty(processors)) {
         filteredPostProcessors = new ArrayList<>(processors.size());
         for (ArtifactPostProcessor processor : processors) {
            if (processor instanceof DestructionAwareArtifactPostProcessor) {
               DestructionAwareArtifactPostProcessor dabpp = (DestructionAwareArtifactPostProcessor) processor;
               if (dabpp.requiresDestruction(artifact)) {
                  filteredPostProcessors.add(dabpp);
               }
            }
         }
      }
      return filteredPostProcessors;
   }

   @Override
   public void run() {
      destroy();
   }

   @Override
   public void destroy() {
      if (!CollectionUtils.isEmpty(this.artifactPostProcessors)) {
         for (DestructionAwareArtifactPostProcessor processor : this.artifactPostProcessors) {
            processor.postProcessBeforeDestruction(this.artifact, this.artifactName);
         }
      }

      if (this.invokeDisposableArtifact) {
         if (logger.isTraceEnabled()) {
            logger.trace("Invoking destroy() on artifact with name '" + this.artifactName + "'");
         }
         try {
            if (System.getSecurityManager() != null) {
               AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                  ((DisposableArtifact) this.artifact).destroy();
                  return null;
               }, this.acc);
            }
            else {
               ((DisposableArtifact) this.artifact).destroy();
            }
         }
         catch (Throwable ex) {
            String msg = "Invocation of destroy method failed on artifact with name '" + this.artifactName + "'";
            if (logger.isDebugEnabled()) {
               logger.warn(msg, ex);
            }
            else {
               logger.warn(msg + ": " + ex);
            }
         }
      }

      if (this.destroyMethod != null) {
         invokeCustomDestroyMethod(this.destroyMethod);
      }
      else if (this.destroyMethodName != null) {
         Method methodToInvoke = determineDestroyMethod(this.destroyMethodName);
         if (methodToInvoke != null) {
            invokeCustomDestroyMethod(ClassUtils.getInterfaceMethodIfPossible(methodToInvoke));
         }
      }
   }

   @Nullable
   private Method determineDestroyMethod(String name) {
      try {
         if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged((PrivilegedAction<Method>) () -> findDestroyMethod(name));
         }
         else {
            return findDestroyMethod(name);
         }
      }
      catch (IllegalArgumentException ex) {
         throw new ArtifactDefinitionValidationException("Could not find unique destroy method on artifact with name '" +
            this.artifactName + ": " + ex.getMessage());
      }
   }

   @Nullable
   private Method findDestroyMethod(String name) {
      return (this.nonPublicAccessAllowed ?
         ArtifactUtils.findMethodWithMinimalParameters(this.artifact.getClass(), name) :
         ArtifactUtils.findMethodWithMinimalParameters(this.artifact.getClass().getMethods(), name));
   }

   /**
    * Invoke the specified custom destroy method on the given artifact.
    * <p>This implementation invokes a no-arg method if found, else checking
    * for a method with a single boolean argument (passing in "true",
    * assuming a "force" parameter), else logging an error.
    */
   private void invokeCustomDestroyMethod(final Method destroyMethod) {
      int paramCount = destroyMethod.getParameterCount();
      final Object[] args = new Object[paramCount];
      if (paramCount == 1) {
         args[0] = Boolean.TRUE;
      }
      if (logger.isTraceEnabled()) {
         logger.trace("Invoking destroy method '" + this.destroyMethodName +
            "' on artifact with name '" + this.artifactName + "'");
      }
      try {
         if (System.getSecurityManager() != null) {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
               ReflectionUtils.makeAccessible(destroyMethod);
               return null;
            });
            try {
               AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () ->
                  destroyMethod.invoke(this.artifact, args), this.acc);
            }
            catch (PrivilegedActionException pax) {
               throw (InvocationTargetException) pax.getException();
            }
         }
         else {
            ReflectionUtils.makeAccessible(destroyMethod);
            destroyMethod.invoke(this.artifact, args);
         }
      }
      catch (InvocationTargetException ex) {
         String msg = "Destroy method '" + this.destroyMethodName + "' on artifact with name '" +
            this.artifactName + "' threw an exception";
         if (logger.isDebugEnabled()) {
            logger.warn(msg, ex.getTargetException());
         }
         else {
            logger.warn(msg + ": " + ex.getTargetException());
         }
      }
      catch (Throwable ex) {
         logger.warn("Failed to invoke destroy method '" + this.destroyMethodName +
            "' on artifact with name '" + this.artifactName + "'", ex);
      }
   }


   /**
    * Serializes a copy of the state of this class,
    * filtering out non-serializable ArtifactPostProcessors.
    */
   protected Object writeReplace() {
      List<DestructionAwareArtifactPostProcessor> serializablePostProcessors = null;
      if (this.artifactPostProcessors != null) {
         serializablePostProcessors = new ArrayList<>();
         for (DestructionAwareArtifactPostProcessor postProcessor : this.artifactPostProcessors) {
            if (postProcessor instanceof Serializable) {
               serializablePostProcessors.add(postProcessor);
            }
         }
      }
      return new DisposableArtifactAdapter(this.artifact, this.artifactName, this.invokeDisposableArtifact,
         this.nonPublicAccessAllowed, this.destroyMethodName, serializablePostProcessors);
   }


   /**
    * Check whether the given artifact has any kind of destroy method to call.
    * @param artifact the artifact instance
    * @param artifactDefinition the corresponding artifact definition
    */
   public static boolean hasDestroyMethod(Object artifact, RootArtifactDefinition artifactDefinition) {
      if (artifact instanceof DisposableArtifact || artifact instanceof AutoCloseable) {
         return true;
      }
      String destroyMethodName = artifactDefinition.getDestroyMethodName();
      if (AbstractArtifactDefinition.INFER_METHOD.equals(destroyMethodName)) {
         return (ClassUtils.hasMethod(artifact.getClass(), CLOSE_METHOD_NAME) ||
            ClassUtils.hasMethod(artifact.getClass(), SHUTDOWN_METHOD_NAME));
      }
      return StringUtils.hasLength(destroyMethodName);
   }

   /**
    * Check whether the given artifact has destruction-aware post-processors applying to it.
    * @param artifact the artifact instance
    * @param postProcessors the post-processor candidates
    */
   public static boolean hasApplicableProcessors(Object artifact, List<ArtifactPostProcessor> postProcessors) {
      if (!CollectionUtils.isEmpty(postProcessors)) {
         for (ArtifactPostProcessor processor : postProcessors) {
            if (processor instanceof DestructionAwareArtifactPostProcessor) {
               DestructionAwareArtifactPostProcessor dabpp = (DestructionAwareArtifactPostProcessor) processor;
               if (dabpp.requiresDestruction(artifact)) {
                  return true;
               }
            }
         }
      }
      return false;
   }
}
