const BASE_URL = 'http://localhost:8080/api';

function getToken() { return localStorage.getItem('token'); }
function getRefreshToken() { return localStorage.getItem('refreshToken'); }

function saveTokens(token, refreshToken) {
    localStorage.setItem('token', token);
    localStorage.setItem('refreshToken', refreshToken);
}
function clearTokens() {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
}

async function tryRefresh() {
    const refreshToken = getRefreshToken();
    if (!refreshToken) { clearTokens(); window.dispatchEvent(new Event('auth:logout')); return null; }
    try {
        const res = await fetch(`${BASE_URL}/auth/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
        });
        if (!res.ok) { clearTokens(); window.dispatchEvent(new Event('auth:logout')); return null; }
        const data = await res.json();
        saveTokens(data.token, data.refreshToken);
        return data.token;
    } catch { clearTokens(); window.dispatchEvent(new Event('auth:logout')); return null; }
}

async function request(path, options = {}, retry = true) {
    const token = getToken();
    const headers = {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
    };
    const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });
    if (res.status === 401 && retry) {
        const newToken = await tryRefresh();
        if (newToken) {
            const retryHeaders = { 'Content-Type': 'application/json', Authorization: `Bearer ${newToken}`, ...options.headers };
            return fetch(`${BASE_URL}${path}`, { ...options, headers: retryHeaders });
        }
        return res;
    }
    return res;
}

async function json(path, options = {}) {
    const res = await request(path, options);
    if (!res.ok) {
        let err; try { err = await res.json(); } catch { err = { message: `Erreur ${res.status}` }; }
        throw { status: res.status, message: err.message || 'Erreur inconnue' };
    }
    if (res.status === 204) return null;
    return res.json();
}

export const auth = {
    inscrire: (data) => json('/auth/inscription', { method: 'POST', body: JSON.stringify(data) }),
    connecter: (data) => json('/auth/connexion', { method: 'POST', body: JSON.stringify(data) }),
    saveSession: (data) => {
        saveTokens(data.token, data.refreshToken);
        localStorage.setItem('user', JSON.stringify({ id: data.id, pseudo: data.pseudo }));
    },
    logout: () => { clearTokens(); window.dispatchEvent(new Event('auth:logout')); },
    getUser: () => { const r = localStorage.getItem('user'); return r ? JSON.parse(r) : null; },
    isAuthenticated: () => !!getToken(),
};

export const jeux = {
    lister: (params = {}) => {
        const qs = new URLSearchParams(
            Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== '' && v !== null))
        ).toString();
        return json(`/jeux${qs ? `?${qs}` : ''}`);
    },
    rechercher: (titre) => json(`/jeux/recherche?titre=${encodeURIComponent(titre)}`, { method: 'POST' }),
    getById: (id) => json(`/jeux/${id}`),
    importer: (igdbId) => json(`/jeux/importer/${igdbId}`, { method: 'POST' }),
    creerManuellement: (data) => json('/jeux/manuel', { method: 'POST', body: JSON.stringify(data) }),
    importerAuto: () => json('/jeux/import-auto', { method: 'POST' }),
    getProgression: () => json('/jeux/import-progression'),
    getGenres: () => json('/jeux/genres'),
    getPlateformes: () => json('/jeux/plateformes'),
};

export const bibliotheque = {
    maBibliotheque: () => json('/bibliotheque'),
    parStatut: (statut) => json(`/bibliotheque/statut/${statut}`),
    ajouter: (jeuId, statut) => json(`/bibliotheque/jeu/${jeuId}?statut=${statut}`, { method: 'POST' }),
    changerStatut: (jeuId, statut) => json(`/bibliotheque/jeu/${jeuId}/statut?statut=${statut}`, { method: 'PUT' }),
    supprimer: (jeuId) => json(`/bibliotheque/jeu/${jeuId}`, { method: 'DELETE' }),
};

export const avis = {
    duJeu: (jeuId, page = 0, size = 20) => json(`/avis/jeu/${jeuId}?page=${page}&size=${size}`),
    mesAvis: () => json('/avis/mes-avis'),
    ajouter: (jeuId, texte) => json(`/avis/jeu/${jeuId}`, { method: 'POST', body: JSON.stringify({ texte }) }),
    liker: (avisId, like) => json(`/avis/${avisId}/like?like=${like}`, { method: 'POST' }),
    supprimer: (avisId) => json(`/avis/${avisId}`, { method: 'DELETE' }),
};

export const notes = {
    duJeu: (jeuId, page = 0, size = 20) => json(`/notes/jeu/${jeuId}?page=${page}&size=${size}`),
    noter: (jeuId, valeur) => json(`/notes/jeu/${jeuId}?valeur=${valeur}`, { method: 'POST' }),
    supprimer: (jeuId) => json(`/notes/jeu/${jeuId}`, { method: 'DELETE' }),
};

export const utilisateurs = {
    profil: (id) => json(`/users/${id}/profil`),
    avis: (id) => json(`/users/${id}/avis`),
    bibliotheque: (id) => json(`/users/${id}/bibliotheque`),
};

export const avatar = {
    get: (userId) => json(`/utilisateurs/${userId}/avatar`),
    upload: async (file) => {
        const token = getToken();
        const formData = new FormData();
        formData.append('fichier', file);
        let res = await fetch(`${BASE_URL}/utilisateurs/moi/avatar`, {
            method: 'POST',
            headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
            body: formData,
        });
        if (res.status === 401) {
            const newToken = await tryRefresh();
            if (newToken) res = await fetch(`${BASE_URL}/utilisateurs/moi/avatar`, {
                method: 'POST', headers: { Authorization: `Bearer ${newToken}` }, body: formData,
            });
        }
        if (!res.ok) {
            let err; try { err = await res.json(); } catch { err = { message: `Erreur ${res.status}` }; }
            throw { status: res.status, message: err.message || 'Erreur upload' };
        }
        return res.json();
    },
    supprimer: () => json('/utilisateurs/moi/avatar', { method: 'DELETE' }),
};

export const reports = {
    soumettre: (data) => json('/reports', { method: 'POST', body: JSON.stringify(data) }),
    listerTous: () => json('/reports'),
    parStatut: (statut) => json(`/reports/statut/${statut}`),
    supprimer: (id) => json(`/reports/${id}`, { method: 'DELETE' }),
    traiter: (id, data) => json(`/reports/${id}/traiter`, { method: 'PUT', body: JSON.stringify(data) }),
};