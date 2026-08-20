const tokenStorageKey = "gamebased.keycloak.tokens";
const pkceStorageKey = "gamebased.keycloak.pkce";

let authConfig = null;

const authMode = document.querySelector("#auth-mode");
const userSummary = document.querySelector("#user-summary");
const loginButton = document.querySelector("#login-button");
const profileButton = document.querySelector("#profile-button");
const logoutButton = document.querySelector("#logout-button");
const continueLink = document.querySelector("#continue-link");
const output = document.querySelector("#login-output");

loginButton.addEventListener("click", login);
profileButton.addEventListener("click", loadProfile);
logoutButton.addEventListener("click", logout);

boot();

async function boot() {
  try {
    authConfig = await fetchJson("/api/auth/config");
    authMode.textContent = "Keycloak aktiv";

    if (new URLSearchParams(window.location.search).has("code")) {
      await finishLogin();
    } else if (readTokens()) {
      await loadProfile();
    }
  } catch (error) {
    showError(error);
  }
}

async function login() {
  if (!authConfig) {
    return;
  }

  const state = randomBase64Url(32);
  const codeVerifier = randomBase64Url(64);
  const codeChallenge = await sha256Base64Url(codeVerifier);
  sessionStorage.setItem(pkceStorageKey, JSON.stringify({ state, codeVerifier }));

  const params = new URLSearchParams({
    client_id: authConfig.keycloakClientId,
    redirect_uri: redirectUri(),
    response_type: "code",
    scope: "openid profile email",
    state,
    code_challenge: codeChallenge,
    code_challenge_method: "S256"
  });

  window.location.href = `${realmUrl()}/protocol/openid-connect/auth?${params}`;
}

async function finishLogin() {
  const params = new URLSearchParams(window.location.search);
  const code = params.get("code");
  const state = params.get("state");
  const pkce = JSON.parse(sessionStorage.getItem(pkceStorageKey) || "{}");

  if (!code || !state || state !== pkce.state || !pkce.codeVerifier) {
    throw new Error("Keycloak-Rückleitung konnte nicht verifiziert werden.");
  }

  const tokenResponse = await fetch(`${realmUrl()}/protocol/openid-connect/token`, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body: new URLSearchParams({
      grant_type: "authorization_code",
      client_id: authConfig.keycloakClientId,
      redirect_uri: redirectUri(),
      code,
      code_verifier: pkce.codeVerifier
    })
  });

  if (!tokenResponse.ok) {
    throw new Error(`Token-Austausch fehlgeschlagen: HTTP ${tokenResponse.status}`);
  }

  const tokens = await tokenResponse.json();
  tokens.expires_at = Date.now() + tokens.expires_in * 1000;
  sessionStorage.setItem(tokenStorageKey, JSON.stringify(tokens));
  sessionStorage.removeItem(pkceStorageKey);
  window.history.replaceState({}, document.title, window.location.pathname);
  await loadProfile();
}

async function loadProfile() {
  const tokens = readTokens();
  if (!tokens?.access_token) {
    writeOutput({ status: "Noch kein Keycloak Token vorhanden." });
    return;
  }

  if (tokens.expires_at && tokens.expires_at < Date.now()) {
    sessionStorage.removeItem(tokenStorageKey);
    writeOutput({ status: "Token ist abgelaufen. Bitte neu anmelden." });
    return;
  }

  const response = await fetch("/api/auth/me", {
    headers: {
      Authorization: `Bearer ${tokens.access_token}`
    }
  });

  if (!response.ok) {
    throw new Error(`Backend-Profil konnte nicht geladen werden: HTTP ${response.status}`);
  }

  renderProfile(await response.json());
}

function logout() {
  const tokens = readTokens();
  sessionStorage.removeItem(tokenStorageKey);
  userSummary.textContent = "Nicht angemeldet";
  continueLink.classList.add("hidden");

  if (authConfig?.provider === "keycloak" && tokens?.id_token) {
    const params = new URLSearchParams({
      post_logout_redirect_uri: redirectUri(),
      id_token_hint: tokens.id_token
    });
    window.location.href = `${realmUrl()}/protocol/openid-connect/logout?${params}`;
    return;
  }

  writeOutput({ status: "Lokale Sitzung gelöscht." });
}

function renderProfile(profile) {
  userSummary.textContent = `${profile.username} (${profile.role})`;
  continueLink.classList.remove("hidden");
  writeOutput({
    status: "Login erfolgreich. Du kannst jetzt zur Plattform weitergehen.",
    backendUser: profile,
    tokenClaims: safeTokenClaims()
  });
}

function safeTokenClaims() {
  const tokens = readTokens();
  if (!tokens?.access_token) {
    return null;
  }

  const claims = parseJwt(tokens.access_token);
  return {
    sub: claims.sub,
    preferred_username: claims.preferred_username,
    name: claims.name,
    distinguishedName: claims.distinguishedName,
    custom_roles: claims.custom_roles,
    groups: claims.groups
  };
}

function realmUrl() {
  return `${authConfig.keycloakUrl}/realms/${authConfig.keycloakRealm}`;
}

function redirectUri() {
  return `${window.location.origin}${window.location.pathname}`;
}

function readTokens() {
  return JSON.parse(sessionStorage.getItem(tokenStorageKey) || "null");
}

async function fetchJson(url) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`${url} lieferte HTTP ${response.status}`);
  }
  return response.json();
}

function parseJwt(token) {
  const payload = token.split(".")[1];
  return JSON.parse(new TextDecoder().decode(base64UrlToBytes(payload)));
}

async function sha256Base64Url(value) {
  const data = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return bytesToBase64Url(new Uint8Array(digest));
}

function randomBase64Url(length) {
  const bytes = new Uint8Array(length);
  crypto.getRandomValues(bytes);
  return bytesToBase64Url(bytes);
}

function bytesToBase64Url(bytes) {
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function base64UrlToBytes(value) {
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(value.length / 4) * 4, "=");
  const binary = atob(base64);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

function writeOutput(value) {
  output.textContent = JSON.stringify(value, null, 2);
}

function showError(error) {
  output.textContent = error instanceof Error ? error.message : String(error);
}
