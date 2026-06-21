const API = "/api/rag";

const dropzone = document.getElementById("dropzone");
const fileInput = document.getElementById("fileInput");
const browseBtn = document.getElementById("browseBtn");
const selectedFiles = document.getElementById("selectedFiles");
const uploadBtn = document.getElementById("uploadBtn");
const clearFilesBtn = document.getElementById("clearFilesBtn");
const uploadAlert = document.getElementById("uploadAlert");
const documentsBody = document.getElementById("documentsBody");
const refreshDocsBtn = document.getElementById("refreshDocsBtn");
const healthStats = document.getElementById("healthStats");
const queryInput = document.getElementById("queryInput");
const topKInput = document.getElementById("topKInput");
const minScoreInput = document.getElementById("minScoreInput");
const searchBtn = document.getElementById("searchBtn");
const exportBtn = document.getElementById("exportBtn");
const searchAlert = document.getElementById("searchAlert");
const resultsPanel = document.getElementById("resultsPanel");
const resultsList = document.getElementById("resultsList");
const toast = document.getElementById("toast");

let pendingFiles = [];

function showToast(message) {
    toast.textContent = message;
    toast.hidden = false;
    clearTimeout(showToast._timer);
    showToast._timer = setTimeout(() => {
        toast.hidden = true;
    }, 3200);
}

function showAlert(element, message, type) {
    element.textContent = message;
    element.className = `alert alert--${type}`;
    element.hidden = false;
}

function hideAlert(element) {
    element.hidden = true;
}

function renderSelectedFiles() {
    selectedFiles.innerHTML = "";

    if (pendingFiles.length === 0) {
        uploadBtn.disabled = true;
        clearFilesBtn.disabled = true;
        return;
    }

    pendingFiles.forEach((file, index) => {
        const item = document.createElement("li");
        item.innerHTML = `
            <span>${file.name}</span>
            <span>${formatBytes(file.size)}</span>
        `;
        item.title = file.name;
        selectedFiles.appendChild(item);
    });

    uploadBtn.disabled = false;
    clearFilesBtn.disabled = false;
}

function formatBytes(bytes) {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function addFiles(fileList) {
    const incoming = Array.from(fileList);
    pendingFiles = [...pendingFiles, ...incoming];
    renderSelectedFiles();
}

async function fetchJson(url, options = {}) {
    const response = await fetch(url, options);
    if (!response.ok) {
        let message = `Error ${response.status}`;
        try {
            const body = await response.json();
            if (body.error) message = body.error;
        } catch (_) {
            /* ignore */
        }
        throw new Error(message);
    }
    if (response.status === 204) return null;
    return response.json();
}

async function loadHealth() {
    try {
        const data = await fetchJson(`${API}/health`);
        healthStats.innerHTML = `
            <span class="stat">${data.documents} documentos</span>
            <span class="stat">${data.chunksIndexed} chunks</span>
        `;
    } catch (error) {
        healthStats.innerHTML = `<span class="stat">API no disponible</span>`;
    }
}

async function loadDocuments() {
    try {
        const documents = await fetchJson(`${API}/documents`);
        documentsBody.innerHTML = "";

        if (documents.length === 0) {
            documentsBody.innerHTML = `<tr><td colspan="3" class="empty">No hay documentos indexados</td></tr>`;
            return;
        }

        documents.forEach((doc) => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${escapeHtml(doc.fileName)}</td>
                <td>${doc.chunkCount}</td>
                <td><button class="btn btn--danger" data-id="${doc.documentId}">Eliminar</button></td>
            `;
            documentsBody.appendChild(row);
        });
    } catch (error) {
        showToast(error.message);
    }
}

function escapeHtml(value) {
    return value
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function getSearchPayload() {
    const query = queryInput.value.trim();
    if (!query) {
        throw new Error("Escribe una consulta antes de continuar");
    }

    return {
        query,
        topK: Number(topKInput.value) || 5,
        minScore: Number(minScoreInput.value) || 0.55
    };
}

async function uploadDocuments() {
    if (pendingFiles.length === 0) return;

    uploadBtn.disabled = true;
    hideAlert(uploadAlert);

    const formData = new FormData();
    pendingFiles.forEach((file) => formData.append("files", file));

    try {
        const response = await fetch(`${API}/documents`, {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            let message = `Error ${response.status}`;
            try {
                const body = await response.json();
                if (body.error) message = body.error;
            } catch (_) {
                /* ignore */
            }
            throw new Error(message);
        }

        const results = await response.json();
        const totalChunks = results.reduce((sum, item) => sum + item.chunksCreated, 0);
        showAlert(
            uploadAlert,
            `Indexados ${results.length} archivo(s) — ${totalChunks} chunks creados`,
            "success"
        );

        pendingFiles = [];
        renderSelectedFiles();
        await Promise.all([loadDocuments(), loadHealth()]);
        showToast("Documentos indexados correctamente");
    } catch (error) {
        showAlert(uploadAlert, error.message, "error");
    } finally {
        uploadBtn.disabled = pendingFiles.length === 0;
    }
}

async function previewSearch() {
    hideAlert(searchAlert);
    resultsPanel.hidden = true;

    try {
        const payload = getSearchPayload();
        searchBtn.disabled = true;

        const results = await fetchJson(`${API}/search`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (results.length === 0) {
            showAlert(searchAlert, "No se encontraron fragmentos con el umbral configurado", "error");
            return;
        }

        resultsList.innerHTML = results.map((result, index) => `
            <article class="result-item">
                <div class="result-item__meta">
                    <span class="result-item__score">Similitud: ${result.score.toFixed(4)}</span>
                    <span>Archivo: ${escapeHtml(result.fileName)}</span>
                    <span>Chunk: ${result.chunkIndex}</span>
                </div>
                <pre>${escapeHtml(result.content)}</pre>
            </article>
        `).join("");

        resultsPanel.hidden = false;
        showAlert(searchAlert, `${results.length} fragmento(s) encontrado(s)`, "success");
    } catch (error) {
        showAlert(searchAlert, error.message, "error");
    } finally {
        searchBtn.disabled = false;
    }
}

async function downloadMarkdown() {
    hideAlert(searchAlert);

    try {
        const payload = getSearchPayload();
        exportBtn.disabled = true;

        const response = await fetch(`${API}/export`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            let message = `Error ${response.status}`;
            try {
                const body = await response.json();
                if (body.error) message = body.error;
            } catch (_) {
                /* ignore */
            }
            throw new Error(message);
        }

        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = "rag-context.md";
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);

        showAlert(searchAlert, "Markdown descargado correctamente", "success");
        showToast("Descarga iniciada: rag-context.md");
    } catch (error) {
        showAlert(searchAlert, error.message, "error");
    } finally {
        exportBtn.disabled = false;
    }
}

async function deleteDocument(documentId) {
    if (!confirm("¿Eliminar este documento del índice?")) return;

    try {
        await fetchJson(`${API}/documents/${documentId}`, { method: "DELETE" });
        await Promise.all([loadDocuments(), loadHealth()]);
        showToast("Documento eliminado");
    } catch (error) {
        showToast(error.message);
    }
}

dropzone.addEventListener("click", () => fileInput.click());
browseBtn.addEventListener("click", (event) => {
    event.stopPropagation();
    fileInput.click();
});

fileInput.addEventListener("change", (event) => {
    addFiles(event.target.files);
    fileInput.value = "";
});

dropzone.addEventListener("dragover", (event) => {
    event.preventDefault();
    dropzone.classList.add("dropzone--active");
});

dropzone.addEventListener("dragleave", () => {
    dropzone.classList.remove("dropzone--active");
});

dropzone.addEventListener("drop", (event) => {
    event.preventDefault();
    dropzone.classList.remove("dropzone--active");
    addFiles(event.dataTransfer.files);
});

uploadBtn.addEventListener("click", uploadDocuments);
clearFilesBtn.addEventListener("click", () => {
    pendingFiles = [];
    renderSelectedFiles();
    hideAlert(uploadAlert);
});

refreshDocsBtn.addEventListener("click", () => {
    loadDocuments();
    loadHealth();
});

searchBtn.addEventListener("click", previewSearch);
exportBtn.addEventListener("click", downloadMarkdown);

documentsBody.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-id]");
    if (button) {
        deleteDocument(button.dataset.id);
    }
});

loadHealth();
loadDocuments();
