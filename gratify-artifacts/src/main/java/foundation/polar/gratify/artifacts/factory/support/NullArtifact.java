package foundation.polar.gratify.artifacts.factory.support;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Internal representation of a null bean instance, e.g. for a {@code null} value
 * returned from {@link FactoryArtifact#getObject()} or from a factory method.
 *
 * <p>Each such null bean is represented by a dedicated {@code NullArtifact} instance
 * which are not equal to each other, uniquely differentiating each bean as returned
 * from all variants of {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory#getArtifact}.
 * However, each such instance will return {@code true} for {@code #equals(null)}
 * and returns "null" from {@code #toString()}, which is how they can be tested
 * externally (since this class itself is not public).
 *
 * @author Juergen Hoeller
 */
final class NullArtifact {
   NullArtifact() {
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      return (this == obj || obj == null);
   }

   @Override
   public int hashCode() {
      return NullArtifact.class.hashCode();
   }

   @Override
   public String toString() {
      return "null";
   }
}
