package com.seldon.mih.mr;

import com.seldon.mih.model.SentenceTfIDF;
import com.seldon.mih.model.TermFrequenceIDF;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Component
public class TfIdf {

    static String[] stopWords = {
            "I", "me", "my",
            "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself",
            "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are",
            "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as",
            "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to",
            "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all",
            "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can",
            "will", "just", "don", "should", "now",
    };
    private Set<String> stopWordsSet = new HashSet<>(Arrays.asList(stopWords));

    private List<TermFrequenceIDF> tfIDF; // TF*IDF , IDF = log(TotalNoOfDocs/NoOfDocsHavingThisWord)
    private List<SentenceTfIDF> sentenceTfIDFlist;
    private Map<String, List<Map.Entry<String, Double>>> term2docMap;

    private Map<String, Long> getWordFrequency(Path path) throws IOException {
        return Files.lines(path)
                .flatMap(line -> Arrays.stream(line.trim().split("\\s+")))
                .filter(word -> word.length() > 1)
                .map(word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase().trim())
                .filter(word -> !stopWordsSet.contains(word))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }


    public void compute2(String dirPath) throws IOException {
        System.out.println(dirPath);
        List<String> filePaths = getFilePaths(dirPath);
        Map<String, Integer> docFreqMap = new ConcurrentHashMap<>(); // count of words in the document space
        Map<String, Map<String, Long>> wordFreqMap = new ConcurrentHashMap<>(); // For each doc have a word frequency map
        Map<String, Map<String, Double>> termFrequencyMap = new ConcurrentHashMap<>(); // For each doc have a TF that is , Frequency of word / total number of words
        Map<String, Map<String,Double>> sentenceFrequencyMap = new ConcurrentHashMap<>(); // for each sentence in the doc
        Map<String,Double> sentenceInverseDocFrequency = new ConcurrentHashMap<>();
        Map<String, List<String>> docSentenceMap = new ConcurrentHashMap<>();

        tfIDF = new ArrayList<>(); // TF*IDF , IDF = log(TotalNoOfDocs/NoOfDocsHavingThisWord)
        term2docMap = new ConcurrentHashMap<>();

        for (String fp : filePaths) {
            Path path = Paths.get(String.format("%s/%s", dirPath, fp));
            String documentName = path.getFileName().toString();

            // Strip sentence as first step
            List<String> sentencesList = new ArrayList<>();
            for(String ss : Files.readAllLines(path)) {
                String ls = Arrays.stream(ss.trim().split("\\s+"))
                        .filter(word -> word.length() > 1)
                        .map(word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase().trim())
                        .filter(word -> !stopWordsSet.contains(word)).collect(Collectors.joining(" "));
                System.out.println("List --> "+ls);
                sentencesList.add(ls);
            }
            //List<String> sentencesList = Files.lines(path)
            //        .flatMap(line -> Arrays.stream(line.trim().split("\\.|\\?|\\!"))).collect(Collectors.toList());

            docSentenceMap.put(documentName,sentencesList);
            for(String  see : sentencesList)
                System.out.println(" >  "+see);

            //Computer word frequency for this doc
            Map<String, Long> wordFrequency = sentencesList.stream().flatMap(line -> Arrays.stream(line.trim().split("\\s+")))
                    .filter(word -> word.length() > 1)
                    .map(word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase().trim())
                    .filter(word -> !stopWordsSet.contains(word))
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            // Compute TermFrequency for this doc
            Map<String, Double> termFrequency = wordFrequency.entrySet().stream()
                    .collect(toMap(e -> e.getKey(), e -> e.getValue().doubleValue() / wordFrequency.size()));


            // Update document frequency
            // Update no of documents this word is present, for all words, increase the count in
            // docFreMap , because its found in this document.
           /*
            wordFrequency.entrySet().stream().peek(e ->
                    docFreqMap.compute(e.getKey(), (k, v) -> {
                        if (k != null && v != null) {
                            return v + 1; // If the word is found already, increment by 1 that is this document
                        }
                        return 1; // If first time set count = 1
                    }));
*/
            for (Map.Entry<String, Long> e : wordFrequency.entrySet()) {
                docFreqMap.compute(e.getKey(), (k, v) -> {
                    if (k != null && v != null) {
                        return v + 1; // If the word is found already, increment by 1 that is this document
                    }
                    return 1; // If first time set count = 1
                });
            }

            // Compute Sentence termFrequency
            Map<String, Double> sentenceTermFrequency = new ConcurrentHashMap<>();
            for (String sentence : sentencesList) {
                //AtomicInteger validWords = new AtomicInteger(0);
                double sentenceTF = Arrays.stream(sentence.trim().split("\\s+"))
                        .filter(word -> word.length() > 1)
                        .map(word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase().trim())
                        .filter(word -> !stopWordsSet.contains(word))
                        .mapToDouble(f -> wordFrequency.get(f)).sum() / wordFrequency.size(); //validWords.get();

                        //.peek(e -> validWords.getAndIncrement()) // This is hack
                        //.mapToDouble(f -> termFrequency.get(f)).sum() / wordFrequency.size(); //validWords.get();
                sentenceTermFrequency.put(sentence, sentenceTF);
            }


            // Final data aggregation
            wordFreqMap.put(documentName, wordFrequency);
            termFrequencyMap.put(documentName, termFrequency);
            sentenceFrequencyMap.put(documentName, sentenceTermFrequency);

        }

        // Compute IDF
        // Total No of words occurance / no of documents this word is present

        int totalNoOfDocs = filePaths.size();
        Map<String, Double> inverseDocumentFrequency = docFreqMap.entrySet().stream()
                .collect(toMap(e -> e.getKey(), e -> (Math.log(totalNoOfDocs / 1 + e.getValue()))));

        // Compute sentence IDF

       // Map<String,Double> sentenceInverseDocFrequency = docSentenceMap.entrySet().stream()
       //         .collect(toMap(e->e.getKey(),
       //                 e -> e.getValue().stream().mapToDouble(f ->inverseDocumentFrequency.get(f)).sum()/e.getValue().size()));


        for(Map.Entry<String,List<String>> de : docSentenceMap.entrySet()) {
            for (String s : de.getValue()) {
                System.out.println("<<-->> " + s);
                double sum =0;
                for(String sss :s.split("\\s+")) {
                    System.out.println("Val -> " + sss + " :" + inverseDocumentFrequency.get(sss));
                    Double idfVal = inverseDocumentFrequency.get(sss);
                    sum+=idfVal==null?0:idfVal;
                }
                double idfSentence = sum / de.getValue().size(); // get idf from idf map
                sentenceInverseDocFrequency.put(s, idfSentence);
            }
        }

        calculateTfIdf(termFrequencyMap, wordFreqMap, inverseDocumentFrequency);
        tfIDF.sort((o1, o2) -> Double.compare(o1.tfIDF, o2.tfIDF));

        // Calculate tf*idf of sentence
        sentenceTfIDFlist = new ArrayList<>();
        for(Map.Entry<String,Map<String,Double>> entry : sentenceFrequencyMap.entrySet()) {
            String doc = entry.getKey();
            for (Map.Entry<String, Double> ie : entry.getValue().entrySet()) {
                SentenceTfIDF sentenceTfIDF = new SentenceTfIDF();
                sentenceTfIDF.doc = doc;
                sentenceTfIDF.sentence = ie.getKey();
                System.out.println("s-key--> "+ie.getKey());
                System.out.println("s-val--> "+ie.getValue());
                sentenceTfIDF.tfidf = ie.getValue() * sentenceInverseDocFrequency.get(ie.getKey());
                System.out.println("s-tdidf--> "+sentenceTfIDF.tfidf);
                sentenceTfIDFlist.add(sentenceTfIDF);
            }
        }
        sentenceTfIDFlist.sort((o1,o2) -> Double.compare(o1.tfidf,o2.tfidf));



    }

    private Map<String, Double> getTermFrequency(Map<String, Long> wordFrequency) {
        return wordFrequency.entrySet().stream().collect(toMap(e -> e.getKey(), e -> e.getValue().doubleValue() / wordFrequency.size()));
    }

    private void calculateTfIdf(Map<String, Map<String, Double>> termFrequencyMap,
                                Map<String, Map<String, Long>> wordFreqMap,
                                Map<String, Double> inverseDocumentFrequency) {
        // Generate the overall Data we need
        for (Map.Entry<String, Map<String, Double>> entry : termFrequencyMap.entrySet()) {
            String doc = entry.getKey();
            for (Map.Entry<String, Double> ie : entry.getValue().entrySet()) {
                TermFrequenceIDF termFrequenceIDF = new TermFrequenceIDF();
                termFrequenceIDF.doc = doc;
                termFrequenceIDF.term = ie.getKey();
                termFrequenceIDF.count = wordFreqMap.get(termFrequenceIDF.doc).get(termFrequenceIDF.term);
                termFrequenceIDF.tf = ie.getValue();
                termFrequenceIDF.tfIDF = termFrequenceIDF.tf * inverseDocumentFrequency.get(termFrequenceIDF.term);
                tfIDF.add(termFrequenceIDF);
                DocEntry docEntry = new DocEntry(termFrequenceIDF.doc, termFrequenceIDF.tfIDF);
                if (term2docMap.containsKey(termFrequenceIDF.term)) {
                    term2docMap.get(termFrequenceIDF.term).add(docEntry);
                } else {
                    List<Map.Entry<String, Double>> d = new ArrayList<>();
                    d.add(docEntry);
                    term2docMap.put(termFrequenceIDF.term, d);
                }

            }
        }
    }

    public void compute(String dirPath) throws IOException {

        Map<String, Integer> docFreqMap = new ConcurrentHashMap<>(); // count of words in the document space
        Map<String, Map<String, Long>> wordFreqMap = new ConcurrentHashMap<>(); // For each doc have a word frequency map
        Map<String, Map<String, Double>> termFrequencyMap = new ConcurrentHashMap<>(); // For each doc have a TF that is , Frequency of word / total number of words
        tfIDF = new ArrayList<>(); // TF*IDF , IDF = log(TotalNoOfDocs/NoOfDocsHavingThisWord)
        term2docMap = new ConcurrentHashMap<>();

        System.out.println(dirPath);
        List<String> filePaths = getFilePaths(dirPath);

        for (String fp : filePaths) {
            Path path = Paths.get(String.format("%s/%s", dirPath, fp));
            String documentName = path.getFileName().toString();

            Map<String, Long> wordFrequency = getWordFrequency(path);
            wordFreqMap.put(documentName, wordFrequency);
            termFrequencyMap.put(documentName, getTermFrequency(wordFrequency));

            // Update no of documents this word is present, for all words, increase the count in
            // docFreMap , because its found in this document.
            for (Map.Entry<String, Long> e : wordFrequency.entrySet()) {
                docFreqMap.compute(e.getKey(), (k, v) -> {
                    if (k != null && v != null) {
                        return v + 1; // If the word is found already, increment by 1 that is this document
                    }
                    return 1; // If first time set count = 1
                });
            }
        }
        int totalNoOfDocs = filePaths.size();
        Map<String, Double> inverseDocumentFrequency = docFreqMap.entrySet().stream()
                .collect(toMap(e -> e.getKey(), e -> (Math.log(totalNoOfDocs / 1 + e.getValue()))));

        calculateTfIdf(termFrequencyMap, wordFreqMap, inverseDocumentFrequency);
        tfIDF.sort((o1, o2) -> Double.compare(o1.tfIDF, o2.tfIDF));
        return;
    }

    private List<String> getFilePaths(String dirPath) {
        List<String> fileNames = new ArrayList<>();
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(dirPath));
            int fileCounter = 0;
            for (Path path : directoryStream) {
                System.out.println(path.getFileName());
                fileCounter++;
                fileNames.add(path.getFileName().toString());
            }
        } catch (IOException ex) {
        }
        System.out.println("Count: " + fileNames.size() + " files");
        return fileNames;
    }

    public void toCSV(String csvFilePath) throws IOException {
        FileWriter fileWriter = new FileWriter(String.format("%s/%s", csvFilePath, "/tfidf.csv"), true);
        for (TermFrequenceIDF entry : tfIDF)
            fileWriter.write(String.format("%s,%s,%d,%f,%f\n", entry.term, entry.doc, entry.count, entry.tf, entry.tfIDF));
        fileWriter.close();

        FileWriter fileWriter2 = new FileWriter(String.format("%s/%s", csvFilePath, "/term2Doc.csv"), true);
        for (Map.Entry<String, List<Map.Entry<String, Double>>> e : term2docMap.entrySet()) {
            List<String> s = new ArrayList<>(); // re-facror this nasry one
            for (Map.Entry<String, Double> x : e.getValue()) {
                s.add(x.getKey());
            }
            fileWriter2.write(String.format("%s,[%s]\n", e.getKey(), s.stream().collect(Collectors.joining(","))));
        }

        FileWriter fileWriter3 = new FileWriter(String.format("%s/%s",csvFilePath,"/senternceTfIdf.csv"));
        for(SentenceTfIDF e  : sentenceTfIDFlist)
            fileWriter3.write(String.format("%s,%s,%f\n",e.sentence,e.doc,e.tfidf));
        fileWriter3.close();

    }

    final class DocEntry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private V value;

        public DocEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V old = this.value;
            this.value = value;
            return old;
        }
    }

}

