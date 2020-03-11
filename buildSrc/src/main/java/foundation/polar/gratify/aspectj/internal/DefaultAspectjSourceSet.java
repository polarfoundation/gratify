package foundation.polar.gratify.aspectj.internal;

import foundation.polar.gratify.aspectj.AspectJSourceSet;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.ConfigureUtil;

public class DefaultAspectjSourceSet extends DefaultWeavingSourceSet
   implements AspectJSourceSet, HasPublicType {
   private final SourceDirectorySet aspectJ;
   private final SourceDirectorySet allAspectJ;

   public DefaultAspectjSourceSet(ObjectFactory objectFactory, SourceSet sourceSet) {
      super(sourceSet);

      String name = sourceSet.getName();
      String displayName = ((DefaultSourceSet) sourceSet).getDisplayName();

      aspectJ = objectFactory.sourceDirectorySet("aspectj", displayName + " AspectJ source");
      aspectJ.getFilter().include("**/*.java", "**/*.aj");
      allAspectJ = objectFactory.sourceDirectorySet("all" + name, displayName + " AspectJ source");
      allAspectJ.source(aspectJ);
      allAspectJ.getFilter().include("**/*.aj");
   }

   @Override
   public AspectJSourceSet aspectJ(Closure configureClosure) {
      ConfigureUtil.configure(configureClosure, getAspectJ());
      return this;
   }

   @Override
   public AspectJSourceSet aspectJ(Action<? super SourceDirectorySet> configureAction) {
      configureAction.execute(getAspectJ());
      return this;
   }

   @Override
   public TypeOf<?> getPublicType() {
      return TypeOf.typeOf(AspectJSourceSet.class);
   }

   @Override
   public SourceDirectorySet getAspectJ() {
      return aspectJ;
   }

   @Override
   public SourceDirectorySet getAllAspectJ() {
      return allAspectJ;
   }
}
