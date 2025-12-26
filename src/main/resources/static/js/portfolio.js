// portfolio.js - Portfolio Management System
// Enhanced to handle both portfolio-view.html and portfolios.html

// ===================== GLOBAL STATE =====================
const portfolioState = {
    currentPortfolioId: null,
    currentFolderId: null,
    selectedFiles: [],
    userRole: null,
    userId: null,
    uploadDestination: 'portfolio',
    isInitialized: false,
    isInitializing: false,
    isDetailsPage: false
};

// ===================== PAGE DETECTION =====================
function detectPageType() {
    const path = window.location.pathname;
    portfolioState.isDetailsPage = path.includes('/details/');
    console.log('Page detection:', {
        path: path,
        isDetailsPage: portfolioState.isDetailsPage,
        hash: window.location.hash
    });
}

// ===================== INITIALIZATION =====================
function initPortfolioSystem() {
    // Detect page type first
    detectPageType();

    // Prevent multiple initializations
    if (portfolioState.isInitialized || portfolioState.isInitializing) {
        console.log('Portfolio system already initialized or initializing');
        return;
    }

    portfolioState.isInitializing = true;

    try {
        console.log('🔧 Initializing portfolio system...');
        console.log('Page type:', portfolioState.isDetailsPage ? 'Details Page' : 'Main Page');

        // Set user data from global variables set by Thymeleaf
        portfolioState.userRole = window.currentUserRole || 'GUEST';
        portfolioState.userId = window.currentUserId || 0;
        portfolioState.currentPortfolioId = window.currentPortfolioId || null;
        portfolioState.currentFolderId = window.currentFolderId || null;

        console.log('User state:', {
            role: portfolioState.userRole,
            id: portfolioState.userId,
            portfolioId: portfolioState.currentPortfolioId,
            folderId: portfolioState.currentFolderId
        });

        // Always setup these basic systems
        setupToastSystem();
        setupModals();
        setupUploadSystem();

        if (portfolioState.isDetailsPage) {
            // Details page specific initialization
            initPortfolioDetailsPage();
        } else {
            // Main page initialization
            initPortfolioMainPage();
        }

        portfolioState.isInitialized = true;
        portfolioState.isInitializing = false;
        console.log('✅ Portfolio system initialized successfully for ' +
                   (portfolioState.isDetailsPage ? 'details page' : 'main page'));

    } catch (error) {
        console.error('❌ Error initializing portfolio system:', error);
        showError('Failed to initialize portfolio system: ' + error.message);
        portfolioState.isInitializing = false;
    }
}

function initPortfolioDetailsPage() {
    console.log('📁 Initializing portfolio details page...');

    // Setup details page specific functionality
    setupDetailsPageEventDelegation();

    // Load portfolios for upload dropdown if modal exists
    if (document.getElementById('uploadPortfolio')) {
        loadPortfoliosForUpload();
    }

    // Setup any details page specific modals
    const uploadModal = document.getElementById('uploadModal');
    if (uploadModal) {
        // Ensure upload modal works in details page
        uploadModal.addEventListener('click', function(e) {
            if (e.target === this || e.target.classList.contains('modal-close')) {
                closeModal('uploadModal');
            }
        });
    }
}

function initPortfolioMainPage() {
    console.log('📋 Initializing portfolio main page...');

    // Main page specific setups
    setupEventDelegation();
    setupTabSwitching();

    // Load initial data
    setTimeout(() => {
        loadInitialData();
    }, 300);
}

function loadInitialData() {
    console.log('📥 Loading initial data for main page...');

    // Load documents by default if on documents tab
    if (document.getElementById('documents')?.classList.contains('active')) {
        console.log('Loading documents tab...');
        setTimeout(() => {
            loadDocuments();
        }, 500);
    }

    // Load portfolios for upload dropdown
    setTimeout(() => {
        loadPortfoliosForUpload();
    }, 700);
}

// ===================== DETAILS PAGE EVENT DELEGATION =====================
function setupDetailsPageEventDelegation() {
    console.log('Setting up details page event delegation...');

    // Handle item clicks
    document.addEventListener('click', function(e) {
        // Handle folder clicks
        if (e.target.closest('.item-card.folder')) {
            e.preventDefault();
            e.stopPropagation();
            const folderId = e.target.closest('.item-card')?.getAttribute('data-folder-id');
            if (folderId) {
                openFolder(folderId);
                return false;
            }
        }

        // Handle file clicks
        if (e.target.closest('.item-card.file')) {
            e.preventDefault();
            e.stopPropagation();
            const itemId = e.target.closest('.item-card')?.getAttribute('data-item-id');
            if (itemId) {
                viewItem(itemId);
                return false;
            }
        }
    });
}

// ===================== EVENT DELEGATION (MAIN PAGE) =====================
function setupEventDelegation() {
    console.log('Setting up event delegation for main page...');

    // Use event delegation for portfolio cards
    document.addEventListener('click', function(e) {
        // Handle portfolio card body clicks
        const cardBody = e.target.closest('.card-body');
        if (cardBody) {
            e.preventDefault();
            e.stopPropagation();
            const portfolioId = cardBody.getAttribute('data-portfolio-id') ||
                               cardBody.closest('.portfolio-card')?.getAttribute('data-portfolio-id');
            if (portfolioId) {
                console.log('Viewing portfolio:', portfolioId);
                window.location.href = `/portfolio/details/${portfolioId}`;
                return false;
            }
        }

        // Handle delete button clicks
        const deleteBtn = e.target.closest('.delete-portfolio-btn');
        if (deleteBtn) {
            e.preventDefault();
            e.stopPropagation();
            const portfolioId = deleteBtn.getAttribute('data-portfolio-id') ||
                               deleteBtn.closest('.portfolio-card')?.getAttribute('data-portfolio-id');
            if (portfolioId) {
                console.log('Deleting portfolio:', portfolioId);
                deletePortfolio(portfolioId, e);
                return false;
            }
        }

        // Handle shared portfolio card clicks
        const sharedCard = e.target.closest('.portfolio-card.shared');
        if (sharedCard && !e.target.closest('.delete-portfolio-btn')) {
            e.preventDefault();
            e.stopPropagation();
            const portfolioId = sharedCard.querySelector('.card-body')?.getAttribute('data-portfolio-id');
            if (portfolioId) {
                console.log('Viewing shared portfolio:', portfolioId);
                window.location.href = `/portfolio/details/${portfolioId}`;
                return false;
            }
        }
    });
}

// ===================== MODAL SYSTEM =====================
function setupModals() {
    console.log('Setting up modals...');

    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('click', function(e) {
            if (e.target === this || e.target.classList.contains('modal-close')) {
                closeModal(this.id);
            }
        });
    });

    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal.active').forEach(modal => {
                closeModal(modal.id);
            });
        }
    });
}

function toggleUploadDestination() {
    const destination = document.querySelector('input[name="uploadDestination"]:checked')?.value || 'portfolio';
    portfolioState.uploadDestination = destination;

    console.log('Upload destination changed to:', destination);

    const portfolioSelection = document.getElementById('portfolioSelection');
    const documentsCategory = document.getElementById('documentsCategory');

    if (destination === 'portfolio') {
        if (portfolioSelection) portfolioSelection.style.display = 'block';
        if (documentsCategory) documentsCategory.style.display = 'none';
        loadPortfoliosForUpload();
    } else {
        if (portfolioSelection) portfolioSelection.style.display = 'none';
        if (documentsCategory) documentsCategory.style.display = 'block';
    }

    updateUploadButton();
}

function showCreatePortfolioModal() {
    console.log('Opening create portfolio modal...');
    openModal('createPortfolioModal');
    document.getElementById('portfolioName')?.focus();
}

function showUploadModal() {
    console.log('Opening upload modal...');
    openModal('uploadModal');

    // Set default destination based on page
    if (portfolioState.isDetailsPage) {
        portfolioState.uploadDestination = 'portfolio';
        const portfolioRadio = document.querySelector('input[name="uploadDestination"][value="portfolio"]');
        if (portfolioRadio) portfolioRadio.checked = true;
    }

    toggleUploadDestination();
}

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
        console.log('Modal opened:', modalId);
    } else {
        console.error('Modal not found:', modalId);
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('active');
        document.body.style.overflow = 'auto';

        if (modalId === 'uploadModal') {
            clearUploadForm();
        }

        console.log('Modal closed:', modalId);
    }
}

// ===================== UPLOAD SYSTEM =====================
function setupUploadSystem() {
    const uploadArea = document.getElementById('uploadArea');
    const fileInput = document.getElementById('fileInput');

    if (uploadArea && fileInput) {
        console.log('Setting up upload system...');

        // Clear any existing event listeners by cloning
        const newUploadArea = uploadArea.cloneNode(true);
        const newFileInput = fileInput.cloneNode(true);

        uploadArea.parentNode.replaceChild(newUploadArea, uploadArea);
        fileInput.parentNode.replaceChild(newFileInput, fileInput);

        // Get fresh references
        const freshUploadArea = document.getElementById('uploadArea');
        const freshFileInput = document.getElementById('fileInput');

        // DRAG AND DROP HANDLERS
        freshUploadArea.addEventListener('dragover', function(e) {
            e.preventDefault();
            e.stopPropagation();
            this.classList.add('dragover');
        });

        freshUploadArea.addEventListener('dragleave', function(e) {
            e.preventDefault();
            e.stopPropagation();
            this.classList.remove('dragover');
        });

        freshUploadArea.addEventListener('drop', function(e) {
            e.preventDefault();
            e.stopPropagation();
            this.classList.remove('dragover');
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                handleFiles(files);
            }
        });

        // CLICK HANDLER
        freshUploadArea.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            if (portfolioState.selectedFiles.length === 0) {
                freshFileInput.click();
            }
        });

        // FILE INPUT CHANGE HANDLER
        freshFileInput.addEventListener('change', function(e) {
            e.stopPropagation();
            console.log('File input changed:', this.files?.length, 'files');
            if (this.files && this.files.length > 0) {
                handleFiles(this.files);
            }
        });

        console.log('✅ Upload system initialized');
    } else {
        console.log('Upload area or file input not found - may not be needed on this page');
    }
}

function handleFiles(files) {
    console.log('Handling files:', files.length);

    const preview = document.getElementById('uploadPreview');
    if (!preview) {
        console.error('Upload preview element not found');
        return;
    }

    // Clear previous selection
    portfolioState.selectedFiles = [];
    preview.innerHTML = '';

    let hasInvalidFiles = false;

    Array.from(files).forEach(file => {
        const validation = validateFile(file);
        if (!validation.valid) {
            showError(validation.message);
            hasInvalidFiles = true;
            return;
        }

        portfolioState.selectedFiles.push(file);
        const previewItem = createFilePreview(file);
        preview.appendChild(previewItem);
    });

    updateUploadButton();

    if (hasInvalidFiles && portfolioState.selectedFiles.length > 0) {
        showWarning('Some files were rejected');
    }

    console.log('Valid files selected:', portfolioState.selectedFiles.length);
}

function validateFile(file) {
    const maxSize = 50 * 1024 * 1024; // 50MB
    if (file.size > maxSize) {
        return {
            valid: false,
            message: `File "${file.name}" exceeds 50MB limit`
        };
    }

    if (file.type.startsWith('video/')) {
        return {
            valid: false,
            message: `Video files are not allowed: "${file.name}"`
        };
    }

    const allowedExtensions = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.jpg', '.jpeg', '.png', '.gif', '.txt', '.zip', '.rar'];
    const fileExtension = '.' + file.name.split('.').pop().toLowerCase();

    if (!allowedExtensions.includes(fileExtension)) {
        return {
            valid: false,
            message: `File type not allowed: "${file.name}". Allowed: ${allowedExtensions.join(', ')}`
        };
    }

    return { valid: true };
}

function createFilePreview(file) {
    const div = document.createElement('div');
    div.className = 'preview-item';
    div.setAttribute('data-file-name', file.name);

    const icon = getFileIcon(file);
    const size = formatFileSize(file.size);

    div.innerHTML = `
        <div class="preview-icon">
            <i class="fas ${icon}"></i>
        </div>
        <div class="preview-info">
            <div class="preview-name" title="${escapeHtml(file.name)}">${escapeHtml(file.name)}</div>
            <div class="preview-size">${size}</div>
        </div>
        <button type="button" class="remove-file" onclick="removeFilePreview('${escapeHtml(file.name)}')">
            <i class="fas fa-times"></i>
        </button>
    `;

    return div;
}

function removeFilePreview(fileName) {
    portfolioState.selectedFiles = portfolioState.selectedFiles.filter(file => file.name !== fileName);

    const previewItem = document.querySelector(`.preview-item[data-file-name="${fileName}"]`);
    if (previewItem) {
        previewItem.remove();
    }

    updateUploadButton();
}

function updateUploadButton() {
    const uploadBtn = document.getElementById('uploadBtn');
    if (!uploadBtn) {
        console.log('Upload button not found - may not be on this page');
        return;
    }

    const hasFiles = portfolioState.selectedFiles.length > 0;

    if (portfolioState.uploadDestination === 'portfolio') {
        const portfolioSelect = document.getElementById('uploadPortfolio');
        const hasPortfolio = portfolioSelect && portfolioSelect.value && portfolioSelect.value !== '';
        uploadBtn.disabled = !(hasPortfolio && hasFiles);
        uploadBtn.innerHTML = hasPortfolio && hasFiles ?
            `<i class="fas fa-upload"></i> Upload ${portfolioState.selectedFiles.length} File(s) to Portfolio` :
            `<i class="fas fa-upload"></i> Upload Files`;
    } else {
        uploadBtn.disabled = !hasFiles;
        uploadBtn.innerHTML = hasFiles ?
            `<i class="fas fa-upload"></i> Upload ${portfolioState.selectedFiles.length} File(s) to Documents` :
            `<i class="fas fa-upload"></i> Upload Files`;
    }
}

async function uploadFiles() {
    console.log('Starting file upload...');

    if (portfolioState.selectedFiles.length === 0) {
        showError('Please select files to upload');
        return;
    }

    const uploadBtn = document.getElementById('uploadBtn');
    if (!uploadBtn) return;

    const originalText = uploadBtn.innerHTML;
    uploadBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Uploading...';
    uploadBtn.disabled = true;

    const progressBar = document.getElementById('uploadProgressBar');
    const progressContainer = document.getElementById('uploadProgress');
    const statusText = document.getElementById('uploadStatus');

    if (progressBar && progressContainer && statusText) {
        progressContainer.style.display = 'block';
        progressBar.style.width = '0%';
        statusText.textContent = 'Starting upload...';
    }

    try {
        if (portfolioState.uploadDestination === 'portfolio') {
            console.log('Uploading to portfolio...');
            await uploadToPortfolio();
        } else {
            console.log('Uploading to documents...');
            await uploadToDocuments();
        }
    } catch (error) {
        console.error('Upload error:', error);
        showError('Upload failed: ' + error.message);

        // Reset button state
        uploadBtn.innerHTML = originalText;
        uploadBtn.disabled = false;
        if (progressContainer) {
            progressContainer.style.display = 'none';
        }
    }
}

async function uploadToPortfolio() {
    const portfolioSelect = document.getElementById('uploadPortfolio');
    const portfolioId = portfolioSelect ? portfolioSelect.value : null;

    if (!portfolioId) {
        showError('Please select a portfolio');
        throw new Error('No portfolio selected');
    }

    console.log('Uploading to portfolio ID:', portfolioId);

    updateUploadProgress(10, 'Preparing upload...');

    const formData = new FormData();

    // Add each file with the correct parameter name "files[]" for multiple files
    portfolioState.selectedFiles.forEach(file => {
        formData.append('file', file); // Use 'file' as parameter name (matching controller)
    });

    formData.append('portfolioId', portfolioId);

    // Check if we're in a specific folder context
    if (portfolioState.isDetailsPage && portfolioState.currentFolderId) {
        formData.append('folderId', portfolioState.currentFolderId);
    }

    updateUploadProgress(30, 'Uploading files to server...');

    try {
        console.log('Sending upload request...');
        const response = await fetch('/portfolio/upload', {
            method: 'POST',
            body: formData
        });

        console.log('Upload response status:', response.status);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Upload failed:', errorText);
            throw new Error(`Server error: ${response.status} - ${errorText}`);
        }

        const result = await response.json();
        console.log('Upload result:', result);

        updateUploadProgress(70, 'Processing files...');

        if (result.success) {
            updateUploadProgress(100, 'Upload complete!');
            showSuccess(result.message || 'Files uploaded successfully!');

            // Close modal and reset form
            setTimeout(() => {
                closeModal('uploadModal');
                clearUploadForm();
            }, 1000);

            // Refresh current view
            if (portfolioState.isDetailsPage) {
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            } else if (document.getElementById('documents')?.classList.contains('active')) {
                loadDocuments();
            }

            return result;
        } else {
            throw new Error(result.message || 'Upload failed');
        }
    } catch (error) {
        console.error('Upload to portfolio error:', error);
        updateUploadProgress(0, 'Upload failed');
        throw error;
    }
}

async function uploadToDocuments() {
    const categorySelect = document.getElementById('documentCategory');
    const category = categorySelect ? categorySelect.value : 'personal';

    console.log('Uploading to documents, category:', category);

    let successCount = 0;
    let failCount = 0;
    const errors = [];

    for (let i = 0; i < portfolioState.selectedFiles.length; i++) {
        const file = portfolioState.selectedFiles[i];

        updateUploadProgress(
            Math.floor((i / portfolioState.selectedFiles.length) * 100),
            `Uploading ${i + 1} of ${portfolioState.selectedFiles.length}: ${file.name}`
        );

        try {
            const formData = new FormData();
            formData.append('file', file);
            formData.append('category', category);

            console.log('Uploading file:', file.name);

            const response = await fetch('/portfolio/api/upload-to-documents', {
                method: 'POST',
                body: formData
            });

            console.log('Document upload response status:', response.status);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const result = await response.json();
            console.log('Document upload result:', result);

            if (result.success) {
                successCount++;
            } else {
                failCount++;
                errors.push(`${file.name}: ${result.message}`);
            }
        } catch (error) {
            console.error('Error uploading document:', file.name, error);
            failCount++;
            errors.push(`${file.name}: ${error.message}`);
        }
    }

    updateUploadProgress(100, 'Upload complete!');

    if (successCount > 0) {
        showSuccess(`Successfully uploaded ${successCount} file(s) to documents`);

        setTimeout(() => {
            closeModal('uploadModal');
            clearUploadForm();
        }, 1000);

        // Refresh documents view if active
        if (document.getElementById('documents')?.classList.contains('active')) {
            setTimeout(() => {
                loadDocuments();
            }, 1500);
        }
    }

    if (failCount > 0) {
        errors.forEach(error => showError(error));
        showWarning(`${failCount} file(s) failed to upload`);
    }
}

function updateUploadProgress(percent, message) {
    const progressBar = document.getElementById('uploadProgressBar');
    const statusText = document.getElementById('uploadStatus');

    if (progressBar) {
        progressBar.style.width = percent + '%';
    }
    if (statusText) {
        statusText.textContent = message;
    }
}

function clearUploadForm() {
    console.log('Clearing upload form...');

    const fileInput = document.getElementById('fileInput');
    if (fileInput) {
        fileInput.value = '';
    }

    const preview = document.getElementById('uploadPreview');
    if (preview) {
        preview.innerHTML = '';
    }

    portfolioState.selectedFiles = [];

    const progressContainer = document.getElementById('uploadProgress');
    if (progressContainer) {
        progressContainer.style.display = 'none';
    }

    updateUploadButton();
}

// ===================== DELETE FUNCTION =====================
async function deletePortfolio(portfolioId, event = null) {
    console.log('Deleting portfolio:', portfolioId);

    if (!portfolioId) {
        showError('No portfolio ID provided');
        return;
    }

    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }

    let portfolioName = 'this portfolio';
    try {
        const portfolioCard = event?.target?.closest('.portfolio-card');
        if (portfolioCard) {
            const nameElement = portfolioCard.querySelector('h3');
            if (nameElement) {
                portfolioName = `"${nameElement.textContent.trim()}"`;
            }
        }
    } catch (e) {
        console.log('Could not get portfolio name:', e);
    }

    const confirmed = confirm(`Are you sure you want to delete ${portfolioName}?\n\nThis action cannot be undone. All files and folders will be permanently deleted.`);

    if (!confirmed) {
        console.log('Portfolio deletion cancelled');
        return;
    }

    const deleteBtn = event?.target?.closest('button');
    const originalText = deleteBtn?.innerHTML;
    if (deleteBtn) {
        deleteBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        deleteBtn.disabled = true;
    }

    try {
        console.log('Sending delete request for portfolio:', portfolioId);
        const response = await fetch(`/portfolio/${portfolioId}`, {
            method: 'DELETE',
            headers: {
                'Accept': 'application/json'
            }
        });

        console.log('Delete response status:', response.status);

        if (!response.ok) {
            throw new Error('Network response was not ok: ' + response.status);
        }

        const result = await response.json();
        console.log('Delete result:', result);

        if (result.success) {
            showSuccess(`Portfolio deleted successfully!`);

            const portfolioCard = event?.target?.closest('.portfolio-card');
            if (portfolioCard) {
                portfolioCard.style.opacity = '0.5';
                portfolioCard.style.transform = 'scale(0.95)';

                setTimeout(() => {
                    portfolioCard.style.transition = 'all 0.3s ease';
                    portfolioCard.style.height = '0';
                    portfolioCard.style.margin = '0';
                    portfolioCard.style.padding = '0';
                    portfolioCard.style.opacity = '0';
                    portfolioCard.style.overflow = 'hidden';

                    setTimeout(() => {
                        portfolioCard.remove();

                        const grid = document.querySelector('.grid-view');
                        if (grid && grid.children.length === 0) {
                            const emptyState = document.createElement('div');
                            emptyState.className = 'empty-state';
                            emptyState.innerHTML = `
                                <i class="fas fa-folder-open fa-3x"></i>
                                <h3>No Portfolios Found</h3>
                                <p>Create your first portfolio to get started</p>
                                <button class="btn btn-primary" onclick="showCreatePortfolioModal()">
                                    <i class="fas fa-plus"></i> Create Portfolio
                                </button>
                            `;
                            grid.appendChild(emptyState);
                        }
                    }, 300);
                }, 100);
            } else {
                setTimeout(() => window.location.reload(), 1500);
            }
        } else {
            showError(result.message || 'Failed to delete portfolio');
            if (deleteBtn) {
                deleteBtn.innerHTML = originalText;
                deleteBtn.disabled = false;
            }
        }
    } catch (error) {
        console.error('Delete portfolio error:', error);
        showError('Network error. Please try again.');
        if (deleteBtn) {
            deleteBtn.innerHTML = originalText;
            deleteBtn.disabled = false;
        }
    }
}

// ===================== PORTFOLIO MANAGEMENT =====================
let isCreatingPortfolio = false; // Prevent multiple simultaneous creations

async function createPortfolio() {
    console.log('Creating portfolio...');

    // Prevent multiple simultaneous calls
    if (isCreatingPortfolio) {
        console.warn('Portfolio creation already in progress');
        return;
    }

    isCreatingPortfolio = true;

    const nameInput = document.getElementById('portfolioName');
    const descInput = document.getElementById('portfolioDescription');
    const typeSelect = document.getElementById('portfolioType');

    if (!nameInput || !typeSelect) {
        showError('Form elements not found');
        isCreatingPortfolio = false;
        return;
    }

    const name = nameInput.value.trim();
    const description = descInput ? descInput.value.trim() : '';
    const type = typeSelect.value;

    if (!name) {
        showError('Please enter portfolio name');
        nameInput.focus();
        isCreatingPortfolio = false;
        return;
    }

    if (!type) {
        showError('Please select portfolio type');
        typeSelect.focus();
        isCreatingPortfolio = false;
        return;
    }

    console.log('Portfolio data:', { name, description, type });

    const modal = document.getElementById('createPortfolioModal');
    const submitBtn = modal ? modal.querySelector('.btn-primary') : null;
    const originalText = submitBtn ? submitBtn.innerHTML : 'Create';

    if (submitBtn) {
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Creating...';
        submitBtn.disabled = true;
    }

    try {
        // Use FormData for better compatibility
        const formData = new URLSearchParams();
        formData.append('name', name);
        formData.append('description', description);
        formData.append('type', type);

        console.log('Sending create portfolio request...');
        const response = await fetch('/portfolio/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: formData
        });

        console.log('Create portfolio response status:', response.status);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Create portfolio failed:', errorText);
            throw new Error(`Server error: ${response.status}`);
        }

        const result = await response.json();
        console.log('Create portfolio result:', result);

        if (result.success) {
            showSuccess('Portfolio created successfully!');
            closeModal('createPortfolioModal');
            nameInput.value = '';
            if (descInput) descInput.value = '';
            typeSelect.value = '';

            // Refresh the page to show new portfolio
            setTimeout(() => {
                window.location.reload();
            }, 1500);
        } else {
            showError(result.message || 'Failed to create portfolio');
        }
    } catch (error) {
        console.error('Create portfolio error:', error);
        showError('Network error. Please check your connection and try again.');
    } finally {
        if (submitBtn) {
            submitBtn.innerHTML = originalText;
            submitBtn.disabled = false;
        }
        // Reset the flag after a delay to prevent rapid re-clicks
        setTimeout(() => {
            isCreatingPortfolio = false;
        }, 2000);
    }
}

// ===================== DATA LOADING =====================
async function loadPortfoliosForUpload() {
    const select = document.getElementById('uploadPortfolio');
    if (!select) {
        console.log('Upload portfolio select element not found - may not be on this page');
        return;
    }

    try {
        console.log('Loading portfolios for upload dropdown...');
        const response = await fetch('/portfolio/api/my-portfolios');
        console.log('Portfolios response status:', response.status);

        if (!response.ok) {
            throw new Error('Network response was not ok: ' + response.status);
        }

        const result = await response.json();
        console.log('Portfolios result:', result);

        if (result.success && result.portfolios && result.portfolios.length > 0) {
            let html = '<option value="">Select a portfolio...</option>';
            result.portfolios.forEach(portfolio => {
                html += `<option value="${portfolio.id}">${escapeHtml(portfolio.name)} (${portfolio.itemCount || 0} items)</option>`;
            });
            select.innerHTML = html;
            updateUploadButton();
            console.log('Loaded', result.portfolios.length, 'portfolios for upload');
        } else {
            select.innerHTML = '<option value="">No portfolios available. Create one first.</option>';
            updateUploadButton();
            console.log('No portfolios available for upload');
        }
    } catch (error) {
        console.error('Error loading portfolios for upload:', error);
        select.innerHTML = '<option value="">Error loading portfolios</option>';
    }
}

async function loadDepartmentPortfolios() {
    console.log('Loading department portfolios...');

    const container = document.getElementById('departmentPortfoliosContainer');
    if (!container) {
        console.error('Department portfolios container not found');
        return;
    }

    container.innerHTML = `
        <div class="loading-state">
            <i class="fas fa-spinner fa-spin"></i>
            <p>Loading department portfolios...</p>
        </div>
    `;

    try {
        const response = await fetch('/portfolio/api/department-portfolios');
        console.log('Department portfolios response status:', response.status);

        if (!response.ok) {
            throw new Error('Network response was not ok: ' + response.status);
        }

        const result = await response.json();
        console.log('Department portfolios result:', result);

        if (result.success && result.portfolios) {
            let html = '<div class="grid-view">';
            result.portfolios.forEach(portfolio => {
                html += `
                    <div class="portfolio-card" data-portfolio-id="${portfolio.id}">
                        <div class="card-header">
                            <i class="fas fa-building"></i>
                            <span class="shared-badge">DEPARTMENT</span>
                        </div>
                        <div class="card-body" data-portfolio-id="${portfolio.id}">
                            <h3>${escapeHtml(portfolio.name)}</h3>
                            <p class="description">${escapeHtml(portfolio.description || 'No description')}</p>
                            <div class="card-meta">
                                <span><i class="fas fa-file"></i> ${portfolio.itemCount || 0} items</span>
                                <span><i class="fas fa-user"></i> ${escapeHtml(portfolio.ownerName)}</span>
                            </div>
                        </div>
                    </div>
                `;
            });
            html += '</div>';
            container.innerHTML = html;
            console.log('Loaded', result.portfolios.length, 'department portfolios');
        } else {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-building fa-3x"></i>
                    <h3>No Department Portfolios</h3>
                    <p>No department portfolios available</p>
                </div>
            `;
        }
    } catch (error) {
        console.error('Error loading department portfolios:', error);
        container.innerHTML = '<div class="alert alert-danger">Failed to load department portfolios</div>';
    }
}

// ===================== COLLEGE PORTFOLIOS =====================
async function loadCollegePortfolios() {
    console.log('Loading college portfolios...');

    const container = document.getElementById('collegePortfoliosList');
    if (!container) {
        console.error('College portfolios list container not found');
        return;
    }

    container.innerHTML = `
        <div class="loading-state">
            <i class="fas fa-spinner fa-spin"></i>
            <p>Loading college portfolios...</p>
        </div>
    `;

    try {
        const response = await fetch('/portfolio/api/college-portfolios');
        console.log('College portfolios response status:', response.status);

        if (!response.ok) {
            throw new Error('Network response was not ok: ' + response.status);
        }

        const result = await response.json();
        console.log('College portfolios result:', result);

        if (result.success && result.portfolios) {
            if (result.portfolios.length === 0) {
                container.innerHTML = `
                    <div class="empty-state">
                        <i class="fas fa-university fa-3x"></i>
                        <h3>No College Portfolios</h3>
                        <p>Create your first college portfolio to get started</p>
                        <button class="btn btn-primary" onclick="showCreatePortfolioModal()">
                            <i class="fas fa-plus"></i> Create College Portfolio
                        </button>
                    </div>
                `;
            } else {
                renderCollegePortfolios(result.portfolios, container);
            }
        } else {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-university fa-3x"></i>
                    <h3>No College Portfolios</h3>
                    <p>${result.message || 'Create your first college portfolio to get started'}</p>
                    <button class="btn btn-primary" onclick="showCreatePortfolioModal()">
                        <i class="fas fa-plus"></i> Create College Portfolio
                    </button>
                </div>
            `;
        }
    } catch (error) {
        console.error('Error loading college portfolios:', error);
        container.innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-circle"></i>
                Failed to load college portfolios. Please try again.
            </div>
        `;
    }
}

function renderCollegePortfolios(portfolios, container) {
    let html = '<div class="grid-view">';

    portfolios.forEach(portfolio => {
        html += `
            <div class="portfolio-card college" data-portfolio-id="${portfolio.id}">
                <div class="card-header">
                    <i class="fas fa-university"></i>
                    <span class="shared-badge">COLLEGE</span>
                    <div class="card-actions">
                        <button class="action-btn delete-portfolio-btn" data-portfolio-id="${portfolio.id}">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
                <div class="card-body" data-portfolio-id="${portfolio.id}">
                    <h3>${escapeHtml(portfolio.name)}</h3>
                    <p class="description">${escapeHtml(portfolio.description || 'No description')}</p>
                    <div class="card-meta">
                        <span><i class="fas fa-file"></i> ${portfolio.itemCount || 0} items</span>
                        <span><i class="fas fa-calendar"></i> ${formatDate(portfolio.createdAt)}</span>
                    </div>
                </div>
            </div>
        `;
    });

    html += '</div>';
    container.innerHTML = html;

    console.log('Rendered', portfolios.length, 'college portfolios');
}

function formatDate(dateString) {
    if (!dateString) return 'Unknown date';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
    });
}

// ===================== DOCUMENT FUNCTIONS =====================
async function loadDocuments() {
    console.log('Loading documents...');

    const container = document.getElementById('documentsList');
    if (!container) {
        console.error('Documents list container not found');
        return;
    }

    container.innerHTML = `
        <div class="loading-state">
            <i class="fas fa-spinner fa-spin"></i>
            <p>Loading documents...</p>
        </div>
    `;

    try {
        const response = await fetch('/portfolio/api/user-documents');
        console.log('Documents response status:', response.status);

        if (!response.ok) {
            throw new Error('Network response was not ok: ' + response.status);
        }

        const result = await response.json();
        console.log('Documents result:', result);

        if (result.success && result.documents && result.documents.length > 0) {
            renderDocuments(result.documents, container);
            console.log('Loaded', result.documents.length, 'documents');
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
            <div class="document-card" data-category="${category}" data-name="${escapeHtml(doc.name).toLowerCase()}">
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
    console.log('Downloading document:', documentId);
    window.open(`/portfolio/download/${documentId}`, '_blank');
}

async function deleteDocument(documentId) {
    console.log('Deleting document:', documentId);

    if (!confirm('Are you sure you want to delete this document?')) {
        return;
    }

    try {
        const response = await fetch(`/portfolio/api/delete-document/${documentId}`, {
            method: 'DELETE'
        });

        console.log('Delete document response status:', response.status);

        if (!response.ok) {
            throw new Error('Network response was not ok: ' + response.status);
        }

        const result = await response.json();
        console.log('Delete document result:', result);

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
    console.log('Viewing document details:', documentId);

    try {
        const response = await fetch(`/portfolio/item/${documentId}`);
        if (!response.ok) {
            throw new Error('Network response was not ok: ' + response.status);
        }

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
    const buttons = document.querySelectorAll('.filter-btn');
    buttons.forEach(btn => btn.classList.remove('active'));

    const clickedBtn = event.target.closest('.filter-btn');
    if (clickedBtn) clickedBtn.classList.add('active');

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

// ===================== TAB SYSTEM =====================
function setupTabSwitching() {
    console.log('Setting up tab switching...');

    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const tabName = this.getAttribute('data-tab');
            if (tabName) {
                switchTab(tabName);
            }
        });
    });
}

function switchTab(tabName) {
    console.log('Switching to tab:', tabName);

    // Hide all tab content
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });

    // Deactivate all tab buttons
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    // Show selected tab content
    const selectedTab = document.getElementById(tabName);
    if (selectedTab) {
        selectedTab.classList.add('active');
    }

    // Activate clicked tab button
    document.querySelectorAll('.tab-btn').forEach(btn => {
        if (btn.getAttribute('data-tab') === tabName) {
            btn.classList.add('active');
        }
    });

    // Load data for specific tabs
    if (tabName === 'documents') {
        loadDocuments();
    } else if (tabName === 'department-portfolios' && portfolioState.userRole === 'DEPT_HEAD') {
        loadDepartmentPortfolios();
    } else if (tabName === 'college-portfolios' && portfolioState.userRole === 'DEAN') {
        loadCollegePortfolios();
    }
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

function getFileIcon(file) {
    const extension = file.name.split('.').pop().toLowerCase();
    const type = file.type.toLowerCase();

    if (type.includes('pdf')) return 'fa-file-pdf';
    if (type.includes('word') || type.includes('document') || ['doc', 'docx'].includes(extension)) return 'fa-file-word';
    if (type.includes('excel') || type.includes('sheet') || ['xls', 'xlsx'].includes(extension)) return 'fa-file-excel';
    if (type.includes('powerpoint') || type.includes('presentation') || ['ppt', 'pptx'].includes(extension)) return 'fa-file-powerpoint';
    if (type.includes('image')) return 'fa-file-image';
    if (['zip', 'rar', '7z'].includes(extension)) return 'fa-file-archive';
    if (['txt', 'text'].includes(extension)) return 'fa-file-alt';
    return 'fa-file';
}

// ===================== TOAST NOTIFICATION SYSTEM =====================
function setupToastSystem() {
    if (!document.querySelector('#toast-styles')) {
        const style = document.createElement('style');
        style.id = 'toast-styles';
        style.textContent = `
            .toast {
                position: fixed;
                top: 20px;
                right: 20px;
                background: white;
                padding: 15px 20px;
                border-radius: 8px;
                box-shadow: 0 5px 15px rgba(0,0,0,0.2);
                display: flex;
                align-items: center;
                gap: 12px;
                z-index: 10000;
                opacity: 0;
                transform: translateX(100%);
                transition: all 0.3s ease;
                border-left: 4px solid #ccc;
                max-width: 400px;
                min-width: 300px;
            }
            .toast.show {
                opacity: 1;
                transform: translateX(0);
            }
            .toast-success { border-left-color: #28a745; color: #155724; background: #d4edda; }
            .toast-error { border-left-color: #dc3545; color: #721c24; background: #f8d7da; }
            .toast-warning { border-left-color: #ffc107; color: #856404; background: #fff3cd; }
            .toast-info { border-left-color: #17a2b8; color: #0c5460; background: #d1ecf1; }
            .toast i { font-size: 20px; }
            .toast-close {
                margin-left: auto;
                background: none;
                border: none;
                color: inherit;
                cursor: pointer;
                padding: 5px;
            }
        `;
        document.head.appendChild(style);
    }
}

function showToast(message, type = 'info', duration = 5000) {
    const existingToasts = document.querySelectorAll('.toast');
    existingToasts.forEach(toast => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    });

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    const icon = type === 'success' ? 'fa-check-circle' :
                 type === 'error' ? 'fa-exclamation-circle' :
                 type === 'warning' ? 'fa-exclamation-triangle' :
                 'fa-info-circle';

    toast.innerHTML = `
        <i class="fas ${icon}"></i>
        <span>${escapeHtml(message)}</span>
        <button class="toast-close" onclick="this.parentElement.remove()">
            <i class="fas fa-times"></i>
        </button>
    `;

    document.body.appendChild(toast);

    setTimeout(() => toast.classList.add('show'), 10);

    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, duration);
}

function showSuccess(message) {
    console.log('Success:', message);
    showToast(message, 'success');
}

function showError(message) {
    console.error('Error:', message);
    showToast(message, 'error');
}

function showWarning(message) {
    console.warn('Warning:', message);
    showToast(message, 'warning');
}

// ===================== DETAILS PAGE FUNCTIONS =====================
function openFolder(folderId) {
    const portfolioId = window.currentPortfolioId || portfolioState.currentPortfolioId;
    if (!portfolioId) {
        alert('No portfolio selected');
        return;
    }
    window.location.href = `/portfolio/details/${portfolioId}?folderId=${folderId}`;
}

function viewItem(itemId) {
    window.open(`/portfolio/download/${itemId}`, '_blank');
}

function downloadItem(itemId, event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    window.open(`/portfolio/download/${itemId}`, '_blank');
}

async function deleteItem(itemId, event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }

    if (!confirm('Are you sure you want to delete this item?')) {
        return;
    }

    try {
        const response = await fetch(`/portfolio/delete-item/${itemId}`, {
            method: 'DELETE',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        const result = await response.json();

        if (result.success) {
            alert('Item deleted successfully');
            window.location.reload();
        } else {
            alert(result.message || 'Failed to delete item');
        }
    } catch (error) {
        console.error('Delete error:', error);
        alert('Network error. Please try again.');
    }
}

// ===================== GLOBAL EXPORTS =====================
window.initPortfolioSystem = initPortfolioSystem;
window.showCreatePortfolioModal = showCreatePortfolioModal;
window.createPortfolio = createPortfolio;
window.showUploadModal = showUploadModal;
window.closeModal = closeModal;
window.uploadFiles = uploadFiles;
window.deletePortfolio = deletePortfolio;
window.toggleUploadDestination = toggleUploadDestination;
window.removeFilePreview = removeFilePreview;
window.loadCollegePortfolios = loadCollegePortfolios;
window.loadDocuments = loadDocuments;
window.searchDocuments = searchDocuments;
window.filterDocuments = filterDocuments;
window.downloadDocument = downloadDocument;
window.deleteDocument = deleteDocument;
window.viewDocumentDetails = viewDocumentDetails;
window.switchTab = switchTab;
window.openFolder = openFolder;
window.viewItem = viewItem;
window.downloadItem = downloadItem;
window.deleteItem = deleteItem;

// Initialize on DOM ready with safety check
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
        // Add a small delay to ensure all elements are loaded
        setTimeout(initPortfolioSystem, 100);
    });
} else {
    setTimeout(initPortfolioSystem, 100);
}