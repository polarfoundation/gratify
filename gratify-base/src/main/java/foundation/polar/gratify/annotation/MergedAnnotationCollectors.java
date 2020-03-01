package foundation.polar.gratify.annotation;

import foundation.polar.gratify.ds.LinkedMultiValueMap;
import foundation.polar.gratify.ds.MultiValueMap;
import foundation.polar.gratify.annotation.MergedAnnotation.Adapt;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collector;

/**
 * Collector implementations that provide various reduction operations for
 * {@link MergedAnnotation} instances.
 *
 * @author Phillip Webb
 */
public abstract class MergedAnnotationCollectors {
   private static final Collector.Characteristics[] NO_CHARACTERISTICS = {};
   private static final Collector.Characteristics[] IDENTITY_FINISH_CHARACTERISTICS = {Collector.Characteristics.IDENTITY_FINISH};

   private MergedAnnotationCollectors() {}

   /**
    * Create a new {@link Collector} that accumulates merged annotations to a
    * {@link LinkedHashSet} containing {@linkplain MergedAnnotation#synthesize()
    * synthesized} versions.
    * @param <A> the annotation type
    * @return a {@link Collector} which collects and synthesizes the
    * annotations into a {@link Set}
    */
   public static <A extends java.lang.annotation.Annotation> Collector<MergedAnnotation<A>, ?, Set<A>> toAnnotationSet() {
      return Collector.of(ArrayList<A>::new, (list, annotation) -> list.add(annotation.synthesize()),
         MergedAnnotationCollectors::addAll, LinkedHashSet::new);
   }

   /**
    * Create a new {@link Collector} that accumulates merged annotations to an
    * {@link java.lang.annotation.Annotation} array containing {@linkplain MergedAnnotation#synthesize()
    * synthesized} versions.
    * @param <A> the annotation type
    * @return a {@link Collector} which collects and synthesizes the
    * annotations into an {@code Annotation[]}
    * @see #toAnnotationArray(IntFunction)
    */
   public static <A extends java.lang.annotation.Annotation> Collector<MergedAnnotation<A>, ?, java.lang.annotation.Annotation[]> toAnnotationArray() {
      return toAnnotationArray(java.lang.annotation.Annotation[]::new);
   }

   /**
    * Create a new {@link Collector} that accumulates merged annotations to an
    * {@link java.lang.annotation.Annotation} array containing {@linkplain MergedAnnotation#synthesize()
    * synthesized} versions.
    * @param <A> the annotation type
    * @param <R> the resulting array type
    * @param generator a function which produces a new array of the desired
    * type and the provided length
    * @return a {@link Collector} which collects and synthesizes the
    * annotations into an annotation array
    * @see #toAnnotationArray
    */
   public static <R extends java.lang.annotation.Annotation, A extends R> Collector<MergedAnnotation<A>, ?, R[]> toAnnotationArray(
      IntFunction<R[]> generator) {
      return Collector.of(ArrayList::new, (list, annotation) -> list.add(annotation.synthesize()),
         MergedAnnotationCollectors::addAll, list -> list.toArray(generator.apply(list.size())));
   }

   /**
    * Create a new {@link Collector} that accumulates merged annotations to an
    * {@link MultiValueMap} with items {@linkplain MultiValueMap#add(Object, Object)
    * added} from each merged annotation
    * {@link MergedAnnotation#asMap(Adapt...) as a map}.
    * @param <A> the annotation type
    * @param adaptations adaptations that should be applied to the annotation values
    * @return a {@link Collector} which collects and synthesizes the
    * annotations into a {@link LinkedMultiValueMap}
    * @see #toMultiValueMap(Function, MergedAnnotation.Adapt...)
    */
   public static <A extends java.lang.annotation.Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(
      Adapt... adaptations) {
      return toMultiValueMap(Function.identity(), adaptations);
   }

   /**
    * Create a new {@link Collector} that accumulates merged annotations to an
    * {@link MultiValueMap} with items {@linkplain MultiValueMap#add(Object, Object)
    * added} from each merged annotation
    * {@link MergedAnnotation#asMap(Adapt...) as a map}.
    * @param <A> the annotation type
    * @param adaptations adaptations that should be applied to the annotation values
    * @param finisher the finisher function for the new {@link MultiValueMap}
    * @return a {@link Collector} which collects and synthesizes the
    * annotations into a {@link LinkedMultiValueMap}
    * @see #toMultiValueMap(MergedAnnotation.Adapt...)
    */
   public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(
      Function<MultiValueMap<String, Object>, MultiValueMap<String, Object>> finisher,
      Adapt... adaptations) {

      Collector.Characteristics[] characteristics = (isSameInstance(finisher, Function.identity()) ?
         IDENTITY_FINISH_CHARACTERISTICS : NO_CHARACTERISTICS);
      return Collector.of(LinkedMultiValueMap::new,
         (map, annotation) -> annotation.asMap(adaptations).forEach(map::add),
         MergedAnnotationCollectors::merge, finisher, characteristics);
   }

   private static boolean isSameInstance(Object instance, Object candidate) {
      return instance == candidate;
   }

   private static <E, L extends List<E>> L addAll(L list, L additions) {
      list.addAll(additions);
      return list;
   }

   private static <K, V> MultiValueMap<K, V> merge(MultiValueMap<K, V> map,
                                                   MultiValueMap<K, V> additions) {
      map.addAll(additions);
      return map;
   }
}
