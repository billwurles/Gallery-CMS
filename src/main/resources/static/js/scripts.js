function confirmSave(button) {
    let url = button.getAttribute("data-url");
    if (confirm("Are you sure? All changes will be immediately live")) {
        fetch(url, {method: 'GET'})
            .then(response => response.text().then(text => {
                if (!response.ok) throw new Error(text || 'the server returned an error');
                alert("Site published");
            }))
            .catch(error => {
                alert("The site was not published: " + error.message);
            });
    }
}
