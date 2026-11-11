// Shared by every editor page: toasts, a confirm dialog, delete buttons and Publish.

/** A short message in the corner of the screen. kind: "ok" (default) or "error". */
function toast(message, kind) {
    let stack = document.getElementById("toasts");
    if (!stack) {
        stack = document.createElement("div");
        stack.id = "toasts";
        stack.setAttribute("aria-live", "polite");
        document.body.appendChild(stack);
    }
    const item = document.createElement("div");
    item.className = "toast" + (kind === "error" ? " toast-error" : "");
    item.textContent = message;
    stack.appendChild(item);
    setTimeout(() => {
        item.classList.add("toast-out");
        setTimeout(() => item.remove(), 300);
    }, kind === "error" ? 8000 : 3500);
}

/** A toast that shows after the next page load, for actions that reload or redirect. */
function flash(message, kind) {
    try {
        sessionStorage.setItem("flash", JSON.stringify({message: message, kind: kind}));
    } catch (e) { /* no storage: the message is just not shown */ }
}

/** "just now", "5 min ago", "today 14:02", "10 Nov 14:02": for the publish status times. */
function friendlyTime(date) {
    const minutes = Math.round((Date.now() - date.getTime()) / 60000);
    if (minutes < 1) return "just now";
    if (minutes < 60) return minutes + " min ago";
    const time = date.toLocaleTimeString([], {hour: "2-digit", minute: "2-digit"});
    if (date.toDateString() === new Date().toDateString()) return "today " + time;
    return date.toLocaleDateString([], {day: "numeric", month: "short"}) + " " + time;
}

function showTimes() {
    document.querySelectorAll(".publish-status time[datetime]").forEach(t => {
        const date = new Date(t.getAttribute("datetime"));
        if (!isNaN(date)) t.textContent = (t.closest(".published-at") ? "" : "since ") + friendlyTime(date);
    });
}

/** After a change is saved without a page load: the live site is now behind the draft. */
function markUnpublished() {
    const status = document.querySelector(".publish-status");
    const button = document.querySelector(".sidebar-actions .btn");
    if (button) { button.classList.add("btn-publish"); button.classList.remove("btn-published"); }
    if (!status) return;
    const published = status.querySelector(".published-at");
    status.innerHTML = '<span class="dot dot-changed"></span>Unpublished changes <time datetime="' + new Date().toISOString() + '"></time>';
    if (published) status.appendChild(published);
    showTimes();
}

/** After a publish: the live site matches the draft. */
function markPublished() {
    const status = document.querySelector(".publish-status");
    const button = document.querySelector(".sidebar-actions .btn");
    if (button) { button.classList.remove("btn-publish"); button.classList.add("btn-published"); }
    if (!status) return;
    status.innerHTML = '<span class="dot dot-live"></span>Live site is up to date <span class="published-at">Published <time datetime="'
        + new Date().toISOString() + '"></time></span>';
    showTimes();
}

document.addEventListener("DOMContentLoaded", function () {
    showTimes();
    try {
        const stored = sessionStorage.getItem("flash");
        if (stored) {
            sessionStorage.removeItem("flash");
            const {message, kind} = JSON.parse(stored);
            toast(message, kind);
        }
    } catch (e) { /* ignore */ }
});

/** Asks a yes/no question in a dialog. Resolves to true when confirmed. */
function confirmDialog(message, okLabel, danger) {
    return new Promise(resolve => {
        const dialog = document.createElement("dialog");
        dialog.className = "confirm";
        const text = document.createElement("p");
        text.textContent = message;
        const buttons = document.createElement("div");
        buttons.className = "confirm-buttons";
        const cancel = document.createElement("button");
        cancel.type = "button";
        cancel.className = "btn";
        cancel.textContent = "Cancel";
        const ok = document.createElement("button");
        ok.type = "button";
        ok.className = "btn " + (danger ? "btn-danger-solid" : "btn-primary");
        ok.textContent = okLabel || "OK";
        buttons.append(cancel, ok);
        dialog.append(text, buttons);
        document.body.appendChild(dialog);

        let answer = false;
        cancel.addEventListener("click", () => dialog.close());
        ok.addEventListener("click", () => {
            answer = true;
            dialog.close();
        });
        dialog.addEventListener("close", () => {
            dialog.remove();
            resolve(answer);
        });
        dialog.showModal();
    });
}

/**
 * For delete buttons: data-url is sent a DELETE, data-what names the thing in the question. With
 * data-remove (a selector), the closest matching element is removed instead of reloading the page,
 * and a "deleted" event is sent on document.
 */
async function confirmDelete(button) {
    const what = button.dataset.what || "this";
    if (!await confirmDialog("Delete " + what + "?", "Delete", true)) return;
    try {
        const response = await fetch(button.dataset.url, {method: "DELETE"});
        if (!response.ok) throw new Error();
        markUnpublished();
        if (button.dataset.remove) {
            button.closest(button.dataset.remove).remove();
            toast("Deleted " + what);
            document.dispatchEvent(new CustomEvent("deleted"));
        } else {
            flash("Deleted");
            location.reload();
        }
    } catch (e) {
        toast("Could not delete " + what, "error");
    }
}

/** The Publish button: data-url is the publish endpoint. */
async function confirmSave(button) {
    if (!await confirmDialog("Publish the site? All changes go live straight away.", "Publish")) return;
    const label = button.textContent;
    button.disabled = true;
    button.textContent = "Publishing…";
    try {
        const response = await fetch(button.dataset.url);
        const text = await response.text();
        if (!response.ok) throw new Error(text || "the server returned an error");
        toast("Site published");
        markPublished();
    } catch (e) {
        toast("The site was not published: " + e.message, "error");
    } finally {
        button.disabled = false;
        button.textContent = label;
    }
}
