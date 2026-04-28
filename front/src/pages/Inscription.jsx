import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { User, Mail, Lock, Eye, EyeOff, Gamepad2, AlertCircle, CheckCircle } from 'lucide-react';
import { auth } from '../api.js';
import { useAuth } from '../context/AuthContext.jsx';

const rules = [
    { test: (p) => p.length >= 8, label: '8 caractères minimum' },
    { test: (p) => /[A-Z]/.test(p), label: 'Une majuscule' },
    { test: (p) => /[0-9]/.test(p), label: 'Un chiffre' },
];

export const Inscription = () => {
    const navigate = useNavigate();
    const { login } = useAuth();
    const [form, setForm] = useState({
        pseudo: '',
        email: '',
        motDePasse: '',
        confirm: '',
    });
    const [showPwd, setShowPwd] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleChange = (e) => {
        setForm({ ...form, [e.target.name]: e.target.value });
        setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (form.motDePasse !== form.confirm) {
            setError('Les mots de passe ne correspondent pas');
            return;
        }
        if (form.pseudo.length < 3) {
            setError('Le pseudo doit faire au moins 3 caractères');
            return;
        }
        setLoading(true);
        setError('');
        try {
            const data = await auth.inscrire({
                pseudo: form.pseudo,
                email: form.email,
                motDePasse: form.motDePasse,
            });
            login(data);
            navigate('/');
        } catch (err) {
            setError(err.message || 'Une erreur est survenue');
        } finally {
            setLoading(false);
        }
    };

    const pwdStrength = rules.filter((r) => r.test(form.motDePasse)).length;

    return (
        <div className="min-h-screen bg-primary-black flex items-center justify-center px-4 py-12">
            {/* Background glow */}
            <div className="absolute inset-0 pointer-events-none overflow-hidden">
                <div className="absolute top-[10%] left-[50%] -translate-x-1/2 w-96 h-96
                        bg-red-900 opacity-10 rounded-full blur-3xl" />
            </div>

            <div className="relative w-full max-w-sm">
                {/* Logo */}
                <div className="flex flex-col items-center mb-8">
                    <div className="w-12 h-12 bg-primary-red rounded-xl flex items-center justify-center mb-4">
                        <Gamepad2 className="w-6 h-6 text-white" />
                    </div>
                    <h1 className="text-2xl font-bold text-white">Créer un compte</h1>
                    <p className="text-gray-500 text-sm mt-1">Rejoignez la communauté gaming</p>
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
                        {/* Pseudo */}
                        <div>
                            <label className="block text-sm font-medium text-gray-300 mb-1.5">
                                Pseudo
                            </label>
                            <div className="relative">
                                <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                                <input
                                    type="text"
                                    name="pseudo"
                                    value={form.pseudo}
                                    onChange={handleChange}
                                    className="w-full bg-accent-black border border-gray-700 rounded-lg
                             pl-10 pr-4 py-2.5 text-white text-sm placeholder-gray-600
                             focus:outline-none focus:border-primary-red focus:ring-1 focus:ring-primary-red
                             transition-colors"
                                    placeholder="votre_pseudo"
                                    minLength={3}
                                    maxLength={20}
                                    required
                                />
                            </div>
                            <p className="text-xs text-gray-600 mt-1">Entre 3 et 20 caractères</p>
                        </div>

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
                                    minLength={8}
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

                            {/* Password strength */}
                            {form.motDePasse && (
                                <div className="mt-2 space-y-1">
                                    <div className="flex gap-1">
                                        {[0, 1, 2].map((i) => (
                                            <div
                                                key={i}
                                                className={`h-1 flex-1 rounded-full transition-colors duration-200 ${
                                                    i < pwdStrength
                                                        ? pwdStrength === 1
                                                            ? 'bg-red-500'
                                                            : pwdStrength === 2
                                                                ? 'bg-yellow-500'
                                                                : 'bg-green-500'
                                                        : 'bg-gray-700'
                                                }`}
                                            />
                                        ))}
                                    </div>
                                    <div className="space-y-0.5">
                                        {rules.map((rule) => (
                                            <div key={rule.label} className="flex items-center gap-1.5">
                                                <CheckCircle
                                                    className={`w-3 h-3 transition-colors ${
                                                        rule.test(form.motDePasse) ? 'text-green-500' : 'text-gray-600'
                                                    }`}
                                                />
                                                <span className={`text-xs ${
                                                    rule.test(form.motDePasse) ? 'text-green-500' : 'text-gray-600'
                                                }`}>
                          {rule.label}
                        </span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* Confirmation */}
                        <div>
                            <label className="block text-sm font-medium text-gray-300 mb-1.5">
                                Confirmer le mot de passe
                            </label>
                            <div className="relative">
                                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                                <input
                                    type={showConfirm ? 'text' : 'password'}
                                    name="confirm"
                                    value={form.confirm}
                                    onChange={handleChange}
                                    className={`w-full bg-accent-black border rounded-lg
                             pl-10 pr-10 py-2.5 text-white text-sm placeholder-gray-600
                             focus:outline-none focus:ring-1 transition-colors
                             ${form.confirm && form.confirm !== form.motDePasse
                                        ? 'border-red-700 focus:border-red-600 focus:ring-red-700'
                                        : 'border-gray-700 focus:border-primary-red focus:ring-primary-red'
                                    }`}
                                    placeholder="••••••••"
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowConfirm(!showConfirm)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300"
                                >
                                    {showConfirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                </button>
                            </div>
                            {form.confirm && form.confirm !== form.motDePasse && (
                                <p className="text-xs text-red-400 mt-1">Les mots de passe ne correspondent pas</p>
                            )}
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
                  Création du compte…
                </span>
                            ) : (
                                "Créer mon compte"
                            )}
                        </button>
                    </form>
                </div>

                {/* Footer */}
                <p className="text-center text-gray-500 text-sm mt-6">
                    Déjà un compte ?{' '}
                    <Link to="/connexion" className="text-primary-red hover:text-accent-red font-medium transition-colors">
                        Se connecter
                    </Link>
                </p>
            </div>
        </div>
    );
};