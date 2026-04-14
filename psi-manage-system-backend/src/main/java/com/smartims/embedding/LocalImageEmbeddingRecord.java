package com.smartims.embedding;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocalImageEmbeddingRecord {

    private String model;
    private String imagesSignature;
    private List<List<Float>> embeddings;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getImagesSignature() {
        return imagesSignature;
    }

    public void setImagesSignature(String imagesSignature) {
        this.imagesSignature = imagesSignature;
    }

    public List<List<Float>> getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(List<List<Float>> embeddings) {
        this.embeddings = embeddings;
    }
}
