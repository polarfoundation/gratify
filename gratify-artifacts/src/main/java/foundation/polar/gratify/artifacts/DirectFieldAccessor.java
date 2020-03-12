package foundation.polar.gratify.artifacts;

import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.utils.ReflectionUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link ConfigurablePropertyAccessor} implementation that directly accesses
 * instance fields. Allows for direct binding to fields instead of going through
 * JavaBean setters.
 *
 * <p>the vast majority of the {@link ArtifactWrapper} features have
 * been merged to {@link AbstractPropertyAccessor}, which means that property
 * traversal as well as collections and map access is now supported here as well.
 *
 * <p>A DirectFieldAccessor's default for the "extractOldValueForEditor" setting
 * is "true", since a field can always be read without side effects.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see #setExtractOldValueForEditor
 * @see ArtifactWrapper
 * @see foundation.polar.gratify.validation.DirectFieldBindingResult
 * @see foundation.polar.gratify.validation.DataBinder#initDirectFieldAccess()
 */
public class DirectFieldAccessor extends AbstractNestablePropertyAccessor {
   private final Map<String, FieldPropertyHandler> fieldMap = new HashMap<>();

   /**
    * Create a new DirectFieldAccessor for the given object.
    * @param object object wrapped by this DirectFieldAccessor
    */
   public DirectFieldAccessor(Object object) {
      super(object);
   }

   /**
    * Create a new DirectFieldAccessor for the given object,
    * registering a nested path that the object is in.
    * @param object object wrapped by this DirectFieldAccessor
    * @param nestedPath the nested path of the object
    * @param parent the containing DirectFieldAccessor (must not be {@code null})
    */
   protected DirectFieldAccessor(Object object, String nestedPath, DirectFieldAccessor parent) {
      super(object, nestedPath, parent);
   }


   @Override
   @Nullable
   protected FieldPropertyHandler getLocalPropertyHandler(String propertyName) {
      FieldPropertyHandler propertyHandler = this.fieldMap.get(propertyName);
      if (propertyHandler == null) {
         Field field = ReflectionUtils.findField(getWrappedClass(), propertyName);
         if (field != null) {
            propertyHandler = new FieldPropertyHandler(field);
            this.fieldMap.put(propertyName, propertyHandler);
         }
      }
      return propertyHandler;
   }

   @Override
   protected DirectFieldAccessor newNestedPropertyAccessor(Object object, String nestedPath) {
      return new DirectFieldAccessor(object, nestedPath, this);
   }

   @Override
   protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
      PropertyMatches matches = PropertyMatches.forField(propertyName, getRootClass());
      throw new NotWritablePropertyException(
         getRootClass(), getNestedPath() + propertyName,
         matches.buildErrorMessage(), matches.getPossibleMatches());
   }

   private class FieldPropertyHandler extends PropertyHandler {

      private final Field field;

      public FieldPropertyHandler(Field field) {
         super(field.getType(), true, true);
         this.field = field;
      }

      @Override
      public TypeDescriptor toTypeDescriptor() {
         return new TypeDescriptor(this.field);
      }

      @Override
      public ResolvableType getResolvableType() {
         return ResolvableType.forField(this.field);
      }

      @Override
      @Nullable
      public TypeDescriptor nested(int level) {
         return TypeDescriptor.nested(this.field, level);
      }

      @Override
      @Nullable
      public Object getValue() throws Exception {
         try {
            ReflectionUtils.makeAccessible(this.field);
            return this.field.get(getWrappedInstance());
         }

         catch (IllegalAccessException ex) {
            throw new InvalidPropertyException(getWrappedClass(),
               this.field.getName(), "Field is not accessible", ex);
         }
      }

      @Override
      public void setValue(@Nullable Object value) throws Exception {
         try {
            ReflectionUtils.makeAccessible(this.field);
            this.field.set(getWrappedInstance(), value);
         }
         catch (IllegalAccessException ex) {
            throw new InvalidPropertyException(getWrappedClass(), this.field.getName(),
               "Field is not accessible", ex);
         }
      }
   }
}
