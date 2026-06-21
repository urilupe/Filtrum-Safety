package com.rag.service;

import com.rag.model.SearchResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MarkdownExportService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String buildContextFile(String userQuery, List<SearchResult> results) {
        StringBuilder markdown = new StringBuilder();

        markdown.append("# Contexto RAG\n\n");
        markdown.append("> Generado: ").append(LocalDateTime.now().format(FORMATTER)).append("\n\n");

        markdown.append("## Consulta del usuario\n\n");
        markdown.append(userQuery.trim()).append("\n\n");

        markdown.append("## Fragmentos relevantes\n\n");

        if (results.isEmpty()) {
            markdown.append("_No se encontraron fragmentos con el umbral de similitud configurado._\n\n");
        } else {
            for (int i = 0; i < results.size(); i++) {
                SearchResult result = results.get(i);
                markdown.append("### Fragmento ").append(i + 1)
                        .append(" (similitud: ")
                        .append(String.format("%.4f", result.score()))
                        .append(")\n\n");
                markdown.append("- **Archivo:** ").append(result.chunk().fileName()).append("\n");
                markdown.append("- **Documento ID:** ").append(result.chunk().documentId()).append("\n");
                markdown.append("- **Chunk:** ").append(result.chunk().chunkIndex()).append("\n\n");
                markdown.append("```text\n")
                        .append(result.chunk().content().trim())
                        .append("\n```\n\n");
            }
        }

        markdown.append("---\n\n");
        markdown.append("## Prompt sugerido para LLM externo\n\n");
        markdown.append("""
                Usa únicamente el contexto anterior para responder la consulta del usuario.
                Si la información no está en el contexto, indícalo claramente.
                Cita el nombre del archivo cuando sea posible.
                """);

        return markdown.toString();
    }
}
