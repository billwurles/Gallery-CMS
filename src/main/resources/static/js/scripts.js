function confirmSave(button) {
    let url = button.getAttribute("data-url");
    if (confirm("Are you sure? All changes will be immediately live")) {
        window.location.href = url;
    }
}