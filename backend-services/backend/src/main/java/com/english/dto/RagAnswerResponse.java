package com.english.dto;

import java.util.List;

public class RagAnswerResponse {
    private String answer;
    private List<RagSearchItem> references;

    public RagAnswerResponse() {}

    public RagAnswerResponse(String answer, List<RagSearchItem> references) {
        this.answer = answer;
        this.references = references;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<RagSearchItem> getReferences() { return references; }
    public void setReferences(List<RagSearchItem> references) { this.references = references; }
}
