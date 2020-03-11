package foundation.polar.gratify.aspectj;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class AspectJExtension {
   private final Property<String> version;

   public AspectJExtension(ObjectFactory objectFactory) {
      this.version = objectFactory.property(String.class).convention("1.9.5");
   }

   public Property<String> getVersion() {
      return version;
   }
}
