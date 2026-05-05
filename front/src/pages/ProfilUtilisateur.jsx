import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
    ArrowLeft, Loader2, Star, BookMarked, MessageSquare,
    Calendar, Shield, User
} from 'lucide-react';
import { utilisateurs } from '../api.js';

const ROLE_LABELS = {
    USER: { label: 'Membre', color: 'text-gray-400 bg-gray-800' },
    POSTER: { label: 'Contributeur', color: 'text-blue-400 bg-blue-950' },
    ADMIN: { label: 'Modérateur', color: 'text-orange-400 bg-orange-950' },
    SUPERADMIN: { label: 'Admin', color: 'text-red-400 bg-red-950' },
};

function AvisCard({ avis }) {
    return (
        <div className="bg-secondary-black border border-gray-800 rounded-xl p-4">
            <div className="flex items-center justify-between mb-2">
                <span className="text-white text-sm font-medium">{avis.jeuTitre}</span>
                <span className="text-xs text-gray-600">
                    {new Date(avis.date).toLocaleDateString('fr-FR')}
                </span>
            </div>
            <p className="text-gray-400 text-sm leading-relaxed line-clamp-3">{avis.texte}</p>
            <div className="flex items-center gap-3 mt-2">
                <span className="text-xs text-gray-600">👍 {avis.likes}</span>
                <span className="text-xs text-gray-600">👎 {avis.dislikes}</span>
            </div>
        </div>
    );
}

function NoteCard({ note }) {
    return (
        <div className="bg-secondary-black border border-gray-800 rounded-xl p-4 flex items-center gap-3">
            <div className="flex-1 min-w-0">
                <p className="text-white text-sm font-medium truncate">{note.jeuTitre}</p>
                <p className="text-xs text-gray-600 mt-0.5">
                    {new Date(note.date).toLocaleDateString('fr-FR')}
                </p>
            </div>
            <div className="flex items-center gap-1 flex-shrink-0">
                <Star className="w-4 h-4 text-yellow-400 fill-yellow-400" />
                <span className="text-white font-bold text-sm">{note.valeur}</span>
                <span className="text-gray-500 text-xs">/10</span>
            </div>
        </div>
    );
}

export const ProfilUtilisateur = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [profil, setProfil] = useState(null);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('avis');

    useEffect(() => {
        const load = async () => {
            setLoading(true);
            try {
                const data = await utilisateurs.profil(id);
                setProfil(data);
            } catch {
                setProfil(null);
            } finally {
                setLoading(false);
            }
        };
        load();
    }, [id]);

    if (loading) {
        return (
            <div className="min-h-screen bg-primary-black flex items-center justify-center">
                <Loader2 className="w-8 h-8 text-primary-red animate-spin" />
            </div>
        );
    }

    if (!profil) {
        return (
            <div className="min-h-screen bg-primary-black flex flex-col items-center justify-center gap-4">
                <p className="text-gray-400">Profil introuvable</p>
                <button onClick={() => navigate(-1)} className="text-primary-red text-sm hover:underline">
                    ← Retour
                </button>
            </div>
        );
    }

    const role = ROLE_LABELS[profil.role] || ROLE_LABELS.USER;

    return (
        <div className="min-h-screen bg-primary-black">
            <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Back */}
                <button
                    onClick={() => navigate(-1)}
                    className="flex items-center gap-2 text-gray-500 hover:text-white text-sm mb-6 transition-colors"
                >
                    <ArrowLeft className="w-4 h-4" />
                    Retour
                </button>

                {/* Profile header */}
                <div className="bg-secondary-black border border-gray-800 rounded-2xl p-6 mb-6">
                    <div className="flex items-start gap-5">
                        {/* Avatar */}
                        <div className="w-20 h-20 rounded-2xl bg-accent-black border border-gray-700 flex-shrink-0
                                        flex items-center justify-center overflow-hidden">
                            <span className="text-3xl font-bold text-gray-500">
                                {profil.pseudo?.charAt(0).toUpperCase()}
                            </span>
                        </div>

                        {/* Info */}
                        <div className="flex-1 min-w-0">
                            <div className="flex flex-wrap items-center gap-2 mb-1">
                                <h1 className="text-2xl font-black text-white">{profil.pseudo}</h1>
                                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${role.color}`}>
                                    {role.label}
                                </span>
                            </div>
                            {profil.dateCompte && (
                                <div className="flex items-center gap-1.5 text-gray-500 text-xs mb-4">
                                    <Calendar className="w-3.5 h-3.5" />
                                    Membre depuis {new Date(profil.dateCompte).toLocaleDateString('fr-FR', {
                                    month: 'long', year: 'numeric'
                                })}
                                </div>
                            )}

                            {/* Stats */}
                            <div className="grid grid-cols-3 gap-3">
                                <div className="text-center p-3 bg-accent-black rounded-xl">
                                    <p className="text-xl font-black text-white">{profil.nombreJeux}</p>
                                    <p className="text-xs text-gray-500 mt-0.5 flex items-center justify-center gap-1">
                                        <BookMarked className="w-3 h-3" />Jeux
                                    </p>
                                </div>
                                <div className="text-center p-3 bg-accent-black rounded-xl">
                                    <p className="text-xl font-black text-white">{profil.nombreAvis}</p>
                                    <p className="text-xs text-gray-500 mt-0.5 flex items-center justify-center gap-1">
                                        <MessageSquare className="w-3 h-3" />Avis
                                    </p>
                                </div>
                                <div className="text-center p-3 bg-accent-black rounded-xl">
                                    <p className="text-xl font-black text-white">{profil.nombreNotes}</p>
                                    <p className="text-xs text-gray-500 mt-0.5 flex items-center justify-center gap-1">
                                        <Star className="w-3 h-3" />Notes
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Tabs */}
                <div className="flex gap-1 mb-5 bg-secondary-black border border-gray-800 rounded-xl p-1">
                    {[
                        { id: 'avis', label: 'Derniers avis' },
                        { id: 'notes', label: 'Dernières notes' },
                    ].map(({ id: tabId, label }) => (
                        <button
                            key={tabId}
                            onClick={() => setActiveTab(tabId)}
                            className={`flex-1 py-2 rounded-lg text-sm font-medium transition-all ${
                                activeTab === tabId
                                    ? 'bg-primary-red text-white'
                                    : 'text-gray-400 hover:text-white'
                            }`}
                        >
                            {label}
                        </button>
                    ))}
                </div>

                {/* Content */}
                {activeTab === 'avis' && (
                    <div className="space-y-3">
                        {(profil.derniersAvis || []).length === 0 ? (
                            <div className="text-center py-12 text-gray-600 border border-gray-800 rounded-xl">
                                Aucun avis pour l'instant
                            </div>
                        ) : (
                            profil.derniersAvis.map((a) => (
                                <AvisCard key={a.id} avis={a} />
                            ))
                        )}
                    </div>
                )}

                {activeTab === 'notes' && (
                    <div className="space-y-3">
                        {(profil.dernieresNotes || []).length === 0 ? (
                            <div className="text-center py-12 text-gray-600 border border-gray-800 rounded-xl">
                                Aucune note pour l'instant
                            </div>
                        ) : (
                            profil.dernieresNotes.map((n) => (
                                <NoteCard key={n.id} note={n} />
                            ))
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};