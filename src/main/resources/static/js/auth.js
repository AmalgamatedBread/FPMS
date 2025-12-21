// Handle registration form submission
document.getElementById('registerForm').addEventListener('submit', async function(e) {
    e.preventDefault();  // Prevent default form submission

    const userData = {
        firstName: document.getElementById('first-name').value,
        middleName: document.getElementById('middle-name').value || null,
        lastName: document.getElementById('last-name').value,
        suffix: document.getElementById('suffix').value || null,
        role: mapRole(document.getElementById('role').value),
        contactNo: document.getElementById('contact-no').value,    // Changed from telNo
        email: document.getElementById('email').value,
        address: document.getElementById('address').value,
        department: mapDepartment(document.getElementById('department').value), // Changed from deptCode
        username: document.getElementById('reg-username').value,
        password: document.getElementById('reg-password').value
    };

    console.log('Sending registration data:', userData);

    // Show loading
    const registerButton = document.getElementById('register-button');
    const originalText = registerButton.textContent;
    registerButton.textContent = 'Creating account...';
    registerButton.disabled = true;

    try {
        const response = await fetch('/api/auth/register', {  // Removed localhost:8080, use relative path
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(userData)
        });

        console.log('Response status:', response.status);

        if (response.ok) {
            const result = await response.json();
            console.log('Registration successful:', result);
            showMessage('Registration successful! Redirecting to login...', 'success');

            // Redirect to login page after 2 seconds
            setTimeout(() => {
                window.location.href = '/login';  // Use controller endpoint, not direct file
            }, 2000);

        } else {
            // Try to parse error as JSON
            try {
                const errorData = await response.json();
                console.error('Registration failed with details:', errorData);
                
                // Display validation errors if available
                if (errorData.errors && errorData.errors.length > 0) {
                    const errorMessages = errorData.errors.map(err => 
                        `${err.field}: ${err.defaultMessage}`
                    ).join('\n');
                    showMessage('Validation errors:\n' + errorMessages);
                } else {
                    showMessage(errorData.message || 'Registration failed');
                }
            } catch (jsonError) {
                // If not JSON, show text response
                const textError = await response.text();
                showMessage('Registration failed: ' + textError);
            }
        }

    } catch (error) {
        console.error('Network error:', error);
        showMessage('Network error. Please check your connection.');
    } finally {
        // Reset button
        registerButton.textContent = originalText;
        registerButton.disabled = false;
    }
});