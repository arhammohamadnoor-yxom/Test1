// Custom JavaScript for School Attendance App

$(document).ready(function() {
    // Initialize tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Initialize popovers
    var popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    var popoverList = popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });

    // Auto-hide alerts after 5 seconds
    setTimeout(function() {
        $('.alert').fadeOut('slow');
    }, 5000);

    // Initialize datepickers
    $('.datepicker').datepicker({
        format: 'yyyy-mm-dd',
        autoclose: true,
        todayHighlight: true
    });

    // Initialize timepickers
    $('.timepicker').timepicker({
        showSeconds: false,
        showMeridian: false,
        defaultTime: 'current'
    });
});

// Utility Functions
function showAlert(type, message, containerId = 'alert-container') {
    const alertHtml = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            <i class="bi bi-${getAlertIcon(type)} me-2"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;

    let container = $('#' + containerId);
    if (container.length === 0) {
        container = $('<div id="' + containerId + '"></div>');
        $('main').prepend(container);
    }

    container.html(alertHtml);

    // Auto-hide after 5 seconds
    setTimeout(function() {
        container.find('.alert').fadeOut('slow');
    }, 5000);
}

function getAlertIcon(type) {
    const icons = {
        'success': 'check-circle-fill',
        'danger': 'exclamation-triangle-fill',
        'warning': 'exclamation-triangle-fill',
        'info': 'info-circle-fill',
        'primary': 'info-circle-fill',
        'secondary': 'info-circle-fill'
    };
    return icons[type] || 'info-circle-fill';
}

function confirmDelete(message = 'Are you sure you want to delete this item?') {
    return confirm(message);
}

function confirmAction(message = 'Are you sure you want to perform this action?') {
    return confirm(message);
}

// Loading state management
function showLoading(element, text = 'Loading...') {
    const originalText = $(element).text();
    $(element).data('original-text', originalText);
    $(element).html('<span class="spinner-border spinner-border-sm me-2"></span>' + text);
    $(element).prop('disabled', true);
}

function hideLoading(element) {
    const originalText = $(element).data('original-text');
    $(element).text(originalText);
    $(element).prop('disabled', false);
}

// AJAX error handler
function handleAjaxError(xhr, status, error) {
    let message = 'An error occurred while processing your request.';

    if (xhr.responseJSON && xhr.responseJSON.message) {
        message = xhr.responseJSON.message;
    } else if (xhr.responseText) {
        try {
            const response = JSON.parse(xhr.responseText);
            message = response.message || message;
        } catch (e) {
            // Use default message
        }
    }

    showAlert('danger', message);
}

// Form validation
function validateForm(form) {
    const $form = $(form);
    let isValid = true;

    $form.find('input[required], select[required], textarea[required]').each(function() {
        if (!$(this).val().trim()) {
            $(this).addClass('is-invalid');
            isValid = false;
        } else {
            $(this).removeClass('is-invalid');
        }
    });

    $form.find('input[type="email"]').each(function() {
        const email = $(this).val().trim();
        if (email && !isValidEmail(email)) {
            $(this).addClass('is-invalid');
            isValid = false;
        }
    });

    return isValid;
}

function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// Date and time formatting
function formatDateTime(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleString();
}

function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString();
}

function formatTime(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
}

// Table utilities
function exportTableToCSV(tableId, filename = 'export.csv') {
    const table = document.getElementById(tableId);
    let csv = [];

    // Get headers
    const headers = [];
    table.querySelectorAll('thead th').forEach(th => {
        headers.push(th.textContent.trim());
    });
    csv.push(headers.join(','));

    // Get data rows
    table.querySelectorAll('tbody tr').forEach(tr => {
        const row = [];
        tr.querySelectorAll('td').forEach(td => {
            // Remove HTML tags and clean up text
            let text = td.textContent.trim();
            text = text.replace(/"/g, '""'); // Escape quotes
            if (text.includes(',') || text.includes('"')) {
                text = '"' + text + '"'; // Wrap in quotes if contains comma or quote
            }
            row.push(text);
        });
        csv.push(row.join(','));
    });

    // Create download link
    const csvContent = csv.join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
}

// Print table
function printTable(tableId) {
    const table = document.getElementById(tableId);
    const printWindow = window.open('', '_blank');

    printWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>Print Table</title>
            <style>
                body { font-family: Arial, sans-serif; }
                table { border-collapse: collapse; width: 100%; }
                th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                th { background-color: #f2f2f2; }
                @media print { body { margin: 1cm; } }
            </style>
        </head>
        <body>
            ${table.outerHTML}
        </body>
        </html>
    `);

    printWindow.document.close();
    printWindow.print();
}

// Local storage utilities
function saveToLocalStorage(key, data) {
    try {
        localStorage.setItem(key, JSON.stringify(data));
    } catch (e) {
        console.error('Error saving to localStorage:', e);
    }
}

function getFromLocalStorage(key, defaultValue = null) {
    try {
        const data = localStorage.getItem(key);
        return data ? JSON.parse(data) : defaultValue;
    } catch (e) {
        console.error('Error reading from localStorage:', e);
        return defaultValue;
    }
}

function removeFromLocalStorage(key) {
    try {
        localStorage.removeItem(key);
    } catch (e) {
        console.error('Error removing from localStorage:', e);
    }
}

// Number formatting
function formatNumber(num) {
    return new Intl.NumberFormat().format(num);
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(amount);
}

// Debounce function
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Throttle function
function throttle(func, limit) {
    let inThrottle;
    return function() {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    }
}

// Copy to clipboard
function copyToClipboard(text) {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
            showAlert('success', 'Copied to clipboard!');
        }).catch(err => {
            console.error('Failed to copy: ', err);
            showAlert('danger', 'Failed to copy to clipboard');
        });
    } else {
        // Fallback for older browsers
        const textArea = document.createElement('textarea');
        textArea.value = text;
        document.body.appendChild(textArea);
        textArea.select();
        try {
            document.execCommand('copy');
            showAlert('success', 'Copied to clipboard!');
        } catch (err) {
            console.error('Failed to copy: ', err);
            showAlert('danger', 'Failed to copy to clipboard');
        }
        document.body.removeChild(textArea);
    }
}