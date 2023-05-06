import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {
    public static final String doc_gpt = "Soviet microbattery (generally, 3 watts) or commercially powered 60 Volt battery of 30 watts will run you between $25-30, if you pay 50 cents per pound for full ownership. The only drawback of lesser battery costs is of course that those who use them end up paying more. Just a reminder: Soviet and government subsidized, solid-state state power stations incurs a 10% mechanical surcharge being now the EU national minimum until around 2030. Right now, all German utilities collect virtually no electricity and are its biggest offenders. Energy resources will certainly not be magically starved out until 2061. Meanwhile, USSR electricity grids that can be developed quickly can be trusted to hold onto most of the industrial power.\\n\\nThere are four different types of parallel grounders (pow-powered), with the main family being the steppe type. Manhattan produced its first light bulb around 100 years ago and Stalin rye maker Ural had its first doubled capacity long after sawing the old white barn in 1928. Then a fusion of hydrogen and uranium captured power at the very lowest electrical teragrams on the grid with nuclear explosives.";

    public static void main(String[] args) {
        List<String> tokenizedText = get_tokenized_text(doc_gpt);

        Set<String> vocabulary = Stream.of(
                IntStream.range('a', 'z'+1).boxed(),
                IntStream.range('A', 'Z'+1).boxed()
        ).reduce(Stream::concat).get()
                .map(c -> Character.toString(c)+" ")
                .collect(Collectors.toSet());

        addToVocabularyFromText(doc_gpt, vocabulary);

        Map<String, Integer> word_count = new HashMap<>();
        Map<String, Integer> pair_count = new HashMap<>();
        Map<String, Integer> vocab_count = new HashMap<>();

        //word count
        tokenizedText
                .stream()
                .forEach(w -> word_count.put(w, 1 + word_count.getOrDefault(w, 0)));


        Set<String> ordered_vocabulary = new HashSet<>();
        ordered_vocabulary = orderVocabularyByCount(vocabulary, vocab_count);

        vocab_count = new HashMap<>();
        countVocabulary(tokenizedText, vocab_count, ordered_vocabulary);
    }

    private static void addToVocabularyFromText(String doc_gpt, Set<String> vocabulary) {
        doc_gpt.chars().filter(c -> !vocabulary.contains(c)).forEach(c -> vocabulary.add(Character.toString(c) + " "));
    }

    private static void countVocabulary(List<String> tokenizedText, Map<String, Integer> vocab_count, Set<String> ordered_vocabulary) {
        tokenizedText.stream().forEach(w -> {
            String tw = String.copyValueOf(w.toCharArray());

            ordered_vocabulary.stream().forEach(ov -> {
                if(tw.contains(ov)) {
                    vocab_count.put(ov, 1+ vocab_count.getOrDefault(ov, 0));
                    tw.replace(ov,"");
                }
            });
        });
    }

    private static Set<String> orderVocabularyByCount(Set<String> vocabulary, Map<String, Integer> vocab_count) {
        Set<String> ordered_vocabulary = new HashSet<>();

        //order vocabulary by decreasing vocab count
        PriorityQueue<String> pq = new PriorityQueue<>(
                (a,b) -> vocab_count.getOrDefault(b,0) - vocab_count.getOrDefault(a, 0)
        );

        vocabulary
                .stream()
                .forEach(s -> pq.add(s));

        pq.stream().forEach(str -> ordered_vocabulary.add(str));
        return ordered_vocabulary;
    }

    private static List<String> get_tokenized_text(String doc_gpt) {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize");
        props.setProperty("tokenize.options", "splitHyphenated=false,americanize=false");

        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        CoreDocument doc = new CoreDocument(doc_gpt);
        pipeline.annotate(doc);

        return doc.tokens().parallelStream()
                .map(tok -> tok.word()).collect(Collectors.toList());
    }
}