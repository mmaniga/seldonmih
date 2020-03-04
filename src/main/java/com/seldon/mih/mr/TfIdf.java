package com.seldon.mih.mr;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Component
public class TfIdf {

    private List<TermFrequenceIDF> termFrequenceIDFS; // TF*IDF , IDF = log(TotalNoOfDocs/NoOfDocsHavingThisWord)

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


    private  Map<String,Integer> getWordFrequency(Path path) throws IOException {
        return Files.lines(path)
                .flatMap(line -> Arrays.stream(line.trim().split("\\s+")))
                .map(word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase().trim())
                .filter(word -> !stopWordsSet.contains(word))
                .filter(word -> word.length() > 0)
                .map(word -> new AbstractMap.SimpleEntry<>(word, 1))
                .collect(toMap(e -> e.getKey(), e -> e.getValue(), (v1, v2) -> v1 + v2));
    }

    private Map<String, Long> getWordFreq2(Path path) throws IOException {
        AtomicInteger x = new AtomicInteger();
        return Files.lines(path)
                .flatMap(line -> Arrays.stream(line.trim().split("\\s+")))
                .map(word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase().trim())
                .filter(word -> !stopWordsSet.contains(word))
                .filter(word -> word.length() > 0)
                .collect(Collectors.groupingBy(Function.identity(),Collectors.counting()));
    }

    private Map<String, Double> getTermFrequency(Map<String,Long> wordFrequency) {
        return wordFrequency.entrySet().stream().collect(toMap(e -> e.getKey(), e -> e.getValue().doubleValue() / wordFrequency.size()));
    }

    public void compute(String dirPath) throws IOException {


        Map<String, Integer> docFreqMap = new ConcurrentHashMap<>(); // count of words in the document space
        Map<String, Map<String, Long>> wordFreqMap = new ConcurrentHashMap<>(); // For each doc have a word frequency map
        Map<String, Map<String, Double>> termFrequencyMap = new ConcurrentHashMap<>(); // For each doc have a TF that is , Frequency of word / total number of words
        termFrequenceIDFS = new ArrayList<>(); // TF*IDF , IDF = log(TotalNoOfDocs/NoOfDocsHavingThisWord)

        List<String> filePaths = getFilePaths(dirPath);

        for (String fp : filePaths) {
            Path path = Paths.get(String.format("%s/%s", dirPath, fp));
            String documentName = path.getFileName().toString();

            Map<String, Long> wordFrequency = getWordFreq2(path);
            wordFreqMap.put(documentName, wordFrequency);
            termFrequencyMap.put(documentName, getTermFrequency(wordFrequency));

            for (Map.Entry<String, Long> e : wordFrequency.entrySet()) {
                docFreqMap.compute(e.getKey(), (k, v) -> {
                    if (k != null && v != null) {
                        return v + 1;
                    }
                    return 1;
                });
            }
        }
        int totalNoOfDocs = filePaths.size();
        Map<String, Double> inverseDocumentFrequency = docFreqMap.entrySet().stream()
                .collect(toMap(e -> e.getKey(), e -> (Math.log(totalNoOfDocs / 1 + e.getValue()))));

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
                termFrequenceIDFS.add(termFrequenceIDF);
            }
        }

        termFrequenceIDFS.sort((o1, o2) -> Double.compare(o1.tfIDF, o2.tfIDF));

        return ;
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
        FileWriter fileWriter = new FileWriter(csvFilePath,true);
        for (TermFrequenceIDF entry : termFrequenceIDFS)
            fileWriter.write(String.format("%s,%s,%d,%f,%f\n",entry.term,entry.doc,entry.count,entry.tf,entry.tfIDF));
        fileWriter.close();
    }
}

