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

document.addEventListener("DOMContentLoaded", function () {
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
    } catch (e) {
        toast("The site was not published: " + e.message, "error");
    } finally {
        button.disabled = false;
        button.textContent = label;
    }
}
