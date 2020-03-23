package foundation.polar.gratify.artifacts.support;


import foundation.polar.gratify.artifacts.PropertyEditorRegistrar;
import foundation.polar.gratify.artifacts.PropertyEditorRegistry;
import foundation.polar.gratify.artifacts.PropertyEditorRegistrySupport;
import foundation.polar.gratify.artifacts.propertyeditors.*;
import foundation.polar.gratify.core.io.ContextResource;
import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.core.io.ResourceEditor;
import foundation.polar.gratify.core.io.ResourceLoader;
import foundation.polar.gratify.core.io.support.ResourceArrayPropertyEditor;
import foundation.polar.gratify.core.io.support.ResourcePatternResolver;
import foundation.polar.gratify.env.PropertyResolver;
import org.xml.sax.InputSource;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

/**
 * PropertyEditorRegistrar implementation that populates a given
 * {@link foundation.polar.gratify.artifacts.PropertyEditorRegistry}
 * (typically a {@link foundation.polar.gratify.artifacts.ArtifactWrapper} used for bean
 * creation within an {@link foundation.polar.gratify.context.ApplicationContext})
 * with resource editors. Used by
 * {@link foundation.polar.gratify.di.support.AbstractApplicationContext}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ResourceEditorRegistrar implements PropertyEditorRegistrar {

   private final PropertyResolver propertyResolver;

   private final ResourceLoader resourceLoader;

   /**
    * Create a new ResourceEditorRegistrar for the given {@link ResourceLoader}
    * and {@link PropertyResolver}.
    * @param resourceLoader the ResourceLoader (or ResourcePatternResolver)
    * to create editors for (usually an ApplicationContext)
    * @param propertyResolver the PropertyResolver (usually an Environment)
    * @see foundation.polar.gratify.env.Environment
    * @see foundation.polar.gratify.core.io.support.ResourcePatternResolver
    * @see foundation.polar.gratify.di.ApplicationContext
    */
   public ResourceEditorRegistrar(ResourceLoader resourceLoader, PropertyResolver propertyResolver) {
      this.resourceLoader = resourceLoader;
      this.propertyResolver = propertyResolver;
   }


   /**
    * Populate the given {@code registry} with the following resource editors:
    * ResourceEditor, InputStreamEditor, InputSourceEditor, FileEditor, URLEditor,
    * URIEditor, ClassEditor, ClassArrayEditor.
    * <p>If this registrar has been configured with a {@link ResourcePatternResolver},
    * a ResourceArrayPropertyEditor will be registered as well.
    * @see foundation.polar.gratify.core.io.ResourceEditor
    * @see foundation.polar.gratify.artifacts.propertyeditors.InputStreamEditor
    * @see foundation.polar.gratify.artifacts.propertyeditors.InputSourceEditor
    * @see foundation.polar.gratify.artifacts.propertyeditors.FileEditor
    * @see foundation.polar.gratify.artifacts.propertyeditors.URLEditor
    * @see foundation.polar.gratify.artifacts.propertyeditors.URIEditor
    * @see foundation.polar.gratify.artifacts.propertyeditors.ClassEditor
    * @see foundation.polar.gratify.artifacts.propertyeditors.ClassArrayEditor
    * @see foundation.polar.gratify.core.io.support.ResourceArrayPropertyEditor
    */
   @Override
   public void registerCustomEditors(PropertyEditorRegistry registry) {
      ResourceEditor baseEditor = new ResourceEditor(this.resourceLoader, this.propertyResolver);
      doRegisterEditor(registry, Resource.class, baseEditor);
      doRegisterEditor(registry, ContextResource.class, baseEditor);
      doRegisterEditor(registry, InputStream.class, new InputStreamEditor(baseEditor));
      doRegisterEditor(registry, InputSource.class, new InputSourceEditor(baseEditor));
      doRegisterEditor(registry, File.class, new FileEditor(baseEditor));
      doRegisterEditor(registry, Path.class, new PathEditor(baseEditor));
      doRegisterEditor(registry, Reader.class, new ReaderEditor(baseEditor));
      doRegisterEditor(registry, URL.class, new URLEditor(baseEditor));

      ClassLoader classLoader = this.resourceLoader.getClassLoader();
      doRegisterEditor(registry, URI.class, new URIEditor(classLoader));
      doRegisterEditor(registry, Class.class, new ClassEditor(classLoader));
      doRegisterEditor(registry, Class[].class, new ClassArrayEditor(classLoader));

      if (this.resourceLoader instanceof ResourcePatternResolver) {
         doRegisterEditor(registry, Resource[].class,
            new ResourceArrayPropertyEditor((ResourcePatternResolver) this.resourceLoader, this.propertyResolver));
      }
   }

   /**
    * Override default editor, if possible (since that's what we really mean to do here);
    * otherwise register as a custom editor.
    */
   private void doRegisterEditor(PropertyEditorRegistry registry, Class<?> requiredType, PropertyEditor editor) {
      if (registry instanceof PropertyEditorRegistrySupport) {
         ((PropertyEditorRegistrySupport) registry).overrideDefaultEditor(requiredType, editor);
      }
      else {
         registry.registerCustomEditor(requiredType, editor);
      }
   }

}
