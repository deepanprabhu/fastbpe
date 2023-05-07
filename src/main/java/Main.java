import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {
    public static final String doc_gpt = "Soviet microbattery (generally, 3 watts) or commercially powered 60 Volt battery of 30 watts will run you between $25-30, if you pay 50 cents per pound for full ownership. The only drawback of lesser battery costs is of course that those who use them end up paying more. Just a reminder: Soviet and government subsidized, solid-state state power stations incurs a 10% mechanical surcharge being now the EU national minimum until around 2030. Right now, all German utilities collect virtually no electricity and are its biggest offenders. Energy resources will certainly not be magically starved out until 2061. Meanwhile, USSR electricity grids that can be developed quickly can be trusted to hold onto most of the industrial power.\\n\\nThere are four different types of parallel grounders (pow-powered), with the main family being the steppe type. Manhattan produced its first light bulb around 100 years ago and Stalin rye maker Ural had its first doubled capacity long after sawing the old white barn in 1928. Then a fusion of hydrogen and uranium captured power at the very lowest electrical teragrams on the grid with nuclear explosives.";

    public static void main(String[] args) {
        List<String> tokenizedText = get_tokenized_text(doc_gpt);

        //Alphabets
        Set<String> vocabulary = Stream.of(
                        IntStream.range('a', 'z' + 1).boxed(),
                        IntStream.range('A', 'Z' + 1).boxed()
                ).reduce(Stream::concat).get()
                .map(c -> Character.toString(c) + " ")
                .collect(Collectors.toSet());

        addToVocabularyFromText(doc_gpt, vocabulary);

        Map<String, Integer> pair_count;
        Map<String, Integer> word_count = new HashMap<>();
        Map<String, Integer> vocab_count = new HashMap<>();

        //word count
        updateTokenTextWordCount(tokenizedText, word_count);


        Set<String> ordered_vocabulary = new HashSet<>();
        ordered_vocabulary = reOrderVocabularyByCount(vocabulary, vocab_count);

        vocab_count = new HashMap<>();
        countVocabulary(tokenizedText, vocab_count, ordered_vocabulary);

        ordered_vocabulary = reOrderVocabularyByCount(vocabulary, vocab_count);

        int numMerges = 20;
        while(numMerges-- > 0) {
            List<String> topPair = new ArrayList<>();
            pair_count = countPairs(tokenizedText, vocab_count, topPair);
            System.out.println("topPair " + topPair.get(0) + " " + topPair.get(1));
            mergePairInVocabulary(tokenizedText, topPair, word_count, vocab_count, pair_count);
        }
        System.out.println("");
    }

    private static void updateTokenTextWordCount(List<String> tokenizedText, Map<String, Integer> word_count) {
        word_count.clear();
        tokenizedText
                .stream()
                .forEach(w -> word_count.put(w, 1 + word_count.getOrDefault(w, 0)));
    }

    private static void mergePairInVocabulary(List<String> tokenizedText, List<String> topPair,
                                              Map<String, Integer> word_count,
                                              Map<String, Integer> vocab_count,
                                              Map<String, Integer> pair_count) {

        String pair1 = topPair.get(0).split(" ")[0];
        String pair2 = topPair.get(1).split(" ")[0];
        String unMergedPair = String.format("%s %s ", pair1, pair2);
        String mergedPair = String.format("%s%s ", pair1, pair2);

        for (int i = 0; i < tokenizedText.size(); i++) {
            if (tokenizedText.get(i).contains(unMergedPair)) {

                //get count of replacements
                Pattern mergedPattern = Pattern.compile(unMergedPair);
                Matcher mergedMatcher = mergedPattern.matcher(tokenizedText.get(i));

                int replacementCount = (int) mergedMatcher.results().count();

                pair_count.remove(unMergedPair);

                String replacedToken = tokenizedText.get(i).replaceAll(unMergedPair, mergedPair);
                word_count.put(replacedToken, 1 + word_count.getOrDefault(replacedToken, 0));

                word_count.remove(word_count.get(tokenizedText.get(i)));
                tokenizedText.set(i, replacedToken);

                vocab_count.put(mergedPair, vocab_count.getOrDefault(mergedPair, 0) + replacementCount);
            }
        }
    }

    private static Map<String, Integer> countPairs(List<String> tokenizedText, Map<String, Integer> vocab_count,
                                                   List<String> topPair) {
        Map<String, Integer> pair_count = new HashMap<>();


        int maxPair = Integer.MIN_VALUE;

        for (String word : tokenizedText) {
            System.out.println(word);
            String[] splitWord = word.split(" ");

            for (int i = 0; i < splitWord.length - 1; i++) {
                String key = String.format("%s %s ", splitWord[i], splitWord[i + 1]);

                pair_count.put(key,
                        pair_count.getOrDefault(key, 0)
                                + vocab_count.get(splitWord[i] + " ")
                                + vocab_count.get(splitWord[i + 1] + " ")
                );

                if (pair_count.get(key) > maxPair) {
                    maxPair = pair_count.get(key);
                    topPair.clear();
                    topPair.add(splitWord[i] + " ");
                    topPair.add(splitWord[i + 1] + " ");
                }
            }
        }
        ;

        return pair_count;
    }

    private static void addToVocabularyFromText(String doc_gpt, Set<String> vocabulary) {
        doc_gpt.chars().filter(c -> !vocabulary.contains(c)).forEach(c -> vocabulary.add(Character.toString(c) + " "));
    }

    private static void countVocabulary(List<String> tokenizedText, Map<String, Integer> vocab_count, Set<String> ordered_vocabulary) {
        tokenizedText.stream().forEach(w -> {
            String tw = String.copyValueOf(w.toCharArray());

            ordered_vocabulary.stream().forEach(ov -> {
                if (tw.contains(ov)) {
                    vocab_count.put(ov, 1 + vocab_count.getOrDefault(ov, 0));
                    tw.replace(ov, "");
                }
            });
        });
    }

    private static Set<String> reOrderVocabularyByCount(Set<String> vocabulary, Map<String, Integer> vocab_count) {
        Set<String> ordered_vocabulary = new TreeSet<>(
                (a, b) -> {
                    int diff = vocab_count.getOrDefault(b, 0) - vocab_count.getOrDefault(a, 0);
                    if (diff == 0) {
                        return b.compareTo(a);
                    } else {
                        return diff;
                    }
                }
        );

        //order vocabulary by decreasing vocab count
        vocabulary
                .stream()
                .forEach(s -> ordered_vocabulary.add(s));

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
                .map(tok -> tok.word())
                .map(aword -> {
                    StringBuilder spacedWord = new StringBuilder();

                    for (char c : aword.toCharArray()) {
                        spacedWord.append(c).append(" ");
                    }

                    return spacedWord.toString();
                })
                .collect(Collectors.toList());
    }
}
