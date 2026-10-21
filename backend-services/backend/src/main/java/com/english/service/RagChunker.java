package com.english.service;

import java.util.List;

public interface RagChunker {
    List<String> split(String content);
}
