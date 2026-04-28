const BASE_URL = 'http://localhost:8080/api';

function getToken() {
    return localStorage.getItem('token');
}

function getRefreshToken() {
    return localStorage.getItem('refreshToken');
}

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
    if (!refreshToken) {
        clearTokens();
        window.dispatchEvent(new Event('auth:logout'));
        return null;
    }
    try {
        const res = await fetch(`${BASE_URL}/auth/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
        });
        if (!res.ok) throw new Error('Refresh failed');
        const data = await res.json();
        saveTokens(data.token, data.refreshToken);
        return data.token;
    } catch {
        clearTokens();
        window.dispatchEvent(new Event('auth:logout'));
        return null;
    }
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
            return request(path, options, false);
        }
        return res;
    }

    return res;
}

async function json(path, options = {}) {
    const res = await request(path, options);
    if (!res.ok) {
        let err;
        try {
            err = await res.json();
        } catch {
            err = { message: `Erreur ${res.status}` };
        }
        throw { status: res.status, message: err.message || 'Erreur inconnue' };
    }
    // 204 No Content
    if (res.status === 204) return null;
    return res.json();
}

// ── Auth ──────────────────────────────────────────────────────────────────────

export const auth = {
    inscrire: (data) =>
        json('/auth/inscription', {
            method: 'POST',
            body: JSON.stringify(data),
        }),

    connecter: (data) =>
        json('/auth/connexion', {
            method: 'POST',
            body: JSON.stringify(data),
        }),

    saveSession: (data) => {
        saveTokens(data.token, data.refreshToken);
        localStorage.setItem(
            'user',
            JSON.stringify({ id: data.id, pseudo: data.pseudo })
        );
    },

    logout: () => {
        clearTokens();
        window.dispatchEvent(new Event('auth:logout'));
    },

    getUser: () => {
        const raw = localStorage.getItem('user');
        return raw ? JSON.parse(raw) : null;
    },

    isAuthenticated: () => !!getToken(),
};

// ── Jeux ─────────────────────────────────────────────────────────────────────

export const jeux = {
    lister: (params = {}) => {
        const qs = new URLSearchParams(
            Object.fromEntries(
                Object.entries(params).filter(([, v]) => v !== undefined && v !== '')
            )
        ).toString();
        return json(`/jeux${qs ? `?${qs}` : ''}`);
    },

    rechercher: (titre) =>
        json(`/jeux/recherche?titre=${encodeURIComponent(titre)}`, {
            method: 'POST',
        }),

    importer: (igdbId) =>
        json(`/jeux/importer/${igdbId}`, { method: 'POST' }),
};

// ── Bibliothèque ──────────────────────────────────────────────────────────────

export const bibliotheque = {
    maBibliotheque: () => json('/bibliotheque'),

    parStatut: (statut) => json(`/bibliotheque/statut/${statut}`),

    ajouter: (jeuId, statut) =>
        json(`/bibliotheque/jeu/${jeuId}?statut=${statut}`, { method: 'POST' }),

    changerStatut: (jeuId, statut) =>
        json(`/bibliotheque/jeu/${jeuId}/statut?statut=${statut}`, {
            method: 'PUT',
        }),

    supprimer: (jeuId) =>
        json(`/bibliotheque/jeu/${jeuId}`, { method: 'DELETE' }),
};

// ── Avis ─────────────────────────────────────────────────────────────────────

export const avis = {
    duJeu: (jeuId, page = 0, size = 20) =>
        json(`/avis/jeu/${jeuId}?page=${page}&size=${size}`),

    mesAvis: () => json('/avis/mes-avis'),

    ajouter: (jeuId, texte) =>
        json(`/avis/jeu/${jeuId}`, {
            method: 'POST',
            body: JSON.stringify({ texte }),
        }),

    liker: (avisId, like) =>
        json(`/avis/${avisId}/like?like=${like}`, { method: 'POST' }),

    supprimer: (avisId) =>
        json(`/avis/${avisId}`, { method: 'DELETE' }),
};

// ── Notes ─────────────────────────────────────────────────────────────────────

export const notes = {
    duJeu: (jeuId, page = 0, size = 20) =>
        json(`/notes/jeu/${jeuId}?page=${page}&size=${size}`),

    noter: (jeuId, valeur) =>
        json(`/notes/jeu/${jeuId}?valeur=${valeur}`, { method: 'POST' }),

    supprimer: (jeuId) =>
        json(`/notes/jeu/${jeuId}`, { method: 'DELETE' }),
};

// ── Utilisateurs ──────────────────────────────────────────────────────────────

export const utilisateurs = {
    profil: (id) => json(`/users/${id}/profil`),
    avis: (id) => json(`/users/${id}/avis`),
    bibliotheque: (id) => json(`/users/${id}/bibliotheque`),
};