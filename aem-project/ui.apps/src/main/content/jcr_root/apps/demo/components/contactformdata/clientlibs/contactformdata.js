function submitContactForm() {
  var nameEl = document.getElementById("name");
  var emailEl = document.getElementById("email");
  var mobileEl = document.getElementById("mobile");
  var editPathEl = document.getElementById("editPath");

  if (!nameEl || !emailEl || !mobileEl) {
    alert("Error while saving: form fields not found");
    return;
  }

  var name = nameEl.value.trim();
  var email = emailEl.value.trim();
  var mobile = mobileEl.value.trim();
  var editPath = editPathEl ? editPathEl.value.trim() : "";

  if (!name || !email || !mobile) {
    alert("Please fill Name, Email and Mobile");
    return;
  }

  var params = new URLSearchParams();
  params.append("name", name);
  params.append("email", email);
  params.append("mobile", mobile);
  if (editPath) {
    params.append("editPath", editPath);
  }

  fetch("/bin/contactformdata?" + params.toString(), {
    method: "GET",
    credentials: "same-origin"
  })
    .then(function (response) {
      return response.text().then(function (text) {
        return { ok: response.ok, text: (text || "").trim() };
      });
    })
    .then(function (result) {
      if (result.text === "success") {
        alert("Saved successfully");
        window.location.href = window.location.pathname + window.location.hash;
      } else {
        alert("Error while saving: " + result.text);
      }
    })
    .catch(function (error) {
      console.error("Error:", error);
      alert("Error while saving");
    });
}

function editContact(path) {
  if (!path) {
    return;
  }
  var url = new URL(window.location.href);
  url.searchParams.set("editPath", path);
  window.location.href = url.toString();
}

function deleteContact(path) {
  if (!path) {
    return;
  }
  if (!confirm("Delete this record?")) {
    return;
  }

  var params = new URLSearchParams();
  params.append("cmd", "delete");
  params.append("path", path);

  fetch("/bin/contactformdata?" + params.toString(), {
    method: "GET",
    credentials: "same-origin"
  })
    .then(function (response) {
      return response.text().then(function (text) {
        return { ok: response.ok, text: (text || "").trim() };
      });
    })
    .then(function (result) {
      if (result.text === "deleted") {
        alert("Deleted successfully");
        window.location.reload();
      } else {
        alert("Error while deleting: " + result.text);
      }
    })
    .catch(function (error) {
      console.error("Error:", error);
      alert("Error while deleting");
    });
}
