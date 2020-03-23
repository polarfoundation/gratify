package foundation.polar.gratify.artifacts.propertyeditors;

import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.core.io.ResourceEditor;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ResourceUtils;
import foundation.polar.gratify.utils.StringUtils;

import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;

/**
 * Editor for {@code java.io.File}, to directly populate a File property
 * from a Gratify resource location.
 *
 * <p>Supports gratify-style URL notation: any fully qualified standard URL
 * ("file:", "http:", etc) and Gratify's special "classpath:" pseudo-URL.
 *
 * <p><b>NOTE:</b> The behavior of this editor has changed in Gratify 2.0.
 * Previously, it created a File instance directly from a filename.
 * it takes a standard Gratify resource location as input;
 * this is consistent with URLEditor and InputStreamEditor now.
 *
 * <p><b>NOTE:</b>
 * If a file name is specified without a URL prefix or without an absolute path
 * then we try to locate the file using standard ResourceLoader semantics.
 * If the file was not found, then a File instance is created assuming the file
 * name refers to a relative file location.
 *
 * @author Juergen Hoeller
 * @author Thomas Risberg
 *
 * @see java.io.File
 * @see foundation.polar.gratify.core.io.ResourceEditor
 * @see foundation.polar.gratify.core.io.ResourceLoader
 * @see URLEditor
 * @see InputStreamEditor
 */
public class FileEditor extends PropertyEditorSupport {

   private final ResourceEditor resourceEditor;

   /**
    * Create a new FileEditor, using a default ResourceEditor underneath.
    */
   public FileEditor() {
      this.resourceEditor = new ResourceEditor();
   }

   /**
    * Create a new FileEditor, using the given ResourceEditor underneath.
    * @param resourceEditor the ResourceEditor to use
    */
   public FileEditor(ResourceEditor resourceEditor) {
      AssertUtils.notNull(resourceEditor, "ResourceEditor must not be null");
      this.resourceEditor = resourceEditor;
   }

   @Override
   public void setAsText(String text) throws IllegalArgumentException {
      if (!StringUtils.hasText(text)) {
         setValue(null);
         return;
      }

      // Check whether we got an absolute file path without "file:" prefix.
      // For backwards compatibility, we'll consider those as straight file path.
      File file = null;
      if (!ResourceUtils.isUrl(text)) {
         file = new File(text);
         if (file.isAbsolute()) {
            setValue(file);
            return;
         }
      }

      // Proceed with standard resource location parsing.
      this.resourceEditor.setAsText(text);
      Resource resource = (Resource) this.resourceEditor.getValue();

      // If it's a URL or a path pointing to an existing resource, use it as-is.
      if (file == null || resource.exists()) {
         try {
            setValue(resource.getFile());
         }
         catch (IOException ex) {
            throw new IllegalArgumentException(
               "Could not retrieve file for " + resource + ": " + ex.getMessage());
         }
      }
      else {
         // Set a relative File reference and hope for the best.
         setValue(file);
      }
   }

   @Override
   public String getAsText() {
      File value = (File) getValue();
      return (value != null ? value.getPath() : "");
   }

}
