document.addEventListener("DOMContentLoaded", function() {
  var span = document.querySelector('.uri');
  if (span) {
    var url = window.location.href;
    var strippedUrl = url.replace(/^https?:\/\//, '');
    span.textContent = strippedUrl;
  }
});
