import { createContext, useContext, useEffect, useState } from 'react';
import { auth } from '../api.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [isAuth, setIsAuth] = useState(false);
    const [ready, setReady] = useState(false); // attend la vérification initiale

    useEffect(() => {
        // Au montage : si un token existe, on tente de le valider via refresh
        // silencieux pour s'assurer que la session est toujours active
        const init = async () => {
            const storedUser = auth.getUser();
            const token = localStorage.getItem('token');
            const refreshToken = localStorage.getItem('refreshToken');

            if (!storedUser || !refreshToken) {
                // Pas de session sauvegardée
                setReady(true);
                return;
            }

            if (token) {
                // Token présent → on fait confiance pour l'instant,
                // le refresh se fera automatiquement si une requête reçoit 401
                setUser(storedUser);
                setIsAuth(true);
                setReady(true);
                return;
            }

            // Pas de token mais refresh token présent → on tente le refresh
            try {
                const res = await fetch('http://localhost:8080/api/auth/refresh', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ refreshToken }),
                });
                if (res.ok) {
                    const data = await res.json();
                    localStorage.setItem('token', data.token);
                    localStorage.setItem('refreshToken', data.refreshToken);
                    setUser(storedUser);
                    setIsAuth(true);
                } else {
                    // Refresh échoué → on nettoie
                    localStorage.removeItem('token');
                    localStorage.removeItem('refreshToken');
                    localStorage.removeItem('user');
                }
            } catch {
                // Erreur réseau → on garde la session locale pour ne pas
                // déconnecter l'utilisateur si le serveur est temporairement indisponible
                setUser(storedUser);
                setIsAuth(true);
            } finally {
                setReady(true);
            }
        };

        init();
    }, []);

    useEffect(() => {
        const handleLogout = () => {
            setUser(null);
            setIsAuth(false);
        };
        window.addEventListener('auth:logout', handleLogout);
        return () => window.removeEventListener('auth:logout', handleLogout);
    }, []);

    const login = (data) => {
        auth.saveSession(data);
        setUser({ id: data.id, pseudo: data.pseudo });
        setIsAuth(true);
    };

    const logout = () => {
        auth.logout();
        setUser(null);
        setIsAuth(false);
    };

    // On attend que la vérification initiale soit faite avant de rendre l'app
    // pour éviter un flash de déconnexion ou une redirection prématurée
    if (!ready) return null;

    return (
        <AuthContext.Provider value={{ user, isAuth, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}