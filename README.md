# RAG Java — Retrieval local con exportación Markdown

Aplicación **Spring Boot** que implementa un pipeline RAG local:

1. Subir **n archivos** (PDF, DOCX, TXT, etc.)
2. Extraer texto con **Apache Tika**
3. Dividir en **chunks** con overlap
4. Generar **embeddings** locales con **AllMiniLM-L6-v2** (LangChain4j, CPU)
5. Buscar por **similitud coseno**
6. Exportar un archivo **Markdown** descargable para usar con cualquier LLM

## Requisitos

- Java 21+ (probado con Java 23)
- Maven 3.9+

## Ejecutar

```bash
cd Projects/rag-java
mvn spring-boot:run
```

La API queda en `http://localhost:8080`.

## Interfaz web

Abre en el navegador:

```
http://localhost:8080
```

Desde la UI puedes:

- Arrastrar o seleccionar **varios archivos** para indexarlos
- Ver la lista de documentos indexados y eliminarlos
- Escribir una consulta y ver una **vista previa** de los fragmentos relevantes
- **Descargar `rag-context.md`** listo para subir a cualquier LLM

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/rag/documents` | Subir archivos (`files` multipart) |
| `GET` | `/api/rag/documents` | Listar documentos indexados |
| `DELETE` | `/api/rag/documents/{documentId}` | Eliminar documento del índice |
| `POST` | `/api/rag/search` | Buscar chunks relevantes (JSON) |
| `POST` | `/api/rag/export` | Descargar `rag-context.md` |
| `GET` | `/api/rag/health` | Estado del índice |

## Ejemplos

### Subir documentos

```bash
curl -X POST http://localhost:8080/api/rag/documents \
  -F "files=@manual.pdf" \
  -F "files=@notas.txt"
```

### Buscar (JSON)

```bash
curl -X POST http://localhost:8080/api/rag/search \
  -H "Content-Type: application/json" \
  -d "{\"query\": \"¿Cómo configurar el sistema?\", \"topK\": 5, \"minScore\": 0.55}"
```

### Descargar Markdown para LLM externo

```bash
curl -X POST http://localhost:8080/api/rag/export \
  -H "Content-Type: application/json" \
  -d "{\"query\": \"Resume los requisitos del proyecto\"}" \
  -o rag-context.md
```

## Configuración (`application.yml`)

```yaml
rag:
  chunk:
    size: 800      # caracteres aprox. por chunk
    overlap: 100   # solapamiento entre chunks
  search:
    default-top-k: 5
    default-min-score: 0.55
```

## Escalabilidad

El almacén actual es **en memoria** (`InMemoryVectorStore`), ideal para desarrollo y cargas pequeñas/medianas.

Para escalar en producción:

- Reemplazar el store por **Qdrant**, **pgvector** o **Weaviate**
- Procesar ingestas de forma **asíncrona** (cola + workers)
- Persistir embeddings y metadatos en disco/BD
- Desplegar múltiples instancias stateless detrás de un balanceador

## Estructura

```
src/main/java/com/rag/
├── controller/     # REST API
├── service/        # Ingesta, chunking, embeddings, búsqueda, export
├── store/          # Vector store (intercambiable)
├── model/          # DTOs
├── config/         # Propiedades y beans
└── util/           # Similitud coseno
```

## Tests

```bash
mvn test
```
