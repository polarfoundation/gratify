package foundation.polar.gratify.artifacts.factory;


import foundation.polar.gratify.artifacts.ArtifactsException;

/**
 * Defines a factory which can return an Object instance
 * (possibly shared or independent) when invoked.
 *
 * <p>This interface is typically used to encapsulate a generic factory which
 * returns a new instance (prototype) of some target object on each invocation.
 *
 * <p>This interface is similar to {@link FactoryArtifact}, but implementations
 * of the latter are normally meant to be defined as SPI instances in a
 * {@link ArtifactFactory}, while implementations of this class are normally meant
 * to be fed as an API to other beans (through injection). As such, the
 * {@code getObject()} method has different exception handling behavior.
 *
 * @author Colin Sampaleanu
 *
 * @param <T> the object type
 * @see FactoryArtifact
 */
@FunctionalInterface
public interface ObjectFactory<T> {

   /**
    * Return an instance (possibly shared or independent)
    * of the object managed by this factory.
    * @return the resulting instance
    * @throws ArtifactsException in case of creation errors
    */
   T getObject() throws ArtifactsException;

}
