// document.js - Document-specific functionality

// ===================== DOCUMENT FUNCTIONS =====================
async function loadDocuments() {
    const container = document.getElementById('documentsList');
    if (!container) return;

    container.innerHTML = `
        <div class="loading-state">
            <i class="fas fa-spinner fa-spin"></i>
            <p>Loading documents...</p>
        </div>
    `;

    try {
        const response = await fetch('/portfolio/api/user-documents');
        const result = await response.json();

        if (result.success && result.documents && result.documents.length > 0) {
            renderDocuments(result.documents, container);
        } else {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-folder-open fa-3x"></i>
                    <h3>No Documents Yet</h3>
                    <p>Upload your first document to get started</p>
                    <button class="btn btn-primary" onclick="showUploadModal()">
                        <i class="fas fa-upload"></i> Upload Document
                    </button>
                </div>
            `;
        }
    } catch (error) {
        console.error('Error loading documents:', error);
        container.innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-circle"></i>
                Failed to load documents. Please try again.
            </div>
        `;
    }
}

function renderDocuments(documents, container) {
    let html = `
        <div class="documents-toolbar">
            <div class="search-box">
                <i class="fas fa-search"></i>
                <input type="text" id="documentSearch" placeholder="Search documents..." onkeyup="searchDocuments()">
            </div>
            <div class="filter-options">
                <button class="filter-btn active" onclick="filterDocuments('all')">
                    <i class="fas fa-list"></i> All
                </button>
                <button class="filter-btn" onclick="filterDocuments('personal')">
                    <i class="fas fa-user"></i> Personal
                </button>
                <button class="filter-btn" onclick="filterDocuments('work')">
                    <i class="fas fa-briefcase"></i> Work
                </button>
                <button class="filter-btn" onclick="filterDocuments('archive')">
                    <i class="fas fa-archive"></i> Archive
                </button>
            </div>
        </div>
        <div class="documents-grid" id="documentsGrid">
    `;

    documents.forEach(doc => {
        const iconClass = doc.icon || 'fas fa-file';
        const category = doc.category || 'personal';

        html += `
            <div class="document-card" data-category="${category}" data-name="${doc.name.toLowerCase()}">
                <div class="document-icon ${category}">
                    <i class="${iconClass}"></i>
                </div>
                <div class="document-info">
                    <div class="document-name" title="${escapeHtml(doc.name)}">${escapeHtml(doc.name)}</div>
                    <div class="document-meta">
                        <span><i class="fas fa-hdd"></i> ${doc.formattedSize || formatFileSize(doc.fileSize)}</span>
                        <span><i class="fas fa-calendar"></i> ${new Date(doc.uploadedAt).toLocaleDateString()}</span>
                        <span><i class="fas fa-tag"></i> ${category.charAt(0).toUpperCase() + category.slice(1)}</span>
                    </div>
                </div>
                <div class="document-actions">
                    <button class="action-btn" onclick="downloadDocument(${doc.id})" title="Download">
                        <i class="fas fa-download"></i>
                    </button>
                    <button class="action-btn" onclick="viewDocumentDetails(${doc.id})" title="Details">
                        <i class="fas fa-info-circle"></i>
                    </button>
                    <button class="action-btn" onclick="deleteDocument(${doc.id})" title="Delete">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
    });

    html += '</div>';
    container.innerHTML = html;
}

async function downloadDocument(documentId) {
    window.open(`/portfolio/download/${documentId}`, '_blank');
}

async function deleteDocument(documentId) {
    if (!confirm('Are you sure you want to delete this document?')) {
        return;
    }

    try {
        const response = await fetch(`/portfolio/api/delete-document/${documentId}`, {
            method: 'DELETE'
        });

        const result = await response.json();

        if (result.success) {
            showSuccess('Document deleted successfully');
            loadDocuments();
        } else {
            showError(result.message || 'Failed to delete document');
        }
    } catch (error) {
        console.error('Error deleting document:', error);
        showError('Network error. Please try again.');
    }
}

async function viewDocumentDetails(documentId) {
    try {
        const response = await fetch(`/portfolio/item/${documentId}`);
        const result = await response.json();

        if (result.success) {
            const item = result.item;
            const modalContent = document.getElementById('itemDetailsContent');
            if (modalContent) {
                modalContent.innerHTML = `
                    <div class="item-details">
                        <h4><i class="fas fa-file"></i> ${escapeHtml(item.name)}</h4>
                        <table class="details-table">
                            <tr><th>Type:</th><td>Personal Document</td></tr>
                            <tr><th>File Type:</th><td>${escapeHtml(item.fileType || 'Unknown')}</td></tr>
                            <tr><th>File Size:</th><td>${formatFileSize(item.fileSize)}</td></tr>
                            <tr><th>File ID:</th><td><code>${escapeHtml(item.fileId || 'N/A')}</code></td></tr>
                            <tr><th>Status:</th><td><span class="badge badge-success">APPROVED</span></td></tr>
                            <tr><th>Uploaded At:</th><td>${new Date(item.uploadedAt).toLocaleString()}</td></tr>
                        </table>
                    </div>
                `;
                openModal('itemDetailsModal');
            }
        }
    } catch (error) {
        console.error('Error loading document details:', error);
        showError('Failed to load document details');
    }
}

function searchDocuments() {
    const searchTerm = document.getElementById('documentSearch')?.value.toLowerCase() || '';
    const cards = document.querySelectorAll('.document-card');

    cards.forEach(card => {
        const name = card.getAttribute('data-name') || '';
        if (name.includes(searchTerm)) {
            card.style.display = 'flex';
        } else {
            card.style.display = 'none';
        }
    });
}

function filterDocuments(category) {
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    event.target.classList.add('active');

    const cards = document.querySelectorAll('.document-card');

    cards.forEach(card => {
        const cardCategory = card.getAttribute('data-category');
        if (category === 'all' || cardCategory === category) {
            card.style.display = 'flex';
        } else {
            card.style.display = 'none';
        }
    });
}

// ===================== UTILITY FUNCTIONS =====================
function formatFileSize(bytes) {
    if (!bytes) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}

// ===================== GLOBAL EXPORTS =====================
window.loadDocuments = loadDocuments;
window.searchDocuments = searchDocuments;
window.filterDocuments = filterDocuments;
window.downloadDocument = downloadDocument;
window.deleteDocument = deleteDocument;
window.viewDocumentDetails = viewDocumentDetails;