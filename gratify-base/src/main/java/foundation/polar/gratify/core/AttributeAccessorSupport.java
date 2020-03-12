package foundation.polar.gratify.core;

import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Support class for {@link AttributeAccessor AttributeAccessors}, providing
 * a base implementation of all methods. To be extended by subclasses.
 *
 * <p>{@link Serializable} if subclasses and all attribute values are {@link Serializable}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public abstract class AttributeAccessorSupport implements AttributeAccessor, Serializable {

   /** Map with String keys and Object values. */
   private final Map<String, Object> attributes = new LinkedHashMap<>();


   @Override
   public void setAttribute(String name, @Nullable Object value) {
      AssertUtils.notNull(name, "Name must not be null");
      if (value != null) {
         this.attributes.put(name, value);
      }
      else {
         removeAttribute(name);
      }
   }

   @Override
   @Nullable
   public Object getAttribute(String name) {
      AssertUtils.notNull(name, "Name must not be null");
      return this.attributes.get(name);
   }

   @Override
   @Nullable
   public Object removeAttribute(String name) {
      AssertUtils.notNull(name, "Name must not be null");
      return this.attributes.remove(name);
   }

   @Override
   public boolean hasAttribute(String name) {
      AssertUtils.notNull(name, "Name must not be null");
      return this.attributes.containsKey(name);
   }

   @Override
   public String[] attributeNames() {
      return StringUtils.toStringArray(this.attributes.keySet());
   }


   /**
    * Copy the attributes from the supplied AttributeAccessor to this accessor.
    * @param source the AttributeAccessor to copy from
    */
   protected void copyAttributesFrom(AttributeAccessor source) {
      AssertUtils.notNull(source, "Source must not be null");
      String[] attributeNames = source.attributeNames();
      for (String attributeName : attributeNames) {
         setAttribute(attributeName, source.getAttribute(attributeName));
      }
   }


   @Override
   public boolean equals(@Nullable Object other) {
      return (this == other || (other instanceof AttributeAccessorSupport &&
         this.attributes.equals(((AttributeAccessorSupport) other).attributes)));
   }

   @Override
   public int hashCode() {
      return this.attributes.hashCode();
   }

}
