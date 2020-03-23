package foundation.polar.gratify.artifacts.factory.support;


import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.factory.ArtifactFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Interface responsible for creating instances corresponding to a root bean definition.
 *
 * <p>This is pulled out into a strategy as various approaches are possible,
 * including using CGLIB to create subclasses on the fly to support Method Injection.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface InstantiationStrategy {
   /**
    * Return an instance of the bean with the given name in this factory.
    * @param bd the bean definition
    * @param beanName the name of the bean when it is created in this context.
    * The name can be {@code null} if we are autowiring a bean which doesn't
    * belong to the factory.
    * @param owner the owning ArtifactFactory
    * @return a bean instance for this bean definition
    * @throws ArtifactsException if the instantiation attempt failed
    */
   Object instantiate(RootArtifactDefinition bd, @Nullable String beanName, ArtifactFactory owner)
      throws ArtifactsException;

   /**
    * Return an instance of the bean with the given name in this factory,
    * creating it via the given constructor.
    * @param bd the bean definition
    * @param beanName the name of the bean when it is created in this context.
    * The name can be {@code null} if we are autowiring a bean which doesn't
    * belong to the factory.
    * @param owner the owning ArtifactFactory
    * @param ctor the constructor to use
    * @param args the constructor arguments to apply
    * @return a bean instance for this bean definition
    * @throws ArtifactsException if the instantiation attempt failed
    */
   Object instantiate(RootArtifactDefinition bd, @Nullable String beanName, ArtifactFactory owner,
                      Constructor<?> ctor, Object... args) throws ArtifactsException;

   /**
    * Return an instance of the bean with the given name in this factory,
    * creating it via the given factory method.
    * @param bd the bean definition
    * @param beanName the name of the bean when it is created in this context.
    * The name can be {@code null} if we are autowiring a bean which doesn't
    * belong to the factory.
    * @param owner the owning ArtifactFactory
    * @param factoryArtifact the factory bean instance to call the factory method on,
    * or {@code null} in case of a static factory method
    * @param factoryMethod the factory method to use
    * @param args the factory method arguments to apply
    * @return a bean instance for this bean definition
    * @throws ArtifactsException if the instantiation attempt failed
    */
   Object instantiate(RootArtifactDefinition bd, @Nullable String beanName, ArtifactFactory owner,
                      @Nullable Object factoryArtifact, Method factoryMethod, Object... args)
      throws ArtifactsException;
}
