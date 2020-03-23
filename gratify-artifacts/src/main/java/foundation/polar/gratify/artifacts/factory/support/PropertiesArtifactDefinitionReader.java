package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.MutablePropertyValues;
import foundation.polar.gratify.artifacts.PropertyAccessor;
import foundation.polar.gratify.artifacts.factory.ArtifactDefinitionStoreException;
import foundation.polar.gratify.artifacts.factory.CannotLoadArtifactClassException;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.artifacts.factory.config.ConstructorArgumentValues;
import foundation.polar.gratify.artifacts.factory.config.RuntimeArtifactReference;
import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.core.io.support.EncodedResource;
import foundation.polar.gratify.utils.DefaultPropertiesPersister;
import foundation.polar.gratify.utils.PropertiesPersister;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Artifact definition reader for a simple properties format.
 *
 * <p>Provides artifact definition registration methods for Map/Properties and
 * ResourceBundle. Typically applied to a DefaultListableArtifactFactory.
 *
 * <p><b>Example:</b>
 *
 * <pre class="code">
 * employee.(class)=MyClass       // artifact is of class MyClass
 * employee.(abstract)=true       // this artifact can't be instantiated directly
 * employee.group=Insurance       // real property
 * employee.usesDialUp=false      // real property (potentially overridden)
 *
 * salesrep.(parent)=employee     // derives from "employee" artifact definition
 * salesrep.(lazy-init)=true      // lazily initialize this singleton artifact
 * salesrep.manager(ref)=tony     // reference to another artifact
 * salesrep.department=Sales      // real property
 *
 * techie.(parent)=employee       // derives from "employee" artifact definition
 * techie.(scope)=prototype       // artifact is a prototype (not a shared instance)
 * techie.manager(ref)=jeff       // reference to another artifact
 * techie.department=Engineering  // real property
 * techie.usesDialUp=true         // real property (overriding parent value)
 *
 * ceo.$0(ref)=secretary          // inject 'secretary' artifact as 0th constructor arg
 * ceo.$1=1000000                 // inject value '1000000' at 1st constructor arg
 * </pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see DefaultListableArtifactFactory
 */
public class PropertiesArtifactDefinitionReader extends AbstractArtifactDefinitionReader {

   /**
    * Value of a T/F attribute that represents true.
    * Anything else represents false. Case seNsItive.
    */
   public static final String TRUE_VALUE = "true";

   /**
    * Separator between artifact name and property name.
    * We follow normal Java conventions.
    */
   public static final String SEPARATOR = ".";

   /**
    * Special key to distinguish {@code owner.(class)=com.myapp.MyClass}.
    */
   public static final String CLASS_KEY = "(class)";

   /**
    * Special key to distinguish {@code owner.(parent)=parentArtifactName}.
    */
   public static final String PARENT_KEY = "(parent)";

   /**
    * Special key to distinguish {@code owner.(scope)=prototype}.
    * Default is "true".
    */
   public static final String SCOPE_KEY = "(scope)";

   /**
    * Special key to distinguish {@code owner.(singleton)=false}.
    * Default is "true".
    */
   public static final String SINGLETON_KEY = "(singleton)";

   /**
    * Special key to distinguish {@code owner.(abstract)=true}
    * Default is "false".
    */
   public static final String ABSTRACT_KEY = "(abstract)";

   /**
    * Special key to distinguish {@code owner.(lazy-init)=true}
    * Default is "false".
    */
   public static final String LAZY_INIT_KEY = "(lazy-init)";

   /**
    * Property suffix for references to other artifacts in the current
    * ArtifactFactory: e.g. {@code owner.dog(ref)=fido}.
    * Whether this is a reference to a singleton or a prototype
    * will depend on the definition of the target artifact.
    */
   public static final String REF_SUFFIX = "(ref)";

   /**
    * Prefix before values referencing other artifacts.
    */
   public static final String REF_PREFIX = "*";

   /**
    * Prefix used to denote a constructor argument definition.
    */
   public static final String CONSTRUCTOR_ARG_PREFIX = "$";

   @Nullable
   private String defaultParentArtifact;

   private PropertiesPersister propertiesPersister = new DefaultPropertiesPersister();

   /**
    * Create new PropertiesArtifactDefinitionReader for the given artifact factory.
    * @param registry the ArtifactFactory to load artifact definitions into,
    * in the form of a ArtifactDefinitionRegistry
    */
   public PropertiesArtifactDefinitionReader(ArtifactDefinitionRegistry registry) {
      super(registry);
   }

   /**
    * Set the default parent artifact for this artifact factory.
    * If a child artifact definition handled by this factory provides neither
    * a parent nor a class attribute, this default value gets used.
    * <p>Can be used e.g. for view definition files, to define a parent
    * with a default view class and common attributes for all views.
    * View definitions that define their own parent or carry their own
    * class can still override this.
    * <p>Strictly speaking, the rule that a default parent setting does
    * not apply to a artifact definition that carries a class is there for
    * backwards compatibility reasons. It still matches the typical use case.
    */
   public void setDefaultParentArtifact(@Nullable String defaultParentArtifact) {
      this.defaultParentArtifact = defaultParentArtifact;
   }

   /**
    * Return the default parent artifact for this artifact factory.
    */
   @Nullable
   public String getDefaultParentArtifact() {
      return this.defaultParentArtifact;
   }

   /**
    * Set the PropertiesPersister to use for parsing properties files.
    * The default is DefaultPropertiesPersister.
    * @see foundation.polar.gratify.util.DefaultPropertiesPersister
    */
   public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
      this.propertiesPersister =
         (propertiesPersister != null ? propertiesPersister : new DefaultPropertiesPersister());
   }

   /**
    * Return the PropertiesPersister to use for parsing properties files.
    */
   public PropertiesPersister getPropertiesPersister() {
      return this.propertiesPersister;
   }

   /**
    * Load artifact definitions from the specified properties file,
    * using all property keys (i.e. not filtering by prefix).
    * @param resource the resource descriptor for the properties file
    * @return the number of artifact definitions found
    * @throws ArtifactDefinitionStoreException in case of loading or parsing errors
    * @see #loadArtifactDefinitions(foundation.polar.gratify.core.io.Resource, String)
    */
   @Override
   public int loadArtifactDefinitions(Resource resource) throws ArtifactDefinitionStoreException {
      return loadArtifactDefinitions(new EncodedResource(resource), null);
   }

   /**
    * Load artifact definitions from the specified properties file.
    * @param resource the resource descriptor for the properties file
    * @param prefix a filter within the keys in the map: e.g. 'artifacts.'
    * (can be empty or {@code null})
    * @return the number of artifact definitions found
    * @throws ArtifactDefinitionStoreException in case of loading or parsing errors
    */
   public int loadArtifactDefinitions(Resource resource, @Nullable String prefix) throws ArtifactDefinitionStoreException {
      return loadArtifactDefinitions(new EncodedResource(resource), prefix);
   }

   /**
    * Load artifact definitions from the specified properties file.
    * @param encodedResource the resource descriptor for the properties file,
    * allowing to specify an encoding to use for parsing the file
    * @return the number of artifact definitions found
    * @throws ArtifactDefinitionStoreException in case of loading or parsing errors
    */
   public int loadArtifactDefinitions(EncodedResource encodedResource) throws ArtifactDefinitionStoreException {
      return loadArtifactDefinitions(encodedResource, null);
   }

   /**
    * Load artifact definitions from the specified properties file.
    * @param encodedResource the resource descriptor for the properties file,
    * allowing to specify an encoding to use for parsing the file
    * @param prefix a filter within the keys in the map: e.g. 'artifacts.'
    * (can be empty or {@code null})
    * @return the number of artifact definitions found
    * @throws ArtifactDefinitionStoreException in case of loading or parsing errors
    */
   public int loadArtifactDefinitions(EncodedResource encodedResource, @Nullable String prefix)
      throws ArtifactDefinitionStoreException {

      if (logger.isTraceEnabled()) {
         logger.trace("Loading properties artifact definitions from " + encodedResource);
      }

      Properties props = new Properties();
      try {
         try (InputStream is = encodedResource.getResource().getInputStream()) {
            if (encodedResource.getEncoding() != null) {
               getPropertiesPersister().load(props, new InputStreamReader(is, encodedResource.getEncoding()));
            }
            else {
               getPropertiesPersister().load(props, is);
            }
         }

         int count = registerArtifactDefinitions(props, prefix, encodedResource.getResource().getDescription());
         if (logger.isDebugEnabled()) {
            logger.debug("Loaded " + count + " artifact definitions from " + encodedResource);
         }
         return count;
      }
      catch (IOException ex) {
         throw new ArtifactDefinitionStoreException("Could not parse properties from " + encodedResource.getResource(), ex);
      }
   }

   /**
    * Register artifact definitions contained in a resource bundle,
    * using all property keys (i.e. not filtering by prefix).
    * @param rb the ResourceBundle to load from
    * @return the number of artifact definitions found
    * @throws ArtifactDefinitionStoreException in case of loading or parsing errors
    * @see #registerArtifactDefinitions(java.util.ResourceBundle, String)
    */
   public int registerArtifactDefinitions(ResourceBundle rb) throws ArtifactDefinitionStoreException {
      return registerArtifactDefinitions(rb, null);
   }

   /**
    * Register artifact definitions contained in a ResourceBundle.
    * <p>Similar syntax as for a Map. This method is useful to enable
    * standard Java internationalization support.
    * @param rb the ResourceBundle to load from
    * @param prefix a filter within the keys in the map: e.g. 'artifacts.'
    * (can be empty or {@code null})
    * @return the number of artifact definitions found
    * @throws ArtifactDefinitionStoreException in case of loading or parsing errors
    */
   public int registerArtifactDefinitions(ResourceBundle rb, @Nullable String prefix) throws ArtifactDefinitionStoreException {
      // Simply create a map and call overloaded method.
      Map<String, Object> map = new HashMap<>();
      Enumeration<String> keys = rb.getKeys();
      while (keys.hasMoreElements()) {
         String key = keys.nextElement();
         map.put(key, rb.getObject(key));
      }
      return registerArtifactDefinitions(map, prefix);
   }

   /**
    * Register artifact definitions contained in a Map, using all property keys (i.e. not
    * filtering by prefix).
    * @param map a map of {@code name} to {@code property} (String or Object). Property
    * values will be strings if coming from a Properties file etc. Property names
    * (keys) <b>must</b> be Strings. Class keys must be Strings.
    * @return the number of artifact definitions found
    * @throws ArtifactsException in case of loading or parsing errors
    * @see #registerArtifactDefinitions(java.util.Map, String, String)
    */
   public int registerArtifactDefinitions(Map<?, ?> map) throws ArtifactsException {
      return registerArtifactDefinitions(map, null);
   }

   /**
    * Register artifact definitions contained in a Map.
    * Ignore ineligible properties.
    * @param map a map of {@code name} to {@code property} (String or Object). Property
    * values will be strings if coming from a Properties file etc. Property names
    * (keys) <b>must</b> be Strings. Class keys must be Strings.
    * @param prefix a filter within the keys in the map: e.g. 'artifacts.'
    * (can be empty or {@code null})
    * @return the number of artifact definitions found
    * @throws ArtifactsException in case of loading or parsing errors
    */
   public int registerArtifactDefinitions(Map<?, ?> map, @Nullable String prefix) throws ArtifactsException {
      return registerArtifactDefinitions(map, prefix, "Map " + map);
   }

   /**
    * Register artifact definitions contained in a Map.
    * Ignore ineligible properties.
    * @param map a map of {@code name} to {@code property} (String or Object). Property
    * values will be strings if coming from a Properties file etc. Property names
    * (keys) <b>must</b> be Strings. Class keys must be Strings.
    * @param prefix a filter within the keys in the map: e.g. 'artifacts.'
    * (can be empty or {@code null})
    * @param resourceDescription description of the resource that the
    * Map came from (for logging purposes)
    * @return the number of artifact definitions found
    * @throws ArtifactsException in case of loading or parsing errors
    * @see #registerArtifactDefinitions(Map, String)
    */
   public int registerArtifactDefinitions(Map<?, ?> map, @Nullable String prefix, String resourceDescription)
      throws ArtifactsException {

      if (prefix == null) {
         prefix = "";
      }
      int artifactCount = 0;

      for (Object key : map.keySet()) {
         if (!(key instanceof String)) {
            throw new IllegalArgumentException("Illegal key [" + key + "]: only Strings allowed");
         }
         String keyString = (String) key;
         if (keyString.startsWith(prefix)) {
            // Key is of form: prefix<name>.property
            String nameAndProperty = keyString.substring(prefix.length());
            // Find dot before property name, ignoring dots in property keys.
            int sepIdx = -1;
            int propKeyIdx = nameAndProperty.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX);
            if (propKeyIdx != -1) {
               sepIdx = nameAndProperty.lastIndexOf(SEPARATOR, propKeyIdx);
            }
            else {
               sepIdx = nameAndProperty.lastIndexOf(SEPARATOR);
            }
            if (sepIdx != -1) {
               String artifactName = nameAndProperty.substring(0, sepIdx);
               if (logger.isTraceEnabled()) {
                  logger.trace("Found artifact name '" + artifactName + "'");
               }
               if (!getRegistry().containsArtifactDefinition(artifactName)) {
                  // If we haven't already registered it...
                  registerArtifactDefinition(artifactName, map, prefix + artifactName, resourceDescription);
                  ++artifactCount;
               }
            }
            else {
               // Ignore it: It wasn't a valid artifact name and property,
               // although it did start with the required prefix.
               if (logger.isDebugEnabled()) {
                  logger.debug("Invalid artifact name and property [" + nameAndProperty + "]");
               }
            }
         }
      }

      return artifactCount;
   }

   /**
    * Get all property values, given a prefix (which will be stripped)
    * and add the artifact they define to the factory with the given name.
    * @param artifactName name of the artifact to define
    * @param map a Map containing string pairs
    * @param prefix prefix of each entry, which will be stripped
    * @param resourceDescription description of the resource that the
    * Map came from (for logging purposes)
    * @throws ArtifactsException if the artifact definition could not be parsed or registered
    */
   protected void registerArtifactDefinition(String artifactName, Map<?, ?> map, String prefix, String resourceDescription)
      throws ArtifactsException {

      String className = null;
      String parent = null;
      String scope = ArtifactDefinition.SCOPE_SINGLETON;
      boolean isAbstract = false;
      boolean lazyInit = false;

      ConstructorArgumentValues cas = new ConstructorArgumentValues();
      MutablePropertyValues pvs = new MutablePropertyValues();

      String prefixWithSep = prefix + SEPARATOR;
      int beginIndex = prefixWithSep.length();

      for (Map.Entry<?, ?> entry : map.entrySet()) {
         String key = StringUtils.trimWhitespace((String) entry.getKey());
         if (key.startsWith(prefixWithSep)) {
            String property = key.substring(beginIndex);
            if (CLASS_KEY.equals(property)) {
               className = StringUtils.trimWhitespace((String) entry.getValue());
            }
            else if (PARENT_KEY.equals(property)) {
               parent = StringUtils.trimWhitespace((String) entry.getValue());
            }
            else if (ABSTRACT_KEY.equals(property)) {
               String val = StringUtils.trimWhitespace((String) entry.getValue());
               isAbstract = TRUE_VALUE.equals(val);
            }
            else if (SCOPE_KEY.equals(property)) {
               // Gratify 2.0 style
               scope = StringUtils.trimWhitespace((String) entry.getValue());
            }
            else if (SINGLETON_KEY.equals(property)) {
               // Gratify 1.2 style
               String val = StringUtils.trimWhitespace((String) entry.getValue());
               scope = ("".equals(val) || TRUE_VALUE.equals(val) ? ArtifactDefinition.SCOPE_SINGLETON :
                  ArtifactDefinition.SCOPE_PROTOTYPE);
            }
            else if (LAZY_INIT_KEY.equals(property)) {
               String val = StringUtils.trimWhitespace((String) entry.getValue());
               lazyInit = TRUE_VALUE.equals(val);
            }
            else if (property.startsWith(CONSTRUCTOR_ARG_PREFIX)) {
               if (property.endsWith(REF_SUFFIX)) {
                  int index = Integer.parseInt(property.substring(1, property.length() - REF_SUFFIX.length()));
                  cas.addIndexedArgumentValue(index, new RuntimeArtifactReference(entry.getValue().toString()));
               }
               else {
                  int index = Integer.parseInt(property.substring(1));
                  cas.addIndexedArgumentValue(index, readValue(entry));
               }
            }
            else if (property.endsWith(REF_SUFFIX)) {
               // This isn't a real property, but a reference to another prototype
               // Extract property name: property is of form dog(ref)
               property = property.substring(0, property.length() - REF_SUFFIX.length());
               String ref = StringUtils.trimWhitespace((String) entry.getValue());

               // It doesn't matter if the referenced artifact hasn't yet been registered:
               // this will ensure that the reference is resolved at runtime.
               Object val = new RuntimeArtifactReference(ref);
               pvs.add(property, val);
            }
            else {
               // It's a normal artifact property.
               pvs.add(property, readValue(entry));
            }
         }
      }

      if (logger.isTraceEnabled()) {
         logger.trace("Registering artifact definition for artifact name '" + artifactName + "' with " + pvs);
      }

      // Just use default parent if we're not dealing with the parent itself,
      // and if there's no class name specified. The latter has to happen for
      // backwards compatibility reasons.
      if (parent == null && className == null && !artifactName.equals(this.defaultParentArtifact)) {
         parent = this.defaultParentArtifact;
      }

      try {
         AbstractArtifactDefinition bd = ArtifactDefinitionReaderUtils.createArtifactDefinition(
            parent, className, getArtifactClassLoader());
         bd.setScope(scope);
         bd.setAbstract(isAbstract);
         bd.setLazyInit(lazyInit);
         bd.setConstructorArgumentValues(cas);
         bd.setPropertyValues(pvs);
         getRegistry().registerArtifactDefinition(artifactName, bd);
      }
      catch (ClassNotFoundException ex) {
         throw new CannotLoadArtifactClassException(resourceDescription, artifactName, className, ex);
      }
      catch (LinkageError err) {
         throw new CannotLoadArtifactClassException(resourceDescription, artifactName, className, err);
      }
   }

   /**
    * Reads the value of the entry. Correctly interprets artifact references for
    * values that are prefixed with an asterisk.
    */
   private Object readValue(Map.Entry<?, ?> entry) {
      Object val = entry.getValue();
      if (val instanceof String) {
         String strVal = (String) val;
         // If it starts with a reference prefix...
         if (strVal.startsWith(REF_PREFIX)) {
            // Expand the reference.
            String targetName = strVal.substring(1);
            if (targetName.startsWith(REF_PREFIX)) {
               // Escaped prefix -> use plain value.
               val = targetName;
            }
            else {
               val = new RuntimeArtifactReference(targetName);
            }
         }
      }
      return val;
   }

}
