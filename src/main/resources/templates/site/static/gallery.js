// The full-size painting view. It opens with the "open" class, and closes on the button,
// a click on the dark background, or Escape.
function showModal(paintingId) {
    document.querySelector(`#modal-${paintingId}`).classList.add('open');
    document.body.classList.add('modal-open');
}

function closeModal(paintingId, event) {
    // A click on the painting or its caption inside the modal doesn't close it
    if (event && event.target.closest('.modal-content')) return;
    document.querySelector(`#modal-${paintingId}`).classList.remove('open');
    if (!document.querySelector('.painting-modal.open')) document.body.classList.remove('modal-open');
}

document.addEventListener('keydown', function (e) {
    if (e.key !== 'Escape') return;
    document.querySelectorAll('.painting-modal.open').forEach(m => m.classList.remove('open'));
    document.body.classList.remove('modal-open');
});
