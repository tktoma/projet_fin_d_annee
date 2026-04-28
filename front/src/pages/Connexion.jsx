import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, Eye, EyeOff, Gamepad2, AlertCircle } from 'lucide-react';
import { auth } from '../api.js';
import { useAuth } from '../context/AuthContext.jsx';

export const Connexion = () => {
    const navigate = useNavigate();
    const { login } = useAuth();
    const [form, setForm] = useState({ email: '', motDePasse: '' });
    const [showPwd, setShowPwd] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleChange = (e) => {
        setForm({ ...form, [e.target.name]: e.target.value });
        setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        try {
            const data = await auth.connecter(form);
            login(data);
            navigate('/');
        } catch (err) {
            setError(err.message || 'Identifiants incorrects');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-primary-black flex items-center justify-center px-4">
            {/* Background glow */}
            <div className="absolute inset-0 pointer-events-none overflow-hidden">
                <div className="absolute top-[20%] left-[50%] -translate-x-1/2 w-96 h-96
                        bg-red-900 opacity-10 rounded-full blur-3xl" />
            </div>

            <div className="relative w-full max-w-sm">
                {/* Logo */}
                <div className="flex flex-col items-center mb-8">
                    <div className="w-12 h-12 bg-primary-red rounded-xl flex items-center justify-center mb-4">
                        <Gamepad2 className="w-6 h-6 text-white" />
                    </div>
                    <h1 className="text-2xl font-bold text-white">Bon retour !</h1>
                    <p className="text-gray-500 text-sm mt-1">Connectez-vous à votre compte</p>
                </div>

                {/* Card */}
                <div className="bg-secondary-black border border-gray-800 rounded-2xl p-6 sm:p-8">
                    {error && (
                        <div className="flex items-center gap-2 p-3 mb-4 rounded-lg
                            bg-red-950 border border-red-900 text-red-400 text-sm">
                            <AlertCircle className="w-4 h-4 flex-shrink-0" />
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-4">
                        {/* Email */}
                        <div>
                            <label className="block text-sm font-medium text-gray-300 mb-1.5">
                                Email
                            </label>
                            <div className="relative">
                                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                                <input
                                    type="email"
                                    name="email"
                                    value={form.email}
                                    onChange={handleChange}
                                    className="w-full bg-accent-black border border-gray-700 rounded-lg
                             pl-10 pr-4 py-2.5 text-white text-sm placeholder-gray-600
                             focus:outline-none focus:border-primary-red focus:ring-1 focus:ring-primary-red
                             transition-colors"
                                    placeholder="vous@exemple.com"
                                    required
                                />
                            </div>
                        </div>

                        {/* Mot de passe */}
                        <div>
                            <label className="block text-sm font-medium text-gray-300 mb-1.5">
                                Mot de passe
                            </label>
                            <div className="relative">
                                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                                <input
                                    type={showPwd ? 'text' : 'password'}
                                    name="motDePasse"
                                    value={form.motDePasse}
                                    onChange={handleChange}
                                    className="w-full bg-accent-black border border-gray-700 rounded-lg
                             pl-10 pr-10 py-2.5 text-white text-sm placeholder-gray-600
                             focus:outline-none focus:border-primary-red focus:ring-1 focus:ring-primary-red
                             transition-colors"
                                    placeholder="••••••••"
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPwd(!showPwd)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300"
                                >
                                    {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                </button>
                            </div>
                        </div>

                        {/* Submit */}
                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full py-2.5 px-4 rounded-lg bg-primary-red hover:bg-secondary-red
                         text-white font-semibold text-sm transition-all duration-200
                         disabled:opacity-50 disabled:cursor-not-allowed hover:scale-[1.02]
                         mt-2"
                        >
                            {loading ? (
                                <span className="flex items-center justify-center gap-2">
                  <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Connexion…
                </span>
                            ) : (
                                'Se connecter'
                            )}
                        </button>
                    </form>
                </div>

                {/* Footer */}
                <p className="text-center text-gray-500 text-sm mt-6">
                    Pas encore de compte ?{' '}
                    <Link to="/inscription" className="text-primary-red hover:text-accent-red font-medium transition-colors">
                        S'inscrire
                    </Link>
                </p>
            </div>
        </div>
    );
};