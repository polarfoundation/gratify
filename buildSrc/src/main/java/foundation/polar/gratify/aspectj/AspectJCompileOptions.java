package foundation.polar.gratify.aspectj;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.*;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.compile.AbstractOptions;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Compilation options to be passed to the AspectJ compiler.
 *
 * @author Lars Grefer
 * @see org.gradle.api.tasks.compile.GroovyCompileOptions
 * @see org.gradle.api.tasks.scala.ScalaCompileOptions
 */
public class AspectJCompileOptions extends AbstractOptions {
   /**
    * Accept as source bytecode any .class files in the .jar files or directories on Path.
    * The output will include these classes, possibly as woven with any applicable aspects.
    * Path is a single argument containing a list of paths to zip files or directories.
    */
   @InputFiles
   @PathSensitive(PathSensitivity.RELATIVE)
   @SkipWhenEmpty
   private final ConfigurableFileCollection inpath;

   /**
    * Weave binary aspects from jar files and directories on path into all sources.
    * The aspects should have been output by the same version of the compiler.
    * When running the output classes, the run classpath should contain all aspectpath entries.
    * Path, like classpath, is a single argument containing a list of paths to jar files.
    */
   @Classpath
   private final ConfigurableFileCollection aspectJPath;

   /**
    * Put output classes in zip file output.jar.
    */
   @OutputFile
   @Optional
   private final RegularFileProperty outJar;

   /**
    * Generate aop xml file for load-time weaving with default name (META-INF/aop-ajc.xml).
    */
   @Input
   private final Property<Boolean> outxml;

   /**
    * Generate aop.xml file for load-time weaving with custom name.
    */
   @Input
   @Optional
   private final Property<String> outXmlFile;

   /**
    * Find and build all .java or .aj source files under any directory listed in DirPaths.
    * DirPaths, like classpath, is a single argument containing a list of paths to directories.
    */
   @SkipWhenEmpty
   @PathSensitive(PathSensitivity.RELATIVE)
   @InputFiles
   private final ConfigurableFileCollection sourceRoots;

   /**
    * Generate a build .ajsym file into the output directory.
    * Used for viewing crosscutting references by tools like the AspectJ Browser.
    */
   @Input
   private final Property<Boolean> crossRefs;

   /**
    * Override location of VM's bootclasspath for purposes of evaluating types when compiling.
    * Path is a single argument containing a list of paths to zip files or directories.
    */
   @Classpath
   private final ConfigurableFileCollection bootClassPath;

   /**
    * Override location of VM's extension directories for purposes of evaluating types when compiling.
    * Path is a single argument containing a list of paths to directories.
    */
   @Classpath
   private final ConfigurableFileCollection extDirs;

   /**
    * Specify default source encoding format.
    */
   @Input
   @Optional
   private final Property<String> encoding;

   /**
    * Emit messages about accessed/processed compilation units.
    */
   @Console
   private final Property<Boolean> verbose;

   /**
    * Any additional arguments to be passed to the compiler.
    */
   @Input
   private List<String> compilerArgs = new ArrayList<>();

   @Input
   private List<CommandLineArgumentProvider> compilerArgumentProviders = new ArrayList<>();

   /**
    * Options for running the compiler in a child process.
    */
   @Internal
   private AjcForkOptions forkOptions = new AjcForkOptions();

   public AspectJCompileOptions(ObjectFactory objectFactory) {
      inpath = objectFactory.fileCollection();
      aspectJPath  = objectFactory.fileCollection();
      outJar = objectFactory.fileProperty();
      outxml = objectFactory.property(Boolean.class).convention(false);
      outXmlFile = objectFactory.property(String.class);
      sourceRoots = objectFactory.fileCollection();
      crossRefs = objectFactory.property(Boolean.class).convention(false);
      bootClassPath = objectFactory.fileCollection();
      extDirs = objectFactory.fileCollection();
      encoding = objectFactory.property(String.class);
      verbose = objectFactory.property(Boolean.class).convention(false);
   }

   public ConfigurableFileCollection getInpath() {
      return inpath;
   }

   public ConfigurableFileCollection getAspectJPath() {
      return aspectJPath;
   }

   public RegularFileProperty getOutJar() {
      return outJar;
   }

   public Property<Boolean> getOutxml() {
      return outxml;
   }

   public Property<String> getOutXmlFile() {
      return outXmlFile;
   }

   public ConfigurableFileCollection getSourceRoots() {
      return sourceRoots;
   }

   public Property<Boolean> getCrossRefs() {
      return crossRefs;
   }

   public ConfigurableFileCollection getBootClassPath() {
      return bootClassPath;
   }

   public ConfigurableFileCollection getExtDirs() {
      return extDirs;
   }

   public Property<String> getEncoding() {
      return encoding;
   }

   public Property<Boolean> getVerbose() {
      return verbose;
   }

   public List<String> getCompilerArgs() {
      return compilerArgs;
   }

   public void setCompilerArgs(List<String> compilerArgs) {
      this.compilerArgs = compilerArgs;
   }

   public List<CommandLineArgumentProvider> getCompilerArgumentProviders() {
      return compilerArgumentProviders;
   }

   public void setCompilerArgumentProviders(List<CommandLineArgumentProvider> compilerArgumentProviders) {
      this.compilerArgumentProviders = compilerArgumentProviders;
   }

   public AjcForkOptions getForkOptions() {
      return forkOptions;
   }

   public void setForkOptions(AjcForkOptions forkOptions) {
      this.forkOptions = forkOptions;
   }
}
