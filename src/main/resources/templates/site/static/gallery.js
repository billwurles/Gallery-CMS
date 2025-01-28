function showModal(paintingId) {
    // Show modal using the ID
    document.querySelector(`#modal-${paintingId}`).style.display = 'block';
}

function closeModal(paintingId) {
    // Close modal using the ID
    document.querySelector(`#modal-${paintingId}`).style.display = 'none';
}