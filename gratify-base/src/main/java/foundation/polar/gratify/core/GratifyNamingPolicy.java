package foundation.polar.gratify.core;

import net.sf.cglib.core.DefaultNamingPolicy;

public class GratifyNamingPolicy extends DefaultNamingPolicy {
   public static final GratifyNamingPolicy INSTANCE = new GratifyNamingPolicy();

   @Override
   protected String getTag() {
      return "ByGratifyCGLIB";
   }
}
