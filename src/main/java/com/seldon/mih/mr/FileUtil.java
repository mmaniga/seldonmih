package com.seldon.mih.mr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.stream.Collectors.toMap;

public class FileUtil {

    public static Map<String,Integer> toFrequencyMap(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Map<String, Integer> wordCount = Files.lines(path).flatMap(line -> Arrays.stream(line.trim().split("\\s+")))
                .map(word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase().trim())
                .filter(word -> word.length() > 0)
                .map(word -> new AbstractMap.SimpleEntry<>(word, 1))
                .collect(toMap(e -> e.getKey(), e -> e.getValue(), (v1, v2) -> v1 + v2));

        return wordCount;
    }

    public static void printFrequency(Map<String,Integer> wordCount) {
        wordCount.forEach((k, v) -> System.out.println(String.format("%s ==>> %d", k, v)));
    }


    public static List<String> getFilePaths() {
        List<String> fileNames = new ArrayList<>();
        return fileNames;
    }
}
