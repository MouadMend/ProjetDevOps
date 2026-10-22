"use strict";
const api = "/api/links";  // Apache agit comme reverse proxy vers Tomcat.
const form = document.querySelector("#link-form");
const titleInput = document.querySelector("#title");
const urlInput = document.querySelector("#url");
const submitButton = document.querySelector("#submit");
const cancelButton = document.querySelector("#cancel");
const message = document.querySelector("#message");
const linksContainer = document.querySelector("#links");
let editId = null;

function status(text, error = false) {
  message.textContent = text;
  message.style.color = error ? "#b02b35" : "#26754c";
}

async function callApi(path = "", options = {}) {
  let response;
  try {
    response = await fetch(`${api}${path}`, options);
  } catch (error) {
    throw new Error("API inaccessible. Vérifie Tomcat et le reverse proxy Apache.");
  }
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new Error(data.error || `Erreur HTTP ${response.status}`);
  }
  return response.status === 204 ? null : response.json();
}

function resetForm() {
  form.reset();
  editId = null;
  document.querySelector("#form-title").textContent = "Ajouter un lien";
  submitButton.textContent = "Ajouter";
  cancelButton.hidden = true;
}

async function loadLinks() {
  const items = await callApi();
  document.querySelector("#count").textContent = `(${items.length})`;
  linksContainer.replaceChildren();
  if (!items.length) {
    const p = document.createElement("p");
    p.className = "muted";
    p.textContent = "Aucun lien pour le moment. Ajoute ton premier lien !";
    linksContainer.append(p);
    return;
  }
  for (const item of items) {
    const row = document.createElement("div");
    row.className = "link-item";
    const info = document.createElement("div");
    const anchor = document.createElement("a");
    anchor.href = item.url;
    anchor.target = "_blank";
    anchor.rel = "noopener noreferrer";
    anchor.textContent = item.title;
    const location = document.createElement("p");
    location.className = "url";
    location.textContent = item.url;
    info.append(anchor, location);
    const actions = document.createElement("div");
    actions.className = "link-actions";
    const edit = document.createElement("button");
    edit.type = "button";
    edit.textContent = "Modifier";
    edit.addEventListener("click", () => {
      editId = item.id;
      titleInput.value = item.title;
      urlInput.value = item.url;
      submitButton.textContent = "Enregistrer";
      document.querySelector("#form-title").textContent = "Modifier un lien";
      cancelButton.hidden = false;
      titleInput.focus();
    });
    const remove = document.createElement("button");
    remove.type = "button";
    remove.className = "delete";
    remove.textContent = "Supprimer";
    remove.addEventListener("click", async () => {
      if (!confirm(`Supprimer « ${item.title} » ?`)) return;
      try {
        await callApi(`/${item.id}`, { method: "DELETE" });
        status("Lien supprimé.");
        resetForm();
        await loadLinks();
      } catch (error) { status(error.message, true); }
    });
    actions.append(edit, remove);
    row.append(info, actions);
    linksContainer.append(row);
  }
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  submitButton.disabled = true;
  try {
    const payload = { title: titleInput.value.trim(), url: urlInput.value.trim() };
    await callApi(editId === null ? "" : `/${editId}`, {
      method: editId === null ? "POST" : "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    status(editId === null ? "Lien ajouté." : "Lien modifié.");
    resetForm();
    await loadLinks();
  } catch (error) { status(error.message, true); }
  finally { submitButton.disabled = false; }
});
cancelButton.addEventListener("click", resetForm);
loadLinks().catch((error) => status(error.message, true));
