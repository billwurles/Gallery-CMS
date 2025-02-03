function confirmSave(button) {
    let url = button.getAttribute("data-url");
    if (confirm("Are you sure? All changes will be immediately live")) {
        fetch(url, {
                method: 'GET',
            })
            .then(response => {
                if (response.ok) {
                    alert("Site saved successfully!");
                } else {
                    throw new Error('Failed to save site');
                }
            })
            .then(data => {
                console.log(data); // Log the server's response
                // redirect or reload page after successful submission
                window.location.href = "/";
            })
            .catch(error => {
                alert("An error occurred: " + error.message);
            });

    }
}