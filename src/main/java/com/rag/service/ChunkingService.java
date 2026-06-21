package com.rag.service;

import com.rag.config.RagProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ChunkingService {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?\\n])\\s+");

    private final RagProperties properties;

    public ChunkingService(RagProperties properties) {
        this.properties = properties;
    }

    public List<String> split(String text) {
        int chunkSize = properties.chunk().size();
        int overlap = properties.chunk().overlap();

        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text.replace("\r\n", "\n").trim();
        String[] sentences = SENTENCE_BOUNDARY.split(normalized);

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            if (sentence.isBlank()) {
                continue;
            }

            if (current.length() + sentence.length() + 1 <= chunkSize) {
                if (!current.isEmpty()) {
                    current.append(' ');
                }
                current.append(sentence.trim());
                continue;
            }

            if (!current.isEmpty()) {
                chunks.add(current.toString());
                current = new StringBuilder(overlapTail(current.toString(), overlap));
            }

            if (sentence.length() <= chunkSize) {
                current.append(sentence.trim());
            } else {
                chunks.addAll(splitBySize(sentence, chunkSize, overlap));
            }
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }

        return chunks.stream()
                .map(String::trim)
                .filter(chunk -> !chunk.isBlank())
                .toList();
    }

    private List<String> splitBySize(String text, int chunkSize, int overlap) {
        List<String> parts = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            parts.add(text.substring(start, end).trim());
            if (end == text.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }

        return parts;
    }

    private String overlapTail(String chunk, int overlap) {
        if (overlap <= 0 || chunk.length() <= overlap) {
            return "";
        }
        return chunk.substring(chunk.length() - overlap);
    }
}
