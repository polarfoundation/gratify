package foundation.polar.gratify.artifacts;

/**
 * Simple implementation of the {@link TypeConverter} interface that does not operate on
 * a specific target object. This is an alternative to using a full-blown ArtifactWrapperImpl
 * instance for arbitrary type conversion needs, while using the very same conversion
 * algorithm (including delegation to {@link java.beans.PropertyEditor} and
 * {@link foundation.polar.gratify.core.convert.ConversionService}) underneath.
 *
 * <p><b>Note:</b> Due to its reliance on {@link java.beans.PropertyEditor PropertyEditors},
 * SimpleTypeConverter is <em>not</em> thread-safe. Use a separate instance for each thread.
 *
 * @author Juergen Hoeller
 * @see ArtifactWrapperImpl
 */
public class SimpleTypeConverter extends TypeConverterSupport {
   public SimpleTypeConverter() {
      this.typeConverterDelegate = new TypeConverterDelegate(this);
      registerDefaultEditors();
   }
}
