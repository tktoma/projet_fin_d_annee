import { createContext, useContext, useEffect, useState } from 'react';
import { auth } from '../api.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(() => auth.getUser());
    const [isAuth, setIsAuth] = useState(() => auth.isAuthenticated());

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

    return (
        <AuthContext.Provider value={{ user, isAuth, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}