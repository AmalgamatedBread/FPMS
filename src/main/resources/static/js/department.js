// department.js - Department page functionality

document.addEventListener('DOMContentLoaded', function() {
    console.log('Department page loaded');

    // Initialize tooltips
    initTooltips();

    // Initialize modals
    initModals();
});

// Contact person function
function contactPerson(email) {
    if (!email) {
        alert('No email address available.');
        return;
    }

    if (confirm(`Would you like to send an email to ${email}?`)) {
        window.location.href = `mailto:${email}`;
    }
}

// Schedule meeting function
function scheduleMeeting(personName) {
    alert(`Scheduling a meeting with ${personName}\n\nThis would open your calendar application.`);
    // In a real implementation, this would integrate with a calendar API
}

// Report to person function
function reportToPerson(personName) {
    alert(`Preparing report for ${personName}\n\nThis would open a reporting form.`);
}

// Manage department options
function showManageOptions() {
    const options = [
        'Edit Department Info',
        'Manage Faculty Assignments',
        'Assign Department Head',
        'Update Office Location',
        'Generate Department Report'
    ];

    let optionList = options.map((opt, index) => `${index + 1}. ${opt}`).join('\n');

    const choice = prompt(
        `Manage Department Options:\n\n${optionList}\n\nEnter option number:`
    );

    if (choice) {
        const index = parseInt(choice) - 1;
        if (index >= 0 && index < options.length) {
            alert(`Selected: ${options[index]}\n\nThis functionality would open the appropriate management interface.`);
        } else {
            alert('Invalid option selected.');
        }
    }
}

// Add new faculty
function addNewFaculty() {
    alert('Add New Faculty\n\nThis would open a form to add new faculty members to the department.');
    // Implementation would include:
    // 1. Form to enter faculty details
    // 2. API call to add faculty
    // 3. Success/error handling
}

// Edit faculty list
function editFacultyList() {
    alert('Edit Faculty List\n\nThis would open an interface to modify faculty assignments, roles, and information.');
    // Implementation would include:
    // 1. List of current faculty with edit options
    // 2. Ability to remove or reassign faculty
    // 3. Role modification
}

// Generate report
function generateReport() {
    const reportTypes = [
        'Faculty Statistics Report',
        'Department Overview Report',
        'Leadership Structure Report',
        'Complete Department Report'
    ];

    let reportList = reportTypes.map((type, index) => `${index + 1}. ${type}`).join('\n');

    const choice = prompt(
        `Generate Report:\n\n${reportList}\n\nEnter report type number:`
    );

    if (choice) {
        const index = parseInt(choice) - 1;
        if (index >= 0 && index < reportTypes.length) {
            if (confirm(`Generate ${reportTypes[index]}? This may take a few moments.`)) {
                // Simulate report generation
                setTimeout(() => {
                    alert(`${reportTypes[index]} generated successfully!\n\nThe report has been downloaded.`);
                }, 1500);
            }
        } else {
            alert('Invalid report type selected.');
        }
    }
}

// Initialize tooltips
function initTooltips() {
    const tooltipElements = document.querySelectorAll('[data-tooltip]');

    tooltipElements.forEach(element => {
        element.addEventListener('mouseenter', function(e) {
            const tooltip = document.createElement('div');
            tooltip.className = 'custom-tooltip';
            tooltip.textContent = this.getAttribute('data-tooltip');
            tooltip.style.position = 'absolute';
            tooltip.style.background = '#1e293b';
            tooltip.style.color = 'white';
            tooltip.style.padding = '8px 12px';
            tooltip.style.borderRadius = '4px';
            tooltip.style.fontSize = '0.85rem';
            tooltip.style.zIndex = '1000';
            tooltip.style.maxWidth = '250px';

            document.body.appendChild(tooltip);

            const rect = this.getBoundingClientRect();
            tooltip.style.left = (rect.left + window.scrollX) + 'px';
            tooltip.style.top = (rect.top + window.scrollY - tooltip.offsetHeight - 10) + 'px';

            this._tooltip = tooltip;
        });

        element.addEventListener('mouseleave', function() {
            if (this._tooltip) {
                document.body.removeChild(this._tooltip);
                this._tooltip = null;
            }
        });
    });
}

// Initialize modals
function initModals() {
    // You can add modal initialization here if needed
    console.log('Modals initialized');
}

// Export attendance data (example function)
function exportAttendanceData() {
    if (confirm('Export department attendance data? This will generate a CSV file.')) {
        // Simulate CSV generation and download
        const csvContent = 'data:text/csv;charset=utf-8,';
        const encodedUri = encodeURI(csvContent);
        const link = document.createElement('a');
        link.setAttribute('href', encodedUri);
        link.setAttribute('download', 'department_attendance.csv');
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
}

// Refresh department data
function refreshDepartmentData() {
    if (confirm('Refresh department data? This will reload the latest information.')) {
        window.location.reload();
    }
}

// Keyboard shortcuts
document.addEventListener('keydown', function(e) {
    // Ctrl + R to refresh (but allow browser refresh)
    if (e.ctrlKey && e.key === 'r') {
        e.preventDefault();
        refreshDepartmentData();
    }

    // Ctrl + E to export
    if (e.ctrlKey && e.key === 'e') {
        e.preventDefault();
        exportAttendanceData();
    }

    // Escape to close any open modals
    if (e.key === 'Escape') {
        // Add modal closing logic here
    }
});