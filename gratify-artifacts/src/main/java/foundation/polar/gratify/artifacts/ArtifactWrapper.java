package foundation.polar.gratify.artifacts;

import java.beans.PropertyDescriptor;

/**
 * The central interface of gratify's low-level JavaBeans infrastructure.
 *
 * <p>Typically not used directly but rather implicitly via a
 * {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory} or a
 * {@link foundation.polar.gratify.validation.DataBinder}.
 *
 * <p>Provides operations to analyze and manipulate standard JavaBeans:
 * the ability to get and set property values (individually or in bulk),
 * get property descriptors, and query the readability/writability of properties.
 *
 * <p>This interface supports <b>nested properties</b> enabling the setting
 * of properties on subproperties to an unlimited depth.
 *
 * <p>A BeanWrapper's default for the "extractOldValueForEditor" setting
 * is "false", to avoid side effects caused by getter method invocations.
 * Turn this to "true" to expose present property values to custom editors.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see PropertyAccessor
 * @see PropertyEditorRegistry
 * @see PropertyAccessorFactory#forBeanPropertyAccess
 * @see foundation.polar.gratify.artifacts.factory.BeanFactory
 * @see foundation.polar.gratify.validation.BeanPropertyBindingResult
 * @see foundation.polar.gratify.validation.DataBinder#initBeanPropertyAccess()
 */
public interface ArtifactWrapper {
   /**
    * Specify a limit for array and collection auto-growing.
    * <p>Default is unlimited on a plain BeanWrapper.
    * @since 4.1
    */
   void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

   /**
    * Return the limit for array and collection auto-growing.
    * @since 4.1
    */
   int getAutoGrowCollectionLimit();

   /**
    * Return the bean instance wrapped by this object.
    */
   Object getWrappedInstance();

   /**
    * Return the type of the wrapped bean instance.
    */
   Class<?> getWrappedClass();

   /**
    * Obtain the PropertyDescriptors for the wrapped object
    * (as determined by standard JavaBeans introspection).
    * @return the PropertyDescriptors for the wrapped object
    */
   PropertyDescriptor[] getPropertyDescriptors();

   /**
    * Obtain the property descriptor for a specific property
    * of the wrapped object.
    * @param propertyName the property to obtain the descriptor for
    * (may be a nested path, but no indexed/mapped property)
    * @return the property descriptor for the specified property
    * @throws InvalidPropertyException if there is no such property
    */
   PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;
}
