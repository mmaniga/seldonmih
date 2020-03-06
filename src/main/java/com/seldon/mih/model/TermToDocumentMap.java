package com.seldon.mih.model;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TermToDocumentMap {
    private String term;
    private List<Map.Entry<String, Double>> tfIdList;  // This is a list of documents sorted by tfidf
    // private Map<String,List<String>> docSentence; // sentences sorted by tfidf derived


    public TermToDocumentMap() {
        tfIdList = new ArrayList<>();
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public List<Map.Entry<String, Double>> getTfIdList() {
        return tfIdList;
    }

    public void setTfIdList(List<Map.Entry<String, Double>> tfIdList) {
        this.tfIdList = tfIdList;
    }

    public void addDocEntry(Map.Entry<String, Double> docEntry) {
        tfIdList.add(docEntry);
    }

}
