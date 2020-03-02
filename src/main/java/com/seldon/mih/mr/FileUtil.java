package com.seldon.mih.mr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toMap;

public class FileUtil {

    public static  void toFrequencyCSV(String sourceFile) {
        Path path = Paths.get(sourceFile);
        System.out.println(path.getFileName());
    }

    public static void writeHashMapToCsv(String filePath, Map<String, Double> freqMap) throws Exception {
        FileWriter fileWriter = new FileWriter(filePath,true);
        for (Map.Entry<String, Double> entry : freqMap.entrySet())
            fileWriter.write(String.format("%s,%s\n",entry.getKey(),entry.getValue()));
        fileWriter.close();
    }

    static String[] stopWords = {
            "I","me", "my",
            "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself",
            "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are",
            "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as",
            "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to",
            "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all",
            "any", "both", "each", "few", "more", "most","other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can",
            "will", "just", "don", "should", "now",
    };

    static Set<String > stopWordsSet = new HashSet<>(Arrays.asList(stopWords));


    public static Map<String,Integer> toFrequencyMap(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Map<String, Integer> wordCount = Files.lines(path)
                .flatMap(line -> Arrays.stream(line.trim().split("\\s+")))
                .map(word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase().trim())
                .filter(word -> !stopWordsSet.contains(word))
                .filter(word -> word.length() > 0)
                .map(word -> new AbstractMap.SimpleEntry<>(word, 1))
                .collect(toMap(e -> e.getKey(), e -> e.getValue(), (v1, v2) -> v1 + v2));

        return wordCount;
    }

    public static int countOfLinesInFile(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        AtomicInteger lines = new AtomicInteger();
        while (reader.readLine() != null) lines.getAndIncrement();
        reader.close();
        return lines.get();
    }

    public static Map<String, Double> toTFMap(String filePath) throws  IOException {
        Map<String,Integer> fqMap = toFrequencyMap(filePath);
        return fqMap.entrySet().stream().collect(toMap(e -> e.getKey(),e -> e.getValue().doubleValue()/fqMap.size()));
    }

    public static Map<String,Double> toTermFrequencyMap(String filePath) throws IOException {
        int noOfWordsInDoc = countOfLinesInFile(filePath);
        Path path = Paths.get(filePath);
        Map<String, Double> wordCount = Files.lines(path)
                .map(str -> str.split(","))
                .collect(toMap(str -> str[0],str-> Integer.getInteger(str[1]).doubleValue() / noOfWordsInDoc));

        return wordCount;
    }

    public static void printFrequency(Map<String,Integer> wordCount) {
        wordCount.forEach((k, v) -> System.out.println(String.format("%s ==>> %d", k, v)));
    }


    public static List<String> getFilePaths(String dirPath) {
        List<String> fileNames = new ArrayList<>();
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(dirPath));
            int fileCounter = 0;
            for (Path path : directoryStream) {
                System.out.println(path.getFileName());
                fileCounter++;
                fileNames.add(path.getFileName().toString());
            }
        } catch(IOException ex){
        }
        System.out.println("Count: "+fileNames.size()+ " files");
        return fileNames;
    }
}
