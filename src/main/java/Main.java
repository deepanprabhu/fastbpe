import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Words are from texts
 * Vocabulary are individual chars from words
 */
public class Main {
    //public static final String doc_gpt = "Soviet microbattery (generally, 3 watts) or commercially powered 60 Volt battery of 30 watts will run you between $25-30, if you pay 50 cents per pound for full ownership. The only drawback of lesser battery costs is of course that those who use them end up paying more. Just a reminder: Soviet and government subsidized, solid-state state power stations incurs a 10% mechanical surcharge being now the EU national minimum until around 2030. Right now, all German utilities collect virtually no electricity and are its biggest offenders. Energy resources will certainly not be magically starved out until 2061. Meanwhile, USSR electricity grids that can be developed quickly can be trusted to hold onto most of the industrial power.\\n\\nThere are four different types of parallel grounders (pow-powered), with the main family being the steppe type. Manhattan produced its first light bulb around 100 years ago and Stalin rye maker Ural had its first doubled capacity long after sawing the old white barn in 1928. Then a fusion of hydrogen and uranium captured power at the very lowest electrical teragrams on the grid with nuclear explosives.";
    public static final String doc_gpt = loadText();
    public static String TOKEN_DELIMITER = " ";
    public static String WORD_DELIMITER = "</w>";

    public static String loadText() {
        try {
            return Files.readString(
                    Path.of(Main.class.getClassLoader().getResource("pg16457.txt").toURI())
            );
        } catch (Exception exception){
        }
        return "";
    }
    public static void main(String[] args) {
        Map<String, Integer> pair_count;
        Map<String, Integer> word_count = new HashMap<>();
        Set<String> ordered_vocabulary;
        List<String> topPair = new ArrayList<>();
        List<String> wordsWithSpace;

        int numMerges = 501;

        //Alphabets
        Set<String> vocabulary = Stream.of(
                        IntStream.range('a', 'z' + 1).boxed(),
                        IntStream.range('A', 'Z' + 1).boxed()
                ).reduce(Stream::concat).get()
                .map(c -> Character.toString(c) + TOKEN_DELIMITER)
                .collect(Collectors.toSet());

        wordsWithSpace = getTokenizedWordsWithSpace(doc_gpt);
        addVocabFromText(doc_gpt, vocabulary);

        calculateWordCount(wordsWithSpace, word_count);

        while(numMerges-- > 0){
            countPairsAndFindBest2(word_count, topPair);
            if(topPair.size() > 0) {
                mergePairInVocabulary2(topPair, vocabulary, word_count);
            }
        }

        ordered_vocabulary = reOrderVocabularyByLength(vocabulary);
        tokenizeText("New soviet powered commercial micro battery", ordered_vocabulary);
        tokenizeText("mountains ced however rench pathetic", ordered_vocabulary);
    }

    private static List<String> tokenizeText(String text, Set<String> ordered_vocabulary) {
        List<String> tokenizedText = new ArrayList<>();
        StringBuilder delimitingString = new StringBuilder();
        text
            .chars()
            .mapToObj(c -> (char)c)
            .forEach(c-> delimitingString.append(c).append(TOKEN_DELIMITER));

        String delimitedString = new String(text);

        Map<Integer, String> map = new HashMap<>();

        int index = 0;
        for(String vocab : ordered_vocabulary) {
            if(delimitedString.contains(vocab)){
                //System.out.println(String.format("replacing %s", vocab));
                //Pattern mergedPattern = Pattern.compile(vocab);
                //Matcher mergedMatcher = mergedPattern.matcher(delimitedString);
                //int replacementCount = (int) mergedMatcher.results().count();
                delimitedString = delimitedString.replace(vocab, String.format("|%d|", index));
                map.put(index, vocab);
            }
            index++;
        };

        for(String s: delimitedString.split("\\|")){
            if(!s.trim().isBlank()) {
                try {
                    index = Integer.parseInt(s);
                    tokenizedText.add(map.get(index));
                } catch (NumberFormatException e) {
                    tokenizedText.add(s);
                }
            }
        }
        for(String s : tokenizedText){
            System.out.print("-"+ s +"-");
        }
        return tokenizedText;
    }

    private static void calculateWordCount(List<String> tokenizedText, Map<String, Integer> word_count) {
        word_count.clear();
        tokenizedText
                .stream()
                .forEach(w ->
                        word_count.put(w, 1 + word_count.getOrDefault(w, 0))
                );
    }

    private static void mergePairInVocabulary2(List<String> topPair,
                                              Set<String> vocabulary,
                                              Map<String, Integer> word_count) {
        String pair1 = topPair.get(0);
        String pair2 = topPair.get(1);
        String unMergedPair = String.format("%s%s%s", pair1, TOKEN_DELIMITER, pair2);
        String mergedPair = String.format("%s%s", pair1, pair2);
        Set<String> words = new HashSet<>(word_count.keySet());

        for(String word : words){
            if(word.contains(unMergedPair)){
                //get count of replacements
                //Pattern mergedPattern = Pattern.compile(unMergedPair);
                //Matcher mergedMatcher = mergedPattern.matcher(word);

                String replacedWord = word.replace(unMergedPair, mergedPair);
                word_count.put(replacedWord, word_count.get(word));
                word_count.remove(word);
                vocabulary.add(mergedPair);
            }
        }
    }

    private static Map<String, Integer> countPairsAndFindBest2(Map<String, Integer> word_count,
                                                              List<String> topPair) {
        topPair.clear();
        Map<String, Integer> pair_count = new HashMap<>();
        int maxPair = Integer.MIN_VALUE;

        for(String word : word_count.keySet()) {
            String[] splitWord = word.split(TOKEN_DELIMITER);

            for(int i = 0; i < splitWord.length - 1; i++){
                String key = String.format("%s%s%s", splitWord[i], TOKEN_DELIMITER, splitWord[i + 1]);
                pair_count.put(key, pair_count.getOrDefault(key, 0)
                        + word_count.getOrDefault(word, 0));

                if (pair_count.get(key) > maxPair && pair_count.get(key) > 0) {
                    maxPair = pair_count.get(key);
                    topPair.clear();
                    topPair.add(splitWord[i]);
                    topPair.add(splitWord[i + 1]);
                }
            }

        }

        if(topPair.size() > 0) {
            System.out.println(String.format("Top pair - <>%s|%s<> - %d", topPair.get(0), topPair.get(1), pair_count.get(topPair.get(0) + TOKEN_DELIMITER + topPair.get(1))));
        }
        return pair_count;
    }

    private static void addVocabFromText(String doc_gpt, Set<String> vocabulary) {
        doc_gpt.chars()
            .filter(c -> !vocabulary.contains(c))
            .forEach(c -> vocabulary.add(Character.toString(c) + TOKEN_DELIMITER));
    }

    private static Set<String> reOrderVocabularyByLength(Set<String> vocabulary) {
        List<String> ordered_vocabulary = new ArrayList<>(vocabulary);
        Collections.sort(ordered_vocabulary, (a,b) -> Integer.compare(b.length(), a.length()));
        return new HashSet<>(ordered_vocabulary);
    }

    private static List<String> getTokenizedWordsWithSpace(String doc_gpt) {
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
                        spacedWord.append(c).append(TOKEN_DELIMITER);
                    }

                    spacedWord.append(WORD_DELIMITER);

                    return spacedWord.toString();
                })
                .collect(Collectors.toList());
    }
}
