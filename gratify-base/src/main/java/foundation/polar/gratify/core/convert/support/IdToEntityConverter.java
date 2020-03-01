package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalGenericConverter;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ClassUtils;
import foundation.polar.gratify.utils.ReflectionUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;

/**
 * Converts an entity identifier to a entity reference by calling a static finder method
 * on the target entity type.
 *
 * <p>For this converter to match, the finder method must be static, have the signature
 * {@code find[EntityName]([IdType])}, and return an instance of the desired entity type.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 */
final class IdToEntityConverter implements ConditionalGenericConverter {

   private final ConversionService conversionService;

   public IdToEntityConverter(ConversionService conversionService) {
      this.conversionService = conversionService;
   }

   @Override
   public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
   }

   @Override
   public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      Method finder = getFinder(targetType.getType());
      return (finder != null &&
         this.conversionService.canConvert(sourceType, TypeDescriptor.valueOf(finder.getParameterTypes()[0])));
   }

   @Override
   @Nullable
   public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (source == null) {
         return null;
      }
      Method finder = getFinder(targetType.getType());
      AssertUtils.state(finder != null, "No finder method");
      Object id = this.conversionService.convert(
         source, sourceType, TypeDescriptor.valueOf(finder.getParameterTypes()[0]));
      return ReflectionUtils.invokeMethod(finder, source, id);
   }

   @Nullable
   private Method getFinder(Class<?> entityClass) {
      String finderMethod = "find" + getEntityName(entityClass);
      Method[] methods;
      boolean localOnlyFiltered;
      try {
         methods = entityClass.getDeclaredMethods();
         localOnlyFiltered = true;
      }
      catch (SecurityException ex) {
         // Not allowed to access non-public methods...
         // Fallback: check locally declared public methods only.
         methods = entityClass.getMethods();
         localOnlyFiltered = false;
      }
      for (Method method : methods) {
         if (Modifier.isStatic(method.getModifiers()) && method.getName().equals(finderMethod) &&
            method.getParameterCount() == 1 && method.getReturnType().equals(entityClass) &&
            (localOnlyFiltered || method.getDeclaringClass().equals(entityClass))) {
            return method;
         }
      }
      return null;
   }

   private String getEntityName(Class<?> entityClass) {
      String shortName = ClassUtils.getShortName(entityClass);
      int lastDot = shortName.lastIndexOf('.');
      if (lastDot != -1) {
         return shortName.substring(lastDot + 1);
      }
      else {
         return shortName;
      }
   }
}
