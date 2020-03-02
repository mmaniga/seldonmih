package com.seldon.mih.model;

public class TermFrequenceIDF {
    public String term;
    public String doc;
    public Integer count;
    public Double tf;
    public Double tfIDF;

    public TermFrequenceIDF() {

    }

    public TermFrequenceIDF(String term, String doc, Integer count, Double tf, Double tfIDF) {
        this.term = term;
        this.doc = doc;
        this.count = count;
        this.tf = tf;
        this.tfIDF = tfIDF;
    }

    @Override
    public String toString() {
        return "TermFrequenceIDF{" +
                "term='" + term + '\'' +
                ", doc='" + doc + '\'' +
                ", count=" + count +
                ", tf=" + tf +
                ", tfIDF=" + tfIDF +
                '}';
    }
}