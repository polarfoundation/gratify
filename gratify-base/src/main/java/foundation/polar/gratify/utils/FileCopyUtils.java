package foundation.polar.gratify.utils;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.nio.file.Files;

/**
 * Simple utility methods for file and stream copying. All copy methods use a block size
 * of 4096 bytes, and close all affected streams when done. A variation of the copy
 * methods from this class that leave streams open can be found in {@link StreamUtils}.
 *
 * <p>Mainly for use within the framework, but also useful for application code.
 *
 * @author Juergen Hoeller
 * @author Hyunjin Choi
 * @see StreamUtils
 * @see FileSystemUtils
 */
public abstract class FileCopyUtils {
   /**
    * The default buffer size used when copying bytes.
    */
   public static final int BUFFER_SIZE = StreamUtils.BUFFER_SIZE;


   //---------------------------------------------------------------------
   // Copy methods for java.io.File
   //---------------------------------------------------------------------

   /**
    * Copy the contents of the given input File to the given output File.
    * @param in the file to copy from
    * @param out the file to copy to
    * @return the number of bytes copied
    * @throws IOException in case of I/O errors
    */
   public static int copy(File in, File out) throws IOException {
      AssertUtils.notNull(in, "No input File specified");
      AssertUtils.notNull(out, "No output File specified");
      return copy(Files.newInputStream(in.toPath()), Files.newOutputStream(out.toPath()));
   }

   /**
    * Copy the contents of the given byte array to the given output File.
    * @param in the byte array to copy from
    * @param out the file to copy to
    * @throws IOException in case of I/O errors
    */
   public static void copy(byte[] in, File out) throws IOException {
      AssertUtils.notNull(in, "No input byte array specified");
      AssertUtils.notNull(out, "No output File specified");
      copy(new ByteArrayInputStream(in), Files.newOutputStream(out.toPath()));
   }

   /**
    * Copy the contents of the given input File into a new byte array.
    * @param in the file to copy from
    * @return the new byte array that has been copied to
    * @throws IOException in case of I/O errors
    */
   public static byte[] copyToByteArray(File in) throws IOException {
      AssertUtils.notNull(in, "No input File specified");
      return copyToByteArray(Files.newInputStream(in.toPath()));
   }


   //---------------------------------------------------------------------
   // Copy methods for java.io.InputStream / java.io.OutputStream
   //---------------------------------------------------------------------

   /**
    * Copy the contents of the given InputStream to the given OutputStream.
    * Closes both streams when done.
    * @param in the stream to copy from
    * @param out the stream to copy to
    * @return the number of bytes copied
    * @throws IOException in case of I/O errors
    */
   public static int copy(InputStream in, OutputStream out) throws IOException {
      AssertUtils.notNull(in, "No InputStream specified");
      AssertUtils.notNull(out, "No OutputStream specified");

      try {
         return StreamUtils.copy(in, out);
      }
      finally {
         close(in);
         close(out);
      }
   }

   /**
    * Copy the contents of the given byte array to the given OutputStream.
    * Closes the stream when done.
    * @param in the byte array to copy from
    * @param out the OutputStream to copy to
    * @throws IOException in case of I/O errors
    */
   public static void copy(byte[] in, OutputStream out) throws IOException {
      AssertUtils.notNull(in, "No input byte array specified");
      AssertUtils.notNull(out, "No OutputStream specified");

      try {
         out.write(in);
      }
      finally {
         close(out);
      }
   }

   /**
    * Copy the contents of the given InputStream into a new byte array.
    * Closes the stream when done.
    * @param in the stream to copy from (may be {@code null} or empty)
    * @return the new byte array that has been copied to (possibly empty)
    * @throws IOException in case of I/O errors
    */
   public static byte[] copyToByteArray(@Nullable InputStream in) throws IOException {
      if (in == null) {
         return new byte[0];
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
      copy(in, out);
      return out.toByteArray();
   }


   //---------------------------------------------------------------------
   // Copy methods for java.io.Reader / java.io.Writer
   //---------------------------------------------------------------------

   /**
    * Copy the contents of the given Reader to the given Writer.
    * Closes both when done.
    * @param in the Reader to copy from
    * @param out the Writer to copy to
    * @return the number of characters copied
    * @throws IOException in case of I/O errors
    */
   public static int copy(Reader in, Writer out) throws IOException {
      AssertUtils.notNull(in, "No Reader specified");
      AssertUtils.notNull(out, "No Writer specified");

      try {
         int byteCount = 0;
         char[] buffer = new char[BUFFER_SIZE];
         int bytesRead = -1;
         while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            byteCount += bytesRead;
         }
         out.flush();
         return byteCount;
      }
      finally {
         close(in);
         close(out);
      }
   }

   /**
    * Copy the contents of the given String to the given output Writer.
    * Closes the writer when done.
    * @param in the String to copy from
    * @param out the Writer to copy to
    * @throws IOException in case of I/O errors
    */
   public static void copy(String in, Writer out) throws IOException {
      AssertUtils.notNull(in, "No input String specified");
      AssertUtils.notNull(out, "No Writer specified");

      try {
         out.write(in);
      }
      finally {
         close(out);
      }
   }

   /**
    * Copy the contents of the given Reader into a String.
    * Closes the reader when done.
    * @param in the reader to copy from (may be {@code null} or empty)
    * @return the String that has been copied to (possibly empty)
    * @throws IOException in case of I/O errors
    */
   public static String copyToString(@Nullable Reader in) throws IOException {
      if (in == null) {
         return "";
      }

      StringWriter out = new StringWriter();
      copy(in, out);
      return out.toString();
   }

   /**
    * Attempt to close the supplied {@link Closeable}, silently swallowing any
    * exceptions.
    * @param closeable the {@code Closeable} to close
    */
   private static void close(Closeable closeable) {
      try {
         closeable.close();
      }
      catch (IOException ex) {
         // ignore
      }
   }

}
