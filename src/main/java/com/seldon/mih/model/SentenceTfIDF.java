package com.seldon.mih.model;

public class SentenceTfIDF {
    public String sentence;
    public String doc;
    public double tfidf;

    public SentenceTfIDF() {
    }

    public SentenceTfIDF(String sentence, String doc, double tfidf) {
        this.sentence = sentence;
        this.doc = doc;
        this.tfidf = tfidf;
    }
}
