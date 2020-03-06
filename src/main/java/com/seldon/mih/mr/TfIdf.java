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

    // Since we use IDF we could be okay without stop word, need to reach about the math
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
    private List<SentenceTfIDF> sentenceTfIdfList;
    private Map<String, List<Map.Entry<String, Double>>> term2docMap;
    private Map<String, List<String>> docSentenceMap ;

    public TfIdf() {
        tfIDF = new ArrayList<>(); // TF*IDF , IDF = log(TotalNoOfDocs/NoOfDocsHavingThisWord)
        term2docMap = new ConcurrentHashMap<>();
        docSentenceMap = new ConcurrentHashMap<>();
    }

    public void compute(String dirPath) throws IOException {
        System.out.printf("Processing file %s \n",dirPath);

        Map<String, Integer> docFreqMap = new ConcurrentHashMap<>(); // count of words in the document space
        Map<String, Map<String, Long>> wordFreqMap = new ConcurrentHashMap<>(); // For each doc have a word frequency map
        Map<String, Map<String, Double>> termFrequencyMap = new ConcurrentHashMap<>(); // For each doc have a TF that is , Frequency of word / total number of words
        Map<String, Map<String,Double>> sentenceFrequencyMap = new ConcurrentHashMap<>(); // for each sentence in the doc
        Map<String,Double> sentenceInverseDocFrequency = new ConcurrentHashMap<>();


        List<String> filePaths = getFilePaths(dirPath);
        for (String fp : filePaths) {
            Path path = Paths.get(String.format("%s/%s", dirPath, fp));
            String documentName = path.getFileName().toString();

            // Step -1 Strip sentence as first step
            System.out.printf("Stripping sentence in doc  -> %s \n ",documentName);
            List<String> sentencesList = new ArrayList<>();
            for(String ss : Files.readAllLines(path)) {
                String ls = Arrays.stream(ss.trim().split("\\s+"))
                        .filter(word -> word.length() > 1)
                        .map(word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase().trim())
                        .filter(word -> !stopWordsSet.contains(word))
                        .collect(Collectors.joining(" "));
                sentencesList.add(ls);
                System.out.printf("Sentence -> %s \n ",ls);
            }
            docSentenceMap.put(documentName,sentencesList);

            //Step -2 Computer word frequency for this doc
            Map<String, Long> wordFrequency = sentencesList.stream()
                    .flatMap(line -> Arrays.stream(line.trim().split("\\s+")))
                    .filter(word -> word.length() > 1)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            //Step -3 Compute Term Frequency for this doc
            Map<String, Double> termFrequency = wordFrequency.entrySet().stream()
                    .collect(toMap(e -> e.getKey(), e -> e.getValue().doubleValue() / wordFrequency.size()));


            //Step -4 Compute Document level frequency of each word in this document
            //docFreqMap is at global scope, that is, for all documents.
            for (Map.Entry<String, Long> e : wordFrequency.entrySet()) {
                docFreqMap.compute(e.getKey(), (k, v) -> {
                    if (k != null && v != null) {
                        return v + 1; // If the word is found already, increment by 1 that is this document
                    }
                    return 1; // If first time set count = 1
                });
            }

            //Step -5 Compute Sentence's term frequency
            //Logic is simple
            //Sum the term frequency of each word in sentence and divide by no of words in the doc
            Map<String, Double> sentenceTermFrequency = new ConcurrentHashMap<>();
            for (String sentence : sentencesList) {
                double sentenceTF = Arrays.stream(sentence.trim().split("\\s+"))
                        // Intentionally keeping this as sentence might have and could have stop words
                        // In current code i am stripping, i am not sure if that would be limiting
                        .filter(word -> word.length() > 1)
                        .map(word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase().trim())
                        .filter(word -> !stopWordsSet.contains(word))
                        .mapToDouble(f -> wordFrequency.get(f)).sum() / wordFrequency.size();
                sentenceTermFrequency.put(sentence, sentenceTF);
            }

            //Step -6 Intermediate data  aggregation
            wordFreqMap.put(documentName, wordFrequency);
            termFrequencyMap.put(documentName, termFrequency);
            sentenceFrequencyMap.put(documentName, sentenceTermFrequency);

        }

        //Step -7 Compute Term IDF
        // log(Total No of words occurrence / 1 + no of documents this word is present)

        int totalNoOfDocs = filePaths.size();
        Map<String, Double> inverseDocumentFrequency = docFreqMap.entrySet().stream()
                .collect(toMap(e -> e.getKey(), e -> (Math.log(totalNoOfDocs / 1 + e.getValue()))));

        //Step -8 Compute Sentence IDF
        for(Map.Entry<String,List<String>> docSentences : docSentenceMap.entrySet()) {
            for (String sentence : docSentences.getValue()) {
                double sum =0;
                for(String sWord :sentence.split("\\s+")) {
                    Double idfVal = inverseDocumentFrequency.get(sWord);
                    sum+=idfVal==null?0:idfVal;
                }
                double idfSentence = sum / docSentences.getValue().size(); // get idf from idf map
                sentenceInverseDocFrequency.put(sentence, idfSentence);
            }
        }

        //Step -9 Compute Term Frequency * Inverse Document Frequency
        calculateTfIdf(termFrequencyMap, wordFreqMap, inverseDocumentFrequency);

        //Step 10 Sort the List in descending order
        tfIDF.sort((o1, o2) -> Double.compare(o1.tfIDF, o2.tfIDF)*-1); // Hack for descending order - Fix latter

        //Step -11  Calculate tf*idf of sentence
        sentenceTfIdfList = new ArrayList<>();
        for(Map.Entry<String,Map<String,Double>> entry : sentenceFrequencyMap.entrySet()) {
            String doc = entry.getKey();
            for (Map.Entry<String, Double> ie : entry.getValue().entrySet()) {
                SentenceTfIDF sentenceTfIDF = new SentenceTfIDF();
                sentenceTfIDF.doc = doc;
                sentenceTfIDF.sentence = ie.getKey();
                sentenceTfIDF.tfidf = ie.getValue() * sentenceInverseDocFrequency.get(ie.getKey());
                sentenceTfIdfList.add(sentenceTfIDF);
            }
        }
        sentenceTfIdfList.sort((o1, o2) -> Double.compare(o1.tfidf,o2.tfidf)*-1); // Hack for descending order - Fix latter
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
        fileWriter.write(String.format("%s,%s,%s,%s,%s\n", "term", "doc", "count", "tf", "tfIdf"));
        for (TermFrequenceIDF entry : tfIDF)
            fileWriter.write(String.format("%s,%s,%d,%f,%f\n", entry.term, entry.doc, entry.count, entry.tf, entry.tfIDF));
        fileWriter.close();

        FileWriter fileWriter2 = new FileWriter(String.format("%s/%s", csvFilePath, "/term2Doc.csv"), true);
        fileWriter2.write(String.format("%s,[%s]\n", "doc", "terms"));

        for (Map.Entry<String, List<Map.Entry<String, Double>>> e : term2docMap.entrySet()) {
            List<String> s = new ArrayList<>(); // re-factor this nasty one
            for (Map.Entry<String, Double> x : e.getValue()) {
                s.add(x.getKey());
            }
            fileWriter2.write(String.format("%s,[%s]\n", e.getKey(), s.stream().collect(Collectors.joining(","))));
        }

        FileWriter fileWriter3 = new FileWriter(String.format("%s/%s",csvFilePath,"/senternceTfIdf.csv"));
        for(SentenceTfIDF e  : sentenceTfIdfList)
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

