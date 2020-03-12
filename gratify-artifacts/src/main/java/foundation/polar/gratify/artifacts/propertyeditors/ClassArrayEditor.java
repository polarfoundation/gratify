package foundation.polar.gratify.artifacts.propertyeditors;

import foundation.polar.gratify.utils.ClassUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyEditorSupport;
import java.util.StringJoiner;

/**
 * Property editor for an array of {@link Class Classes}, to enable
 * the direct population of a {@code Class[]} property without having to
 * use a {@code String} class name property as bridge.
 *
 * <p>Also supports "java.lang.String[]"-style array class names, in contrast
 * to the standard {@link Class#forName(String)} method.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class ClassArrayEditor extends PropertyEditorSupport {

   @Nullable
   private final ClassLoader classLoader;

   /**
    * Create a default {@code ClassEditor}, using the thread
    * context {@code ClassLoader}.
    */
   public ClassArrayEditor() {
      this(null);
   }

   /**
    * Create a default {@code ClassArrayEditor}, using the given
    * {@code ClassLoader}.
    * @param classLoader the {@code ClassLoader} to use
    * (or pass {@code null} for the thread context {@code ClassLoader})
    */
   public ClassArrayEditor(@Nullable ClassLoader classLoader) {
      this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
   }

   @Override
   public void setAsText(String text) throws IllegalArgumentException {
      if (StringUtils.hasText(text)) {
         String[] classNames = StringUtils.commaDelimitedListToStringArray(text);
         Class<?>[] classes = new Class<?>[classNames.length];
         for (int i = 0; i < classNames.length; i++) {
            String className = classNames[i].trim();
            classes[i] = ClassUtils.resolveClassName(className, this.classLoader);
         }
         setValue(classes);
      }
      else {
         setValue(null);
      }
   }

   @Override
   public String getAsText() {
      Class<?>[] classes = (Class[]) getValue();
      if (ObjectUtils.isEmpty(classes)) {
         return "";
      }
      StringJoiner sj = new StringJoiner(",");
      for (Class<?> klass : classes) {
         sj.add(ClassUtils.getQualifiedName(klass));
      }
      return sj.toString();
   }

}
