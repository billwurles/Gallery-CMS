// The full-size painting view: one modal for the whole gallery, filled from each thumbnail's data-*.
// Arrows, the arrow keys and swiping move between paintings; Close, Escape or a click on the dark
// background closes it. Keyboard focus stays inside while it's open.
(function () {
    let thumbs = [];
    let current = -1;
    let modal, img, title, details, count, opener;

    function show(index) {
        current = (index + thumbs.length) % thumbs.length; // wraps around at both ends
        const t = thumbs[current].dataset;
        img.src = t.src;
        img.alt = t.title || '';
        title.textContent = t.title || '';
        details.replaceChildren(...[t.dimensions, t.medium, t.sold]
            .filter(v => v && v.trim())
            .map(v => Object.assign(document.createElement('span'), {textContent: v})));
        count.textContent = thumbs.length > 1 ? `${current + 1} of ${thumbs.length}` : '';
        // Fetch the neighbours now, so the next arrow press shows its painting straight away
        [current - 1, current + 1].forEach(i => { new Image().src = thumbs[(i + thumbs.length) % thumbs.length].dataset.src; });
    }

    function open(index) {
        opener = thumbs[index];
        show(index);
        modal.hidden = false;
        modal.classList.add('open');
        document.body.classList.add('modal-open');
        modal.querySelector('.modal-close').focus();
    }

    function close() {
        modal.classList.remove('open');
        modal.hidden = true;
        document.body.classList.remove('modal-open');
        if (opener) opener.focus();
    }

    function isOpen() {
        return modal && !modal.hidden;
    }

    document.addEventListener('DOMContentLoaded', function () {
        modal = document.getElementById('painting-modal');
        if (!modal) return;
        thumbs = Array.from(document.querySelectorAll('.painting-open'));
        img = modal.querySelector('img');
        title = modal.querySelector('.modal-title');
        details = modal.querySelector('.modal-details');
        count = modal.querySelector('.modal-count');
        if (thumbs.length < 2) modal.classList.add('single');

        thumbs.forEach((thumb, i) => {
            thumb.addEventListener('click', () => open(i));
            thumb.addEventListener('keydown', e => {
                if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); open(i); }
            });
        });

        modal.querySelector('.modal-close').addEventListener('click', close);
        modal.querySelector('.modal-prev').addEventListener('click', () => show(current - 1));
        modal.querySelector('.modal-next').addEventListener('click', () => show(current + 1));
        // A click on the dark background closes; a click on the painting or its caption doesn't
        modal.addEventListener('click', e => { if (e.target === modal) close(); });

        document.addEventListener('keydown', e => {
            if (!isOpen()) return;
            if (e.key === 'Escape') close();
            else if (e.key === 'ArrowLeft') show(current - 1);
            else if (e.key === 'ArrowRight') show(current + 1);
            else if (e.key === 'Tab') {
                // Keep focus on the modal's buttons
                const buttons = Array.from(modal.querySelectorAll('button')).filter(b => b.offsetParent !== null);
                const at = buttons.indexOf(document.activeElement);
                e.preventDefault();
                buttons[(at + (e.shiftKey ? -1 : 1) + buttons.length) % buttons.length].focus();
            }
        });

        // Swipe left or right on a phone
        let startX = null, startY = null;
        modal.addEventListener('touchstart', e => {
            startX = e.touches[0].clientX; startY = e.touches[0].clientY;
        }, {passive: true});
        modal.addEventListener('touchend', e => {
            if (startX === null) return;
            const dx = e.changedTouches[0].clientX - startX, dy = e.changedTouches[0].clientY - startY;
            if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy)) show(current + (dx < 0 ? 1 : -1));
            startX = null;
        });
    });
})();
