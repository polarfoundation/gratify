package foundation.polar.gratify.core;

/**
 * Interface to be implemented by decorating proxies, in particular Gratify AOP
 * proxies but potentially also custom proxies with decorator semantics.
 *
 * <p>Note that this interface should just be implemented if the decorated class
 * is not within the hierarchy of the proxy class to begin with. In particular,
 * a "target-class" proxy such as a Gratify AOP CGLIB proxy should not implement
 * it since any lookup on the target class can simply be performed on the proxy
 * class there anyway.
 *
 * <p>Defined in the core module in order to allow
 * {@link foundation.polar.gratify.annotation.AnnotationAwareOrderComparator}
 * (and potential other candidates without spring-aop dependencies) to use it
 * for introspection purposes, in particular annotation lookups.
 *
 * @author Juergen Hoeller
 */
public interface DecoratingProxy {
   /**
    * Return the (ultimate) decorated class behind this proxy.
    * <p>In case of an AOP proxy, this will be the ultimate target class,
    * not just the immediate target (in case of multiple nested proxies).
    * @return the decorated class (never {@code null})
    */
   Class<?> getDecoratedClass();
}
