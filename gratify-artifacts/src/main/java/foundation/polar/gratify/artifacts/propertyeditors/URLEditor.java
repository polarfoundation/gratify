package foundation.polar.gratify.artifacts.propertyeditors;

import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.core.io.ResourceEditor;
import foundation.polar.gratify.utils.AssertUtils;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URL;

/**
 * Editor for {@code java.net.URL}, to directly populate a URL property
 * instead of using a String property as bridge.
 *
 * <p>Supports Gratify-style URL notation: any fully qualified standard URL
 * ("file:", "http:", etc) and Gratify's special "classpath:" pseudo-URL,
 * as well as Gratify's context-specific relative file paths.
 *
 * <p>Note: A URL must specify a valid protocol, else it will be rejected
 * upfront. However, the target resource does not necessarily have to exist
 * at the time of URL creation; this depends on the specific resource type.
 *
 * @author Juergen Hoeller
 * @see java.net.URL
 * @see foundation.polar.gratify.core.io.ResourceEditor
 * @see foundation.polar.gratify.core.io.ResourceLoader
 * @see FileEditor
 * @see InputStreamEditor
 */
public class URLEditor extends PropertyEditorSupport {

   private final ResourceEditor resourceEditor;

   /**
    * Create a new URLEditor, using a default ResourceEditor underneath.
    */
   public URLEditor() {
      this.resourceEditor = new ResourceEditor();
   }

   /**
    * Create a new URLEditor, using the given ResourceEditor underneath.
    * @param resourceEditor the ResourceEditor to use
    */
   public URLEditor(ResourceEditor resourceEditor) {
      AssertUtils.notNull(resourceEditor, "ResourceEditor must not be null");
      this.resourceEditor = resourceEditor;
   }

   @Override
   public void setAsText(String text) throws IllegalArgumentException {
      this.resourceEditor.setAsText(text);
      Resource resource = (Resource) this.resourceEditor.getValue();
      try {
         setValue(resource != null ? resource.getURL() : null);
      }
      catch (IOException ex) {
         throw new IllegalArgumentException("Could not retrieve URL for " + resource + ": " + ex.getMessage());
      }
   }

   @Override
   public String getAsText() {
      URL value = (URL) getValue();
      return (value != null ? value.toExternalForm() : "");
   }
}
