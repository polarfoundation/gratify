package foundation.polar.gratify.artifacts.factory;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface to be implemented by objects used within a {@link ArtifactFactory} which
 * are themselves factories for individual objects. If a bean implements this
 * interface, it is used as a factory for an object to expose, not directly as a
 * bean instance that will be exposed itself.
 *
 * <p><b>NB: A bean that implements this interface cannot be used as a normal bean.</b>
 * A FactoryArtifact is defined in a bean style, but the object exposed for bean
 * references ({@link #getObject()}) is always the object that it creates.
 *
 * <p>FactoryArtifacts can support singletons and prototypes, and can either create
 * objects lazily on demand or eagerly on startup. The {@link SmartFactoryArtifact}
 * interface allows for exposing more fine-grained behavioral metadata.
 *
 * <p>This interface is heavily used within the framework itself, for example for
 * the AOP {@link foundation.polar.gratify.aop.framework.ProxyFactoryArtifact} or the
 * {@link foundation.polar.gratify.jndi.JndiObjectFactoryArtifact}. It can be used for
 * custom components as well; however, this is only common for infrastructure code.
 *
 * <p><b>{@code FactoryArtifact} is a programmatic contract. Implementations are not
 * supposed to rely on annotation-driven injection or other reflective facilities.</b>
 * {@link #getObjectType()} {@link #getObject()} invocations may arrive early in
 * the bootstrap process, even ahead of any post-processor setup. If you need access
 * other beans, implement {@link ArtifactFactoryAware} and obtain them programmatically.
 *
 * <p>Finally, FactoryArtifact objects participate in the containing ArtifactFactory's
 * synchronization of bean creation. There is usually no need for internal
 * synchronization other than for purposes of lazy initialization within the
 * FactoryArtifact itself (or the like).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 *
 * @param <T> the bean type
 * @see foundation.polar.gratify.artifacts.factory.ArtifactFactory
 * @see foundation.polar.gratify.aop.framework.ProxyFactoryArtifact
 * @see foundation.polar.gratify.jndi.JndiObjectFactoryArtifact
 */
public interface FactoryArtifact<T> {

   /**
    * The name of an attribute that can be
    * {@link foundation.polar.gratify.core.AttributeAccessor#setAttribute set} on a
    * {@link foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition} so that
    * factory beans can signal their object type when it can't be deduced from
    * the factory bean class.
    */
   String OBJECT_TYPE_ATTRIBUTE = "factoryArtifactObjectType";

   /**
    * Return an instance (possibly shared or independent) of the object
    * managed by this factory.
    * <p>As with a {@link ArtifactFactory}, this allows support for both the
    * Singleton and Prototype design pattern.
    * <p>If this FactoryArtifact is not fully initialized yet at the time of
    * the call (for example because it is involved in a circular reference),
    * throw a corresponding {@link FactoryArtifactNotInitializedException}.
    * <p>FactoryArtifacts are allowed to return {@code null}
    * objects. The factory will consider this as normal value to be used; it
    * will not throw a FactoryArtifactNotInitializedException in this case anymore.
    * FactoryArtifact implementations are encouraged to throw
    * FactoryArtifactNotInitializedException themselves now, as appropriate.
    * @return an instance of the bean (can be {@code null})
    * @throws Exception in case of creation errors
    * @see FactoryArtifactNotInitializedException
    */
   @Nullable
   T getObject() throws Exception;

   /**
    * Return the type of object that this FactoryArtifact creates,
    * or {@code null} if not known in advance.
    * <p>This allows one to check for specific types of beans without
    * instantiating objects, for example on autowiring.
    * <p>In the case of implementations that are creating a singleton object,
    * this method should try to avoid singleton creation as far as possible;
    * it should rather estimate the type in advance.
    * For prototypes, returning a meaningful type here is advisable too.
    * <p>This method can be called <i>before</i> this FactoryArtifact has
    * been fully initialized. It must not rely on state created during
    * initialization; of course, it can still use such state if available.
    * <p><b>NOTE:</b> Autowiring will simply ignore FactoryArtifacts that return
    * {@code null} here. Therefore it is highly recommended to implement
    * this method properly, using the current state of the FactoryArtifact.
    * @return the type of object that this FactoryArtifact creates,
    * or {@code null} if not known at the time of the call
    * @see ListableArtifactFactory#getArtifactsOfType
    */
   @Nullable
   Class<?> getObjectType();

   /**
    * Is the object managed by this factory a singleton? That is,
    * will {@link #getObject()} always return the same object
    * (a reference that can be cached)?
    * <p><b>NOTE:</b> If a FactoryArtifact indicates to hold a singleton object,
    * the object returned from {@code getObject()} might get cached
    * by the owning ArtifactFactory. Hence, do not return {@code true}
    * unless the FactoryArtifact always exposes the same reference.
    * <p>The singleton status of the FactoryArtifact itself will generally
    * be provided by the owning ArtifactFactory; usually, it has to be
    * defined as singleton there.
    * <p><b>NOTE:</b> This method returning {@code false} does not
    * necessarily indicate that returned objects are independent instances.
    * An implementation of the extended {@link SmartFactoryArtifact} interface
    * may explicitly indicate independent instances through its
    * {@link SmartFactoryArtifact#isPrototype()} method. Plain {@link FactoryArtifact}
    * implementations which do not implement this extended interface are
    * simply assumed to always return independent instances if the
    * {@code isSingleton()} implementation returns {@code false}.
    * <p>The default implementation returns {@code true}, since a
    * {@code FactoryArtifact} typically manages a singleton instance.
    * @return whether the exposed object is a singleton
    * @see #getObject()
    * @see SmartFactoryArtifact#isPrototype()
    */
   default boolean isSingleton() {
      return true;
   }

}

