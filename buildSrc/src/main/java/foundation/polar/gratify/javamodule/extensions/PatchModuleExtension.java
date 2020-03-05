package foundation.polar.gratify.javamodule.extensions;

import foundation.polar.gratify.javamodule.utils.PatchModuleResolver;
import foundation.polar.gratify.javamodule.utils.TaskOption;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * group the jars in classpath collection by config, if jar in path config, then put jar into --patch-module, if not in
 * path config, put it into --module-path
 */
public class PatchModuleExtension {
   private List<String> config = new ArrayList<>();
   private Map<String, String> indexByJar = new HashMap<>();

   public List<String> getConfig() {
      return config;
   }

   public void setConfig(List<String> config) {
      this.config = config;
      indexByJar = config.stream().map(s -> s.split("=")).collect(Collectors.toMap(c -> c[1], c -> c[0]));
   }

   public PatchModuleResolver resolvePatched(FileCollection classpath) {
      return resolvePatched(jarName -> classpath.filter(jar -> jar.getName().endsWith(jarName)).getAsPath());
   }

   public PatchModuleResolver resolvePatched(UnaryOperator<String> jarNameResolver) {
      return new PatchModuleResolver(this, jarNameResolver);
   }

   public Set<String> getJars() {
      return indexByJar.keySet();
   }

   public boolean isUnpatched(File jar) {
      return !indexByJar.containsKey(jar.getName());
   }

   public Optional<TaskOption> buildModulePathOption(FileCollection classpath) {
      String modulePath = classpath.filter(this::isUnpatched).getAsPath();
      if (modulePath.isEmpty()) {
         return Optional.empty();
      }
      return Optional.of(new TaskOption("--module-path", modulePath));
   }

   @Override
   public String toString() {
      return "PatchModuleExtension{" +
         "config=" + config +
         '}';
   }

}
