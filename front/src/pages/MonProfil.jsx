import { useState, useEffect, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
    Camera, Star, BookMarked, MessageSquare, Calendar,
    Loader2, Trash2, Upload
} from 'lucide-react';
import { utilisateurs, avatar as avatarApi } from '../api.js';
import { useAuth } from '../context/AuthContext.jsx';

export const MonProfil = () => {
    const { user, isAuth } = useAuth();
    const navigate = useNavigate();
    const fileInputRef = useRef(null);

    const [profil, setProfil] = useState(null);
    const [avatarUrl, setAvatarUrl] = useState(null);
    const [loading, setLoading] = useState(true);
    const [uploadingAvatar, setUploadingAvatar] = useState(false);
    const [feedback, setFeedback] = useState('');
    const [error, setError] = useState('');

    const toast = (msg, isErr = false) => {
        if (isErr) setError(msg);
        else setFeedback(msg);
        setTimeout(() => { setFeedback(''); setError(''); }, 3000);
    };

    useEffect(() => {
        if (!isAuth) { navigate('/connexion'); return; }
        const load = async () => {
            setLoading(true);
            try {
                const [profilData, avData] = await Promise.allSettled([
                    utilisateurs.profil(user.id),
                    avatarApi.get(user.id),
                ]);
                if (profilData.status === 'fulfilled') setProfil(profilData.value);
                if (avData.status === 'fulfilled' && avData.value?.url) setAvatarUrl(avData.value.url);
            } finally {
                setLoading(false);
            }
        };
        load();
    }, [isAuth, user]);

    const handleAvatarChange = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        if (file.size > 2 * 1024 * 1024) { toast('Fichier trop volumineux (max 2 Mo)', true); return; }
        setUploadingAvatar(true);
        try {
            const data = await avatarApi.upload(file);
            setAvatarUrl(data.url);
            toast('Avatar mis à jour !');
        } catch (err) {
            toast(err.message || 'Erreur lors de l\'upload', true);
        } finally {
            setUploadingAvatar(false);
        }
    };

    const handleDeleteAvatar = async () => {
        setUploadingAvatar(true);
        try {
            await avatarApi.supprimer();
            setAvatarUrl(null);
            toast('Avatar supprimé');
        } catch (err) {
            toast(err.message || 'Erreur', true);
        } finally {
            setUploadingAvatar(false);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-primary-black flex items-center justify-center">
                <Loader2 className="w-8 h-8 text-primary-red animate-spin" />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-primary-black">
            {feedback && (
                <div className="fixed bottom-6 right-6 z-50 px-4 py-3 bg-green-900 border border-green-700
                                text-green-300 text-sm font-medium rounded-xl shadow-xl">{feedback}</div>
            )}
            {error && (
                <div className="fixed bottom-6 right-6 z-50 px-4 py-3 bg-red-950 border border-red-800
                                text-red-300 text-sm font-medium rounded-xl shadow-xl">{error}</div>
            )}

            <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <h1 className="text-3xl font-black text-white mb-6">Mon Profil</h1>

                {/* Avatar + Info */}
                <div className="bg-secondary-black border border-gray-800 rounded-2xl p-6 mb-6">
                    <div className="flex items-start gap-5">
                        {/* Avatar */}
                        <div className="relative flex-shrink-0">
                            <div className="w-24 h-24 rounded-2xl bg-accent-black border border-gray-700
                                            flex items-center justify-center overflow-hidden">
                                {avatarUrl ? (
                                    <img src={avatarUrl} alt="avatar" className="w-full h-full object-cover" />
                                ) : (
                                    <span className="text-4xl font-bold text-gray-500">
                                        {user?.pseudo?.charAt(0).toUpperCase()}
                                    </span>
                                )}
                            </div>
                            {/* Upload overlay */}
                            <button
                                onClick={() => fileInputRef.current?.click()}
                                disabled={uploadingAvatar}
                                className="absolute -bottom-2 -right-2 w-8 h-8 rounded-full bg-primary-red
                                           hover:bg-secondary-red border-2 border-primary-black flex items-center
                                           justify-center transition-colors disabled:opacity-50"
                                title="Changer l'avatar"
                            >
                                {uploadingAvatar
                                    ? <Loader2 className="w-3.5 h-3.5 text-white animate-spin" />
                                    : <Camera className="w-3.5 h-3.5 text-white" />
                                }
                            </button>
                            <input
                                ref={fileInputRef}
                                type="file"
                                accept="image/jpeg,image/png,image/webp"
                                onChange={handleAvatarChange}
                                className="hidden"
                            />
                        </div>

                        {/* Info */}
                        <div className="flex-1 min-w-0">
                            <h2 className="text-xl font-bold text-white mb-1">{user?.pseudo}</h2>
                            {profil?.dateCompte && (
                                <div className="flex items-center gap-1.5 text-gray-500 text-xs mb-4">
                                    <Calendar className="w-3.5 h-3.5" />
                                    Membre depuis {new Date(profil.dateCompte).toLocaleDateString('fr-FR', {
                                    month: 'long', year: 'numeric'
                                })}
                                </div>
                            )}
                            <div className="flex flex-wrap gap-2">
                                <button
                                    onClick={() => fileInputRef.current?.click()}
                                    disabled={uploadingAvatar}
                                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs
                                               font-medium border border-gray-700 text-gray-400
                                               hover:border-gray-500 hover:text-gray-300 transition-colors
                                               disabled:opacity-50"
                                >
                                    <Upload className="w-3 h-3" />
                                    Changer l'avatar
                                </button>
                                {avatarUrl && (
                                    <button
                                        onClick={handleDeleteAvatar}
                                        disabled={uploadingAvatar}
                                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs
                                                   font-medium border border-gray-700 text-gray-600
                                                   hover:border-red-800 hover:text-red-400 transition-colors
                                                   disabled:opacity-50"
                                    >
                                        <Trash2 className="w-3 h-3" />
                                        Supprimer
                                    </button>
                                )}
                            </div>
                            <p className="text-xs text-gray-600 mt-2">JPG, PNG ou WebP · max 2 Mo</p>
                        </div>
                    </div>
                </div>

                {/* Stats */}
                {profil && (
                    <div className="grid grid-cols-3 gap-3 mb-6">
                        <div className="text-center p-4 bg-secondary-black border border-gray-800 rounded-xl">
                            <p className="text-2xl font-black text-white">{profil.nombreJeux}</p>
                            <p className="text-xs text-gray-500 mt-1 flex items-center justify-center gap-1">
                                <BookMarked className="w-3 h-3" />Jeux
                            </p>
                        </div>
                        <div className="text-center p-4 bg-secondary-black border border-gray-800 rounded-xl">
                            <p className="text-2xl font-black text-white">{profil.nombreAvis}</p>
                            <p className="text-xs text-gray-500 mt-1 flex items-center justify-center gap-1">
                                <MessageSquare className="w-3 h-3" />Avis
                            </p>
                        </div>
                        <div className="text-center p-4 bg-secondary-black border border-gray-800 rounded-xl">
                            <p className="text-2xl font-black text-white">{profil.nombreNotes}</p>
                            <p className="text-xs text-gray-500 mt-1 flex items-center justify-center gap-1">
                                <Star className="w-3 h-3" />Notes
                            </p>
                        </div>
                    </div>
                )}

                {/* Derniers avis */}
                {profil?.derniersAvis?.length > 0 && (
                    <div>
                        <h3 className="text-sm font-semibold text-gray-400 uppercase tracking-wider mb-3">
                            Derniers avis
                        </h3>
                        <div className="space-y-2">
                            {profil.derniersAvis.map((a) => (
                                <div key={a.id} className="bg-secondary-black border border-gray-800 rounded-xl p-4">
                                    <div className="flex items-center justify-between mb-1">
                                        <span className="text-white text-sm font-medium">{a.jeuTitre}</span>
                                        <span className="text-xs text-gray-600">
                                            {new Date(a.date).toLocaleDateString('fr-FR')}
                                        </span>
                                    </div>
                                    <p className="text-gray-400 text-sm line-clamp-2">{a.texte}</p>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};