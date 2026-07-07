package com.english.dto;

import com.english.entity.Word;

public class WordPracticeItem {
    private Long id;
    private String word;
    private String phonetic;
    private String meaning;
    private String example;
    private String moduleCode;
    private Integer knownCount;

    public static WordPracticeItem from(Word word, int knownCount) {
        WordPracticeItem item = new WordPracticeItem();
        item.setId(word.getId());
        item.setWord(word.getWord());
        item.setPhonetic(word.getPhonetic());
        item.setMeaning(word.getMeaning());
        item.setExample(word.getExample());
        item.setModuleCode(word.getModule() == null ? null : word.getModule().getCode());
        item.setKnownCount(knownCount);
        return item;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getPhonetic() { return phonetic; }
    public void setPhonetic(String phonetic) { this.phonetic = phonetic; }
    public String getMeaning() { return meaning; }
    public void setMeaning(String meaning) { this.meaning = meaning; }
    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }
    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public Integer getKnownCount() { return knownCount; }
    public void setKnownCount(Integer knownCount) { this.knownCount = knownCount; }
}
