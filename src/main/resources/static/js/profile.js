// profile.js - Profile page JavaScript

// Global variables
let currentUser = {};
let csrfToken = '';
let csrfHeader = '';

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    console.log('Profile page JavaScript loaded');

    // Get CSRF token from meta tag
    const csrfMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

    if (csrfMeta) {
        csrfToken = csrfMeta.content;
        console.log('CSRF token found');
    } else {
        console.warn('CSRF token meta tag not found!');
    }

    if (csrfHeaderMeta) {
        csrfHeader = csrfHeaderMeta.content;
    } else {
        csrfHeader = 'X-CSRF-TOKEN'; // Default header name
    }

    // Initialize event listeners
    initializeEventListeners();

    // Add click handler for Change Photo button
    const changePhotoBtn = document.querySelector('.edit-avatar-btn');
    if (changePhotoBtn) {
        changePhotoBtn.addEventListener('click', handleChangePhoto);
    }
});

// Initialize all event listeners
function initializeEventListeners() {
    console.log('Initializing event listeners');

    // Edit profile form
    const editForm = document.getElementById('editProfileForm');
    if (editForm) {
        editForm.addEventListener('submit', handleEditProfileSubmit);
        console.log('Edit form listener attached');
    } else {
        console.error('Edit form not found!');
    }

    // Change password form
    const passwordForm = document.getElementById('changePasswordForm');
    if (passwordForm) {
        passwordForm.addEventListener('submit', handleChangePasswordSubmit);
        console.log('Password form listener attached');
    } else {
        console.error('Password form not found!');
    }

    // Close modals when clicking outside
    window.onclick = function(event) {
        const editModal = document.getElementById('editProfileModal');
        const passwordModal = document.getElementById('changePasswordModal');

        if (event.target === editModal) {
            closeEditProfileModal();
        }
        if (event.target === passwordModal) {
            closeChangePasswordModal();
        }
    };

    // Close modals with Escape key
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape') {
            closeEditProfileModal();
            closeChangePasswordModal();
        }
    });
}

// Modal Functions
function showEditProfileModal() {
    console.log('Opening edit profile modal');

    // Fill form with current data
    document.getElementById('firstName').value = currentUser.firstName || '';
    document.getElementById('middleName').value = currentUser.middleName || '';
    document.getElementById('lastName').value = currentUser.lastName || '';
    document.getElementById('suffix').value = currentUser.suffix || '';
    document.getElementById('contactNo').value = currentUser.telNo || '';
    document.getElementById('address').value = currentUser.address || '';

    // Clear errors
    clearErrors();
    document.getElementById('editProfileModal').style.display = 'block';
}

function closeEditProfileModal() {
    console.log('Closing edit profile modal');
    document.getElementById('editProfileModal').style.display = 'none';
    document.getElementById('editProfileForm').reset();
    clearErrors();
}

function showChangePasswordModal() {
    console.log('Opening change password modal');
    clearErrors();
    document.getElementById('changePasswordModal').style.display = 'block';
}

function closeChangePasswordModal() {
    console.log('Closing change password modal');
    document.getElementById('changePasswordModal').style.display = 'none';
    document.getElementById('changePasswordForm').reset();
    clearErrors();
}

/ Handle Change Photo button
function handleChangePhoto() {
    console.log('Change photo clicked');

    // Create file input
    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.accept = 'image/*';
    fileInput.style.display = 'none';

    fileInput.addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (file) {
            if (file.size > 5 * 1024 * 1024) { // 5MB limit
                showToast('File size should be less than 5MB', 'error');
                return;
            }

            const validTypes = ['image/jpeg', 'image/png', 'image/gif'];
            if (!validTypes.includes(file.type)) {
                showToast('Please select a valid image (JPEG, PNG, GIF)', 'error');
                return;
            }

            // Show preview
            const reader = new FileReader();
            reader.onload = function(e) {
                const avatar = document.querySelector('.avatar');
                if (avatar) {
                    avatar.src = e.target.result;
                    showToast('Profile picture updated (preview)', 'success');
                }
            };
            reader.readAsDataURL(file);

            // Actually upload to server - THIS WAS COMMENTED OUT!
            uploadProfilePhoto(file);
        }
    });

    document.body.appendChild(fileInput);
    fileInput.click();
    document.body.removeChild(fileInput);
}

/// Upload profile photo to server
 async function uploadProfilePhoto(file) {
     showLoading();

     const formData = new FormData();
     formData.append('file', file); // Changed from 'profilePhoto' to 'file'

     try {
         const response = await fetch('/api/profile/upload-photo', {
             method: 'POST',
             headers: {
                 // Don't set Content-Type for FormData, browser sets it automatically
                 [csrfHeader]: csrfToken
             },
             body: formData
         });

         const result = await response.json();
         console.log('Upload response:', result);

         if (response.ok && result.success) {
             showToast('Profile picture updated successfully!', 'success');

             // Update the avatar image with the new URL
             const avatar = document.querySelector('.avatar');
             if (avatar && result.photoUrl) {
                 // Add timestamp to prevent caching
                 avatar.src = result.photoUrl + '?t=' + new Date().getTime();
             }

             // Refresh the page after 2 seconds to show the new image
             setTimeout(() => {
                 window.location.reload();
             }, 2000);
         } else {
             showToast(result.error || 'Failed to upload photo', 'error');
         }
     } catch (error) {
         console.error('Upload error:', error);
         showToast('Failed to upload photo. Please try again.', 'error');
     } finally {
         hideLoading();
     }
 }

// Clear all error messages
function clearErrors() {
    document.querySelectorAll('.error-message').forEach(el => {
        el.style.display = 'none';
        el.textContent = '';
    });
}

// Show error message
function showError(fieldId, message) {
    const errorEl = document.getElementById(fieldId + 'Error');
    if (errorEl) {
        errorEl.textContent = message;
        errorEl.style.display = 'block';
    }
}

// Show loading overlay
function showLoading() {
    document.getElementById('loadingOverlay').style.display = 'flex';
}

// Hide loading overlay
function hideLoading() {
    document.getElementById('loadingOverlay').style.display = 'none';
}

// Show toast notification
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast ' + type;
    toast.style.display = 'block';

    setTimeout(() => {
        toast.style.display = 'none';
    }, 3000);
}

// Update profile display
function updateProfileDisplay(data) {
    console.log('Updating profile display with:', data);

    if (data.firstName) {
        document.getElementById('first-name-display').textContent = data.firstName;
        currentUser.firstName = data.firstName;
    }
    if (data.middleName !== undefined) {
        const middleNameDisplay = document.getElementById('middle-name-display');
        if (data.middleName) {
            middleNameDisplay.textContent = data.middleName;
            middleNameDisplay.style.display = 'inline';
        } else {
            middleNameDisplay.style.display = 'none';
        }
        currentUser.middleName = data.middleName;
    }
    if (data.lastName) {
        document.getElementById('last-name-display').textContent = data.lastName;
        currentUser.lastName = data.lastName;
    }
    if (data.suffix !== undefined) {
        const suffixDisplay = document.getElementById('suffix-display');
        if (data.suffix) {
            suffixDisplay.textContent = data.suffix;
            suffixDisplay.style.display = 'inline';
        } else {
            suffixDisplay.style.display = 'none';
        }
        currentUser.suffix = data.suffix;
    }
    if (data.contactNo !== undefined) {
        const phoneDisplay = document.getElementById('phone-display');
        phoneDisplay.textContent = data.contactNo || 'Not provided';
        currentUser.telNo = data.contactNo;
    }
    if (data.address !== undefined) {
        const addressDisplay = document.getElementById('address-display');
        addressDisplay.textContent = data.address || 'Not provided';
        currentUser.address = data.address;
    }
}

// Handle edit profile form submission
async function handleEditProfileSubmit(e) {
    e.preventDefault();
    console.log('Edit profile form submitted');

    // Validate form
    const firstName = document.getElementById('firstName').value.trim();
    const lastName = document.getElementById('lastName').value.trim();

    if (!firstName) {
        showError('firstName', 'First name is required');
        return;
    }
    if (!lastName) {
        showError('lastName', 'Last name is required');
        return;
    }

    // Prepare data
    const formData = {
        firstName: firstName,
        middleName: document.getElementById('middleName').value.trim() || null,
        lastName: lastName,
        suffix: document.getElementById('suffix').value.trim() || null,
        contactNo: document.getElementById('contactNo').value.trim() || null,
        address: document.getElementById('address').value.trim() || null
    };

    console.log('Sending profile update:', formData);
    console.log('CSRF Token:', csrfToken ? 'Present' : 'Missing');
    console.log('CSRF Header:', csrfHeader);

    showLoading();

    try {
        const response = await fetch('/api/profile/update', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify(formData)
        });

        console.log('Response status:', response.status);
        console.log('Response headers:', response.headers);

        const result = await response.json();
        console.log('Response data:', result);

        if (response.ok && result.success) {
            // Update display with new data
            updateProfileDisplay(formData);
            showToast('Profile updated successfully!');
            closeEditProfileModal();
        } else {
            const errorMsg = result.error || 'Failed to update profile';
            console.error('Server error:', errorMsg);
            showToast(errorMsg, 'error');
        }
    } catch (error) {
        console.error('Network error:', error);
        showToast('Network error. Please check your connection and try again.', 'error');
    } finally {
        hideLoading();
    }
}

// Handle change password form submission
async function handleChangePasswordSubmit(e) {
    e.preventDefault();
    console.log('Change password form submitted');

    // Validate form
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    clearErrors();

    if (!currentPassword) {
        showError('currentPassword', 'Current password is required');
        return;
    }
    if (!newPassword) {
        showError('newPassword', 'New password is required');
        return;
    }
    if (newPassword.length < 6) {
        showError('newPassword', 'Password must be at least 6 characters');
        return;
    }
    if (newPassword !== confirmPassword) {
        showError('confirmPassword', 'Passwords do not match');
        return;
    }

    showLoading();

    try {
        const response = await fetch('/api/profile/change-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({
                currentPassword: currentPassword,
                newPassword: newPassword
            })
        });

        console.log('Password change response status:', response.status);
        const result = await response.json();
        console.log('Password change response:', result);

        if (response.ok && result.success) {
            showToast('Password changed successfully!');
            closeChangePasswordModal();
        } else {
            const errorMsg = result.error || 'Failed to change password';
            console.error('Password change error:', errorMsg);
            showToast(errorMsg, 'error');
        }
    } catch (error) {
        console.error('Network error:', error);
        showToast('Network error. Please check your connection and try again.', 'error');
    } finally {
        hideLoading();
    }
}

// Make functions available globally
window.showEditProfileModal = showEditProfileModal;
window.showChangePasswordModal = showChangePasswordModal;
window.closeEditProfileModal = closeEditProfileModal;
window.closeChangePasswordModal = closeChangePasswordModal;
console.log('Global functions attached to window');