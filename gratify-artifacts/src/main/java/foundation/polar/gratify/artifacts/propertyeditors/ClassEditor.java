package foundation.polar.gratify.artifacts.propertyeditors;

import foundation.polar.gratify.utils.ClassUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyEditorSupport;

/**
 * Property editor for {@link Class java.lang.Class}, to enable the direct
 * population of a {@code Class} property without recourse to having to use a
 * String class name property as bridge.
 *
 * <p>Also supports "java.lang.String[]"-style array class names, in contrast to the
 * standard {@link Class#forName(String)} method.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @see Class#forName
 * @see foundation.polar.gratify.utils.ClassUtils#forName(String, ClassLoader)
 */
public class ClassEditor extends PropertyEditorSupport {

   @Nullable
   private final ClassLoader classLoader;

   /**
    * Create a default ClassEditor, using the thread context ClassLoader.
    */
   public ClassEditor() {
      this(null);
   }

   /**
    * Create a default ClassEditor, using the given ClassLoader.
    * @param classLoader the ClassLoader to use
    * (or {@code null} for the thread context ClassLoader)
    */
   public ClassEditor(@Nullable ClassLoader classLoader) {
      this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
   }

   @Override
   public void setAsText(String text) throws IllegalArgumentException {
      if (StringUtils.hasText(text)) {
         setValue(ClassUtils.resolveClassName(text.trim(), this.classLoader));
      }
      else {
         setValue(null);
      }
   }

   @Override
   public String getAsText() {
      Class<?> clazz = (Class<?>) getValue();
      if (clazz != null) {
         return ClassUtils.getQualifiedName(clazz);
      }
      else {
         return "";
      }
   }

}
