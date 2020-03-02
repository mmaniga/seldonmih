package com.seldon.mih.mr;

import com.seldon.mih.model.TermFrequenceIDF;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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



    /*
     log10( no of documents/ 1+number of documents having this word )
     1 added to get away from divide by zero
     */
    public static Map<String,Double> inverseDocumentFrequency = new ConcurrentHashMap<>();

    /*
     This is a global count of words, for each words, this contains how many documents has the word
     This is not the count of occurrence, its count of how many documents has this word.
     */
    public static Map<String,Integer> docFreqMap = new ConcurrentHashMap<>();

    /*
     For each file, have a map of its word count that is local to that file.
     */
    public static Map<String,Map<String,Integer>> wordFreqMap = new ConcurrentHashMap<>();

    /*
      For each file, have a map of TermFrequency that is #count of Word / total number of words in the document
     */
    public static Map<String,Map<String,Double>> termFrequencyMap = new ConcurrentHashMap<>();

    public static List<TermFrequenceIDF> tdIDFVector = new ArrayList<>();

    public static void toFrequencyMap(String dirPath) throws IOException {
        List<String> filePaths = FileUtil.getFilePaths(dirPath);
        for(String fp : filePaths) {
            Path path = Paths.get(String.format("%s/%s",dirPath,fp));
            System.out.println(path);
            Map<String, Integer> wordCount = Files.lines(path)
                    .flatMap(line -> Arrays.stream(line.trim().split("\\s+")))
                    .map(word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase().trim())
                    .filter(word -> !stopWordsSet.contains(word))
                    .filter(word -> word.length() > 0)
                    .map(word -> new AbstractMap.SimpleEntry<>(word, 1))
                    .collect(toMap(e -> e.getKey(), e -> e.getValue(), (v1, v2) -> v1 + v2));

            wordFreqMap.put(path.getFileName().toString(), wordCount);
            termFrequencyMap.put(path.getFileName().toString(),
                    wordCount.entrySet().stream().collect(toMap(e -> e.getKey(), e -> e.getValue().doubleValue() /wordCount.size())));

            for (Map.Entry<String, Integer> e : wordCount.entrySet()) {
                docFreqMap.compute(e.getKey(), (k, v) -> {
                    if (k != null && v!=null) {
                        return v + 1;
                    }
                    return 1;
                });

            }
        }
        int totalNoOfDocs = filePaths.size();
        inverseDocumentFrequency = docFreqMap.entrySet().stream().collect(toMap(e->e.getKey(),e->(Math.log(totalNoOfDocs/1+e.getValue()))));

        // Generate the overall Data we need
        for(Map.Entry<String,Map<String,Double>> entry : termFrequencyMap.entrySet()) {
            String doc = entry.getKey();
            for(Map.Entry<String,Double> ie : entry.getValue().entrySet()) {
                TermFrequenceIDF termFrequenceIDF = new TermFrequenceIDF();
                termFrequenceIDF.doc = doc;
                termFrequenceIDF.term = ie.getKey();
                termFrequenceIDF.count = wordFreqMap.get(termFrequenceIDF.doc).get(termFrequenceIDF.term);
                termFrequenceIDF.tf = ie.getValue();
                termFrequenceIDF.tfIDF=termFrequenceIDF.tf*inverseDocumentFrequency.get(termFrequenceIDF.term);
                tdIDFVector.add(termFrequenceIDF);
            }
        }

        tdIDFVector.sort((o1, o2) -> Double.compare(o1.tfIDF,o2.tfIDF));

        }

/*


    public static Map<String, Double> toTFMap(String filePath) IOException{
        Map<String,Integer> fqMap = toFrequencyMap(filePath);
        return fqMap.entrySet().stream().collect(toMap(e -> e.getKey(),e -> e.getValue().doubleValue()/fqMap.size()));
    }
*/
public static int countOfLinesInFile(String filePath) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(filePath));
    AtomicInteger lines = new AtomicInteger();
    while (reader.readLine() != null) lines.getAndIncrement();
    reader.close();
    return lines.get();
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

