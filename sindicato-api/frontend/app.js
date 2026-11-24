const app = document.querySelector("#app");
const nav = document.querySelector(".main-nav");
const menuToggle = document.querySelector(".menu-toggle");

function getSession() {
  try {
    const session = JSON.parse(sessionStorage.getItem("sindicato-session") || "null");
    if (session && !["ADMIN", "AFILIADO"].includes(session.role)) {
      sessionStorage.removeItem("sindicato-session");
      return null;
    }
    return session;
  } catch {
    sessionStorage.removeItem("sindicato-session");
    return null;
  }
}

function updateNavigation() {
  const session = getSession();
  const login = nav.querySelector(".nav-login");
  if (!login) return;
  if (session) {
    login.href = "#/cuenta";
    login.textContent = `Hola, ${session.nombre}`;
    if (!nav.querySelector("[data-logout]")) {
      const logout = document.createElement("button");
      logout.className = "nav-logout";
      logout.type = "button";
      logout.dataset.logout = "";
      logout.textContent = "Cerrar sesión";
      logout.addEventListener("click", () => {
        sessionStorage.removeItem("sindicato-session");
        updateNavigation();
        location.hash = "#/";
      });
      nav.append(logout);
    }
  } else {
    login.href = "#/iniciar-sesion";
    login.textContent = "Iniciar sesión";
    nav.querySelector("[data-logout]")?.remove();
  }
}

function escapeHtml(value = "") {
  return String(value).replace(/[&<>"']/g, (character) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  })[character]);
}

function formatDate(value) {
  if (!value) return "Fecha por confirmar";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Fecha por confirmar" :
    new Intl.DateTimeFormat("es-ES", { dateStyle: "long", timeStyle: "short" }).format(date);
}

function excerpt(text = "", length = 125) {
  const clean = String(text).replace(/\s+/g, " ").trim();
  return clean.length > length ? `${clean.slice(0, length).trim()}…` : clean;
}

function imageMarkup(url, alt, className = "card-image") {
  return url
    ? `<img class="${className}" src="${escapeHtml(url)}" alt="${escapeHtml(alt)}" loading="lazy">`
    : `<div class="card-image-placeholder" aria-hidden="true">⌂</div>`;
}

function hasRole(...roles) {
  return roles.includes(getSession()?.role);
}

function canPublish() {
  return hasRole("AFILIADO", "ADMIN");
}

async function api(url, options = {}) {
  const headers = new Headers(options.headers || {});
  const method = (options.method || "GET").toUpperCase();
  const session = getSession();
  if (session?.token && !["GET", "HEAD", "OPTIONS"].includes(method)) {
    headers.set("Authorization", `Bearer ${session.token}`);
  }
  const response = await fetch(url, { ...options, headers });
  if (!response.ok) {
    let message = `No se pudo completar la petición (${response.status}).`;
    try {
      const body = await response.json();
      message = body.error || body.message || message;
    } catch { /* La respuesta de error no siempre incluye JSON. */ }
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  return response.status === 204 ? null : response.json();
}

async function pageData(resource, page = 0) {
  try {
    return await api(`/api/${resource}?pagina=${page}`);
  } catch (error) {
    if (error.status === 404) return { content: [], totalPages: 0, number: page };
    throw error;
  }
}

function sectionHeader(label, title, action, href) {
  return `<div class="section-heading"><div><span class="eyebrow">${label}</span><h2>${title}</h2></div><a class="text-link" href="${href}">${action} <span aria-hidden="true">↗</span></a></div>`;
}

function newsCard(item) {
  return `<article class="content-card">
    ${imageMarkup(item.imagenUrl, item.titulo)}
    <div class="card-body"><div class="card-meta"><span class="tag">Noticias</span><span>${formatDate(item.fechaPublicacion)}</span></div>
      <h3>${escapeHtml(item.titulo)}</h3><p>${escapeHtml(excerpt(item.contenido))}</p>
      <a class="card-more" href="#/noticias/${item.id}">Leer noticia <span aria-hidden="true">→</span></a>
    </div>
  </article>`;
}

function activityCard(item) {
  return `<article class="content-card">
    ${imageMarkup(item.imagenUrl, item.titulo)}
    <div class="card-body"><div class="card-meta"><span class="tag">Actividades</span><span>${formatDate(item.fechaPublicacion)}</span></div>
      <h3>${escapeHtml(item.titulo)}</h3><p>${escapeHtml(excerpt(item.contenido))}</p>
      <a class="card-more" href="#/actividades/${item.id}">Ver actividad y apuntarme <span aria-hidden="true">→</span></a>
    </div>
  </article>`;
}

function renderCards(items, render, label, href) {
  if (!items.length) return `<div class="empty-state"><h3>Aún no hay ${label}</h3><p>Vuelve pronto para descubrir las novedades del sindicato.</p><a class="text-link" href="${href}">Conoce más <span aria-hidden="true">↗</span></a></div>`;
  return items.map(render).join("");
}

function loadingPage() {
  app.innerHTML = `<div class="section loading">Cargando contenidos…</div>`;
}

function errorPage(error) {
  app.innerHTML = `<section class="section error-state"><h3>No se pudo cargar el contenido</h3><p>${escapeHtml(error.message)}</p><button class="button button-dark" onclick="location.reload()">Intentar de nuevo</button></section>`;
}

async function renderHome() {
  loadingPage();
  try {
    const [news, activities] = await Promise.all([pageData("noticias"), pageData("actividades")]);
    app.innerHTML = `
      <section class="hero"><div class="hero-content">
        <span class="eyebrow">Juntas somos más fuertes</span>
        <h1>La vivienda es un derecho, <em>no un negocio.</em></h1>
        <p class="hero-copy">Somos un sindicato de inquilinas e inquilinos. Nos organizamos para defender nuestros hogares y construir barrios donde vivir no sea un privilegio.</p>
        <a class="button button-primary" href="#/afiliate">Únete al sindicato <span aria-hidden="true">↗</span></a>
      </div></section>
      <section class="section intro-grid">
        <div><span class="eyebrow">¿Quiénes somos?</span><h2>Unidas frente a los abusos del alquiler.</h2></div>
        <div class="intro-copy"><p>El sindicato es un espacio colectivo para inquilinas e inquilinos que quieren conocer sus derechos y hacerlos valer. Frente a problemas que parecen individuales, respondemos juntas.</p>
          <p>Nos acompañamos, compartimos herramientas y nos organizamos para que nadie tenga que defender su hogar en soledad.</p>
          <ul class="principles"><li><b>Apoyo mutuo</b><span>Nos cuidamos y nos acompañamos.</span></li><li><b>Organización</b><span>La fuerza colectiva cambia las cosas.</span></li><li><b>Vivienda digna</b><span>Un derecho para todas las personas.</span></li></ul>
        </div>
      </section>
      <section class="section home-feed">
        ${sectionHeader("Lo que pasa", "Noticias", "Todas las noticias", "#/noticias")}
        <div class="feed-grid">${renderCards((news.content || []).slice(0, 3), newsCard, "noticias", "#/noticias")}</div>
      </section>
      <section class="section">
        ${sectionHeader("Nos encontramos", "Próximas actividades", "Todas las actividades", "#/actividades")}
        <div class="feed-grid">${renderCards((activities.content || []).slice(0, 3), activityCard, "actividades", "#/actividades")}</div>
      </section>
      <section class="cta-band"><div><h2>Un alquiler justo se conquista en colectivo.</h2><p>Da el primer paso y forma parte del sindicato.</p></div><a class="button button-dark" href="#/afiliate">Afíliate ahora <span aria-hidden="true">↗</span></a></section>`;
  } catch (error) { errorPage(error); }
}

async function renderListing(type, page = 0) {
  const isNews = type === "noticias";
  const label = isNews ? "noticias" : "actividades";
  const title = isNews ? "Lo que pasa en el sindicato." : "Nos organizamos y nos encontramos.";
  const description = isNews
    ? "Actualidad, comunicados y avances de la organización colectiva por una vivienda digna."
    : "Encuentros, talleres y acciones para compartir herramientas y fortalecer nuestras redes.";
  const renderCard = isNews ? newsCard : activityCard;
  loadingPage();
  try {
    const result = await pageData(type, page);
    const entries = result.content || [];
    const pages = result.totalPages || 0;
    const pagination = pages > 1 ? `<div class="pagination"><button class="button button-outline" ${page <= 0 ? "disabled" : ""} data-page="${page - 1}">← Anterior</button><span>Página ${page + 1} de ${pages}</span><button class="button button-outline" ${page + 1 >= pages ? "disabled" : ""} data-page="${page + 1}">Siguiente →</button></div>` : "";
    const publishAction = canPublish()
      ? `<a class="button button-dark" href="#/crear-${isNews ? "noticia" : "actividad"}">+ Crear ${isNews ? "noticia" : "actividad"}</a>`
      : "";
    app.innerHTML = `<section class="page-intro"><span class="eyebrow">${isNews ? "Actualidad" : "Agenda abierta"}</span><h1>${title}</h1><p>${description}</p></section>
      <section class="section listing-section"><div class="listing-topline"><p>${entries.length ? `${result.totalElements || entries.length} publicaciones` : "El tablón del sindicato"}</p>${publishAction}</div>
      <div class="card-grid">${renderCards(entries, renderCard, label, `#/crear-${isNews ? "noticia" : "actividad"}`)}</div>${pagination}</section>`;
    app.querySelectorAll("[data-page]").forEach((button) => button.addEventListener("click", () => renderListing(type, Number(button.dataset.page))));
  } catch (error) { errorPage(error); }
}

async function renderDetail(type, id) {
  const isNews = type === "noticias";
  const label = isNews ? "Noticia" : "Actividad";
  loadingPage();
  try {
    const item = await api(`/api/${type}/${encodeURIComponent(id)}`);
    const facts = isNews
      ? `<span class="fact">${formatDate(item.fechaPublicacion)}</span>${item.actividadId ? `<span class="fact">Actividad relacionada #${item.actividadId}</span>` : ""}`
      : `<span class="fact">${formatDate(item.fechaPublicacion)}</span><span class="fact">${item.capacidad} plazas</span><span class="fact">Estado: ${escapeHtml(item.estado || "Disponible")}</span>`;
    app.innerHTML = `<article class="detail-wrap"><a class="back-link" href="#/${type}">← Volver a ${isNews ? "noticias" : "actividades"}</a>
      <div class="card-meta"><span class="tag">${label}</span><span>${formatDate(item.fechaPublicacion)}</span></div>
      <h1 class="detail-title">${escapeHtml(item.titulo)}</h1>${imageMarkup(item.imagenUrl, item.titulo, "detail-image")}
      <div class="detail-facts">${facts}</div><div class="detail-content">${escapeHtml(item.contenido)}</div>
      ${!isNews ? `<div class="participation-box"><div><strong>¿Te apuntas?</strong><p>Reserva tu plaza para participar en esta actividad.</p></div>
        ${getSession()
          ? `<button class="button button-dark" type="button" data-signup="${item.id}">Apuntarme <span aria-hidden="true">↗</span></button>`
          : `<a class="button button-dark" href="#/iniciar-sesion" data-login-redirect="#/actividades/${item.id}">Inicia sesión para apuntarte <span aria-hidden="true">↗</span></a>`}
        <span class="form-message" role="status"></span></div>` : ""}
      ${hasRole("ADMIN") ? `<div class="form-actions"><button class="button button-danger" type="button" data-delete-content="${escapeHtml(id)}">Eliminar ${isNews ? "noticia" : "actividad"}</button></div>` : ""}
      </article>`;
    app.querySelector("[data-signup]")?.addEventListener("click", (event) => registerForActivity(event.currentTarget.dataset.signup));
    app.querySelector("[data-delete-content]")?.addEventListener("click", async (event) => {
      if (!window.confirm(`¿Eliminar esta ${isNews ? "noticia" : "actividad"}? Esta acción no se puede deshacer.`)) return;
      try {
        await api(`/api/${type}/${encodeURIComponent(event.currentTarget.dataset.deleteContent)}`, { method: "DELETE" });
        location.hash = `#/${type}`;
      } catch (error) {
        errorPage(error);
      }
    });
    app.querySelector("[data-login-redirect]")?.addEventListener("click", (event) => {
      sessionStorage.setItem("sindicato-login-redirect", event.currentTarget.dataset.loginRedirect);
    });
  } catch (error) { errorPage(error); }
}

function imageField(required = true) {
  return `<div class="field full"><label for="imagen">Imagen ${required ? "*" : ""}</label><div class="upload-box">
    <input id="imagen" name="imagen" type="file" accept="image/png,image/jpeg,image/gif" ${required ? "required" : ""}>
    <small>JPG, PNG o GIF · máximo 5 MB</small><img class="image-preview" alt="Vista previa de la imagen seleccionada">
  </div></div>`;
}

function setupImagePreview() {
  const input = document.querySelector("#imagen");
  if (!input) return;
  input.addEventListener("change", () => {
    const file = input.files[0];
    const preview = document.querySelector(".image-preview");
    if (!file) { preview.removeAttribute("src"); preview.style.display = "none"; return; }
    if (file.size > 5 * 1024 * 1024) {
      input.setCustomValidity("La imagen no puede superar los 5 MB.");
      input.reportValidity();
      input.value = "";
      return;
    }
    input.setCustomValidity("");
    preview.src = URL.createObjectURL(file);
    preview.style.display = "block";
  });
}

function formShell({ eyebrow, heading, intro, asideTitle, asideText, form }) {
  app.innerHTML = `<section class="form-page"><aside class="form-aside"><span class="eyebrow">${eyebrow}</span><h2>${asideTitle}</h2><p>${asideText}</p></aside><div class="form-main"><div class="form-heading"><span class="eyebrow">${eyebrow}</span><h1>${heading}</h1><p>${intro}</p></div>${form}</div></section>`;
  setupImagePreview();
}

function renderJoin() {
  formShell({
    eyebrow: "Afíliate",
    heading: "Hagamos fuerza juntas.",
    intro: "Completa tus datos y sube una foto de perfil. Te enviaremos un correo para confirmar tu afiliación.",
    asideTitle: "Tu hogar también es nuestra lucha.",
    asideText: "Al afiliarte formas parte de una red de apoyo que defiende el derecho a una vivienda digna. Tus datos se utilizarán para gestionar tu alta y mantenerte al tanto de la organización.",
    form: `<form id="join-form"><div class="form-grid">
      <div class="field"><label for="nombre">Nombre *</label><input id="nombre" name="nombre" autocomplete="given-name" required></div>
      <div class="field"><label for="apellidos">Apellidos *</label><input id="apellidos" name="apellidos" autocomplete="family-name" required></div>
      <div class="field"><label for="dni">DNI / NIE *</label><input id="dni" name="dni" autocomplete="off" required></div>
      <div class="field"><label for="email">Correo electrónico *</label><input id="email" name="email" type="email" autocomplete="email" required></div>
      <div class="field full"><label for="password">Contraseña *</label><input id="password" name="password" type="password" minlength="10" maxlength="72" autocomplete="new-password" required><small>Al menos 10 caracteres.</small></div>
      <div class="field full"><label for="telefono">Teléfono</label><input id="telefono" name="telefono" type="tel" autocomplete="tel"></div>
      ${imageField()}
    </div><div class="form-actions"><button class="button button-dark" type="submit">Enviar solicitud <span aria-hidden="true">↗</span></button><span class="form-message" role="status"></span></div></form>`,
  });
  document.querySelector("#join-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const button = form.querySelector("button");
    const message = form.querySelector(".form-message");
    button.disabled = true;
    message.className = "form-message";
    message.textContent = "Enviando solicitud…";
    const data = Object.fromEntries(new FormData(form).entries());
    const image = data.imagen;
    delete data.imagen;
    const body = new FormData();
    body.append("datos", new Blob([JSON.stringify(data)], { type: "application/json" }));
    body.append("imagen", image);
    try {
      await api("/api/afiliados", { method: "POST", body });
      form.reset();
      document.querySelector(".image-preview").style.display = "none";
      message.textContent = "Solicitud enviada. Revisa tu correo para confirmar el alta.";
    } catch (error) {
      message.classList.add("error");
      message.textContent = error.message;
    } finally { button.disabled = false; }
  });
}

function renderLogin() {
  formShell({
    eyebrow: "Área de afiliadas",
    heading: "Qué bueno verte.",
    intro: "Inicia sesión con el correo y la contraseña que elegiste al afiliarte.",
    asideTitle: "Tu comunidad te espera.",
    asideText: "Accede para apuntarte a actividades y seguir participando en la vida del sindicato.",
    form: `<form id="login-form"><div class="form-grid">
      <div class="field full"><label for="email">Correo electrónico *</label><input id="email" name="email" type="email" autocomplete="email" required></div>
      <div class="field full"><label for="password">Contraseña *</label><input id="password" name="password" type="password" autocomplete="current-password" required></div>
    </div><div class="form-actions"><button class="button button-dark" type="submit">Iniciar sesión <span aria-hidden="true">↗</span></button><span class="form-message" role="status"></span></div>
    <p class="form-note">¿Todavía no tienes cuenta? <a class="text-link" href="#/afiliate">Afíliate al sindicato</a></p></form>`,
  });
  document.querySelector("#login-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const button = form.querySelector("button");
    const message = form.querySelector(".form-message");
    button.disabled = true;
    message.className = "form-message";
    message.textContent = "Comprobando tus datos…";
    try {
      const session = await api("/api/afiliados/sesion", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(Object.fromEntries(new FormData(form).entries())),
      });
      sessionStorage.setItem("sindicato-session", JSON.stringify(session));
      updateNavigation();
      const redirect = sessionStorage.getItem("sindicato-login-redirect");
      sessionStorage.removeItem("sindicato-login-redirect");
      location.hash = redirect || "#/cuenta";
    } catch (error) {
      message.classList.add("error");
      message.textContent = error.message;
      button.disabled = false;
    }
  });
}

function renderAccount() {
  const session = getSession();
  if (!session) {
    location.hash = "#/iniciar-sesion";
    return;
  }
  app.innerHTML = `<section class="page-intro"><span class="eyebrow">Área de afiliadas</span><h1>Hola, ${escapeHtml(session.nombre)}.</h1><p>Ya puedes apuntarte a las actividades del sindicato.</p></section>
    <section class="section"><a class="button button-dark" href="#/actividades">Ver actividades <span aria-hidden="true">↗</span></a></section>`;
}

async function registerForActivity(activityId) {
  const button = app.querySelector("[data-signup]");
  const message = app.querySelector(".participation-box .form-message");
  button.disabled = true;
  message.className = "form-message";
  message.textContent = "Reservando tu plaza…";
  try {
    const session = getSession();
    await api(`/api/actividades/${encodeURIComponent(activityId)}/participaciones`, { method: "POST" });
    message.textContent = "¡Ya tienes tu plaza reservada!";
    button.textContent = "Inscripción confirmada";
  } catch (error) {
    message.classList.add("error");
    message.textContent = error.message;
    if (error.status === 401 || error.status === 403) {
      sessionStorage.removeItem("sindicato-session");
      sessionStorage.setItem("sindicato-login-redirect", `#/actividades/${activityId}`);
      updateNavigation();
      const login = document.createElement("a");
      login.className = "text-link";
      login.href = "#/iniciar-sesion";
      login.textContent = "Inicia sesión de nuevo";
      message.after(login);
    } else {
      button.disabled = false;
    }
  }
}

function localDateTimeValue() {
  const date = new Date(Date.now() - new Date().getTimezoneOffset() * 60000);
  return date.toISOString().slice(0, 16);
}

function renderCreate(type) {
  if (!canPublish()) {
    location.hash = "#/iniciar-sesion";
    return;
  }
  const isNews = type === "noticias";
  const noun = isNews ? "noticia" : "actividad";
  const title = isNews ? "Comparte lo que está pasando." : "Crea un espacio para encontrarnos.";
  const extra = isNews ? "" : `<div class="field"><label for="capacidad">Número de plazas *</label><input id="capacidad" name="capacidad" type="number" min="1" value="20" required></div>`;
  formShell({
    eyebrow: `Publicar ${noun}`,
    heading: title,
    intro: `Completa los detalles y añade una imagen para publicar tu ${noun} en la web del sindicato.`,
    asideTitle: isNews ? "Una voz colectiva, muchas historias." : "La organización también se construye en persona.",
    asideText: isNews ? "Comparte noticias, comunicados y avances de la lucha por una vivienda digna." : "Organiza un encuentro, taller o acción para que la comunidad pueda participar.",
    form: `<form id="content-form"><div class="form-grid">
      <div class="field full"><label for="titulo">Título *</label><input id="titulo" name="titulo" maxlength="180" required></div>
      <div class="field full"><label for="contenido">Descripción *</label><textarea id="contenido" name="contenido" required></textarea></div>
      <div class="field"><label for="fechaPublicacion">Fecha y hora *</label><input id="fechaPublicacion" name="fechaPublicacion" type="datetime-local" value="${localDateTimeValue()}" required></div>
      ${extra}${imageField()}
    </div><div class="form-actions"><button class="button button-dark" type="submit">Publicar ${noun} <span aria-hidden="true">↗</span></button><span class="form-message" role="status"></span></div></form>`,
  });
  document.querySelector("#content-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const button = form.querySelector("button");
    const message = form.querySelector(".form-message");
    button.disabled = true;
    message.className = "form-message";
    message.textContent = "Publicando…";
    const data = Object.fromEntries(new FormData(form).entries());
    const image = data.imagen;
    delete data.imagen;
    if (isNews) data.actividadId = null;
    else data.capacidad = Number(data.capacidad);
    data.fechaPublicacion = `${data.fechaPublicacion}:00`;
    const body = new FormData();
    body.append("datos", new Blob([JSON.stringify(data)], { type: "application/json" }));
    body.append("imagen", image);
    try {
      const created = await api(`/api/${type}`, { method: "POST", body });
      location.hash = `#/${type}/${created.id}`;
    } catch (error) {
      message.classList.add("error");
      message.textContent = error.message;
      button.disabled = false;
    }
  });
}

function documentCard(item) {
  const id = encodeURIComponent(item.id);
  const typeLabel = escapeHtml(String(item.tipo || "OTRO").replaceAll("_", " "));
  return `<article class="document-card">
    <div class="card-meta"><span class="tag">${typeLabel}</span><span>${formatDate(item.fechaCreacion)}</span></div>
    <h3>${escapeHtml(item.nombreArchivo)}</h3>
    ${item.descripcion ? `<p>${escapeHtml(item.descripcion)}</p>` : ""}
    <div class="document-actions">
      <button class="button button-outline" type="button" data-preview-document="${id}">Previsualizar</button>
      <a class="button button-dark" href="/api/documentos/${id}/archivo">Descargar</a>
      ${hasRole("ADMIN") ? `<button class="button button-danger" type="button" data-delete-document="${id}">Eliminar</button>` : ""}
    </div>
    <div class="document-preview" data-document-preview="${id}" hidden></div>
  </article>`;
}

async function renderDocuments() {
  loadingPage();
  try {
    const documents = await api("/api/documentos");
    const uploadForm = canPublish() ? `<section class="document-upload">
      <h2>Publicar un documento</h2>
      <form id="document-form"><div class="form-grid">
        <div class="field"><label for="tipo">Tipo de documento *</label><select id="tipo" name="tipo" required>
          <option value="OTRO">Otro</option><option value="CONTRATO_ALQUILER">Contrato de alquiler</option>
          <option value="RECIBO_ALQUILER">Recibo de alquiler</option><option value="FACTURA">Factura</option>
          <option value="DESPERFECTOS_VIVIENDA">Desperfectos en la vivienda</option><option value="DENUNCIA">Denuncia</option>
        </select></div>
        <div class="field"><label for="archivo">Archivo PDF, JPG o PNG *</label><input id="archivo" name="archivo" type="file" accept="application/pdf,image/jpeg,image/png" required><small>Máximo 20 MB. El documento será público al publicarlo.</small></div>
        <div class="field full"><label for="descripcion">Descripción</label><textarea id="descripcion" name="descripcion" maxlength="500"></textarea><small>No incluyas datos personales o información confidencial: el documento se podrá ver y descargar públicamente.</small></div>
      </div><div class="form-actions"><button class="button button-dark" type="submit">Publicar documento</button><span class="form-message" role="status"></span></div></form>
    </section>` : "";
    app.innerHTML = `<section class="page-intro"><span class="eyebrow">Biblioteca pública</span><h1>Documentos del sindicato.</h1><p>Consulta, previsualiza y descarga documentos publicados por las personas afiliadas y el equipo administrador.</p></section>
      <section class="section listing-section">${uploadForm}
        <div class="document-grid">${documents.length
          ? documents.map(documentCard).join("")
          : `<div class="empty-state"><h3>Aún no hay documentos</h3><p>Cuando se publiquen, aparecerán aquí para toda la comunidad.</p></div>`}
        </div></section>`;

    app.querySelector("#document-form")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      const form = event.currentTarget;
      const file = form.elements.archivo.files[0];
      const message = form.querySelector(".form-message");
      const button = form.querySelector("button");
      if (file.size > 20 * 1024 * 1024) {
        message.className = "form-message error";
        message.textContent = "El documento no puede superar los 20 MB.";
        return;
      }
      button.disabled = true;
      message.className = "form-message";
      message.textContent = "Publicando…";
      const data = Object.fromEntries(new FormData(form).entries());
      delete data.archivo;
      const body = new FormData();
      body.append("datos", new Blob([JSON.stringify(data)], { type: "application/json" }));
      body.append("archivo", file);
      try {
        await api("/api/documentos", { method: "POST", body });
        renderDocuments();
      } catch (error) {
        message.classList.add("error");
        message.textContent = error.message;
        button.disabled = false;
      }
    });

    app.querySelectorAll("[data-preview-document]").forEach((button) => button.addEventListener("click", () => {
      const id = button.dataset.previewDocument;
      const item = documents.find((document) => String(document.id) === id);
      const preview = [...app.querySelectorAll(".document-preview")]
        .find((element) => element.dataset.documentPreview === id);
      if (!item || !preview) return;
      if (!preview.hidden) {
        preview.hidden = true;
        preview.innerHTML = "";
        button.textContent = "Previsualizar";
        return;
      }
      const url = `/api/documentos/${encodeURIComponent(id)}/previsualizacion`;
      preview.innerHTML = item.tipoContenido === "application/pdf"
        ? `<iframe title="Previsualización de ${escapeHtml(item.nombreArchivo)}" src="${url}"></iframe>`
        : `<img src="${url}" alt="Previsualización de ${escapeHtml(item.nombreArchivo)}">`;
      preview.hidden = false;
      button.textContent = "Cerrar vista previa";
    }));
    app.querySelectorAll("[data-delete-document]").forEach((button) => button.addEventListener("click", async (event) => {
      const id = event.currentTarget.dataset.deleteDocument;
      if (!window.confirm("¿Eliminar este documento? Esta acción no se puede deshacer.")) return;
      try {
        await api(`/api/documentos/${encodeURIComponent(id)}`, { method: "DELETE" });
        renderDocuments();
      } catch (error) {
        errorPage(error);
      }
    }));
  } catch (error) { errorPage(error); }
}

async function renderRoute() {
  const route = location.hash.replace(/^#\/?/, "").split("/").filter(Boolean);
  nav.classList.remove("open");
  menuToggle.setAttribute("aria-expanded", "false");
  updateNavigation();
  if (!route.length) return renderHome();
  if (route[0] === "noticias" && route[1]) return renderDetail("noticias", route[1]);
  if (route[0] === "actividades" && route[1]) return renderDetail("actividades", route[1]);
  if (route[0] === "noticias") return renderListing("noticias");
  if (route[0] === "actividades") return renderListing("actividades");
  if (route[0] === "documentos") return renderDocuments();
  if (route[0] === "afiliate") return renderJoin();
  if (route[0] === "iniciar-sesion") return renderLogin();
  if (route[0] === "cuenta") return renderAccount();
  if (route[0] === "crear-noticia") return renderCreate("noticias");
  if (route[0] === "crear-actividad") return renderCreate("actividades");
  location.hash = "#/";
}

menuToggle.addEventListener("click", () => {
  const open = nav.classList.toggle("open");
  menuToggle.setAttribute("aria-expanded", String(open));
});
window.addEventListener("hashchange", renderRoute);
renderRoute();
