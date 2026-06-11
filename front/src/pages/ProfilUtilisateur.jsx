import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
    ArrowLeft, Loader2, Star, BookMarked, MessageSquare,
    Calendar, Flag, CheckCircle, AlertCircle, ThumbsUp, ThumbsDown
} from 'lucide-react';
import { utilisateurs, avis as avisApi, reports } from '../api.js';
import { useAuth } from '../context/AuthContext.jsx';

const ROLE_LABELS = {
    USER:       { label: 'Membre',       color: 'text-gray-400 bg-gray-800' },
    POSTER:     { label: 'Contributeur', color: 'text-blue-400 bg-blue-950' },
    ADMIN:      { label: 'Modérateur',   color: 'text-orange-400 bg-orange-950' },
    SUPERADMIN: { label: 'Admin',        color: 'text-red-400 bg-red-950' },
};

const RAISONS = [
    { value: 'CONTENU_INAPPROPRIE', label: 'Contenu inapproprié' },
    { value: 'SPAM',                label: 'Spam' },
    { value: 'HARCELEMENT',         label: 'Harcèlement' },
    { value: 'FAUSSE_INFORMATION',  label: 'Fausse information' },
    { value: 'AUTRE',               label: 'Autre' },
];

// ── Modal signalement ─────────────────────────────────────────────────────────

function ReportModal({ titre, onClose, onSubmit }) {
    const [raison, setRaison]   = useState('CONTENU_INAPPROPRIE');
    const [details, setDetails] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try { await onSubmit(raison, details); onClose(); }
        finally { setLoading(false); }
    };

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center px-4"
            style={{ background: 'rgba(0,0,0,0.75)' }}
            onClick={(e) => e.target === e.currentTarget && onClose()}
        >
            <div className="bg-secondary-black border border-gray-700 rounded-2xl p-6 w-full max-w-sm">
                <h3 className="text-white font-semibold text-lg mb-1 flex items-center gap-2">
                    <Flag className="w-4 h-4 text-primary-red" />
                    Signaler cet avis
                </h3>
                {titre && <p className="text-gray-500 text-xs mb-4 truncate">{titre}</p>}
                <form onSubmit={handleSubmit} className="space-y-4">
                    <select
                        value={raison}
                        onChange={(e) => setRaison(e.target.value)}
                        className="w-full bg-accent-black border border-gray-700 rounded-lg
                                   px-3 py-2.5 text-white text-sm focus:outline-none
                                   focus:border-primary-red"
                    >
                        {RAISONS.map(({ value, label }) => (
                            <option key={value} value={value}>{label}</option>
                        ))}
                    </select>
                    <textarea
                        value={details}
                        onChange={(e) => setDetails(e.target.value)}
                        rows={3}
                        placeholder="Précisez si nécessaire…"
                        className="w-full bg-accent-black border border-gray-700 rounded-lg
                                   px-3 py-2 text-white text-sm placeholder-gray-600
                                   resize-none focus:outline-none focus:border-primary-red"
                    />
                    <div className="flex gap-2">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 py-2 rounded-lg border border-gray-700 text-gray-400
                                       text-sm font-medium hover:border-gray-500 transition-colors"
                        >
                            Annuler
                        </button>
                        <button
                            type="submit"
                            disabled={loading}
                            className="flex-1 py-2 rounded-lg bg-primary-red hover:bg-secondary-red
                                       text-white text-sm font-medium transition-colors disabled:opacity-50"
                        >
                            {loading
                                ? <Loader2 className="w-4 h-4 animate-spin mx-auto" />
                                : 'Signaler'
                            }
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

// ── Carte avis avec signalement ───────────────────────────────────────────────

function AvisCard({ avis, isAuth, currentUserId, onLike, onReport }) {
    const [reportOpen, setReportOpen]   = useState(false);
    const [reported, setReported]       = useState(false);
    const [likeLoading, setLikeLoading] = useState(false);
    const [localAvis, setLocalAvis]     = useState(avis);

    const isOwn = currentUserId === avis.utilisateurId;

    const handleLike = async (like) => {
        if (likeLoading) return;
        setLikeLoading(true);
        try {
            const updated = await onLike(avis.id, like);
            if (updated) setLocalAvis(updated);
        } finally {
            setLikeLoading(false);
        }
    };

    const handleReport = async (raison, details) => {
        await onReport(avis.id, raison, details);
        setReported(true);
    };

    return (
        <>
            {reportOpen && (
                <ReportModal
                    titre={`Avis sur "${localAvis.jeuTitre}"`}
                    onClose={() => setReportOpen(false)}
                    onSubmit={handleReport}
                />
            )}
            <div className="bg-secondary-black border border-gray-800 rounded-xl p-4">
                {/* En-tête */}
                <div className="flex items-center justify-between mb-2">
                    <Link
                        to={`/bibliotheque/${localAvis.jeuId}`}
                        className="text-white text-sm font-semibold hover:text-primary-red transition-colors"
                    >
                        {localAvis.jeuTitre}
                    </Link>
                    <span className="text-xs text-gray-600">
                        {new Date(localAvis.date).toLocaleDateString('fr-FR')}
                    </span>
                </div>

                {/* Texte */}
                <p className="text-gray-400 text-sm leading-relaxed line-clamp-3 mb-3">
                    {localAvis.texte}
                </p>

                {/* Pied : likes + actions */}
                <div className="flex items-center gap-3">
                    {/* Compteurs */}
                    <span className="flex items-center gap-1 text-xs text-gray-600">
                        <ThumbsUp className="w-3.5 h-3.5" />
                        {localAvis.likes}
                    </span>
                    <span className="flex items-center gap-1 text-xs text-gray-600">
                        <ThumbsDown className="w-3.5 h-3.5" />
                        {localAvis.dislikes}
                    </span>

                    {/* Boutons d'action (visible seulement si connecté et pas son propre avis) */}
                    {isAuth && !isOwn && (
                        <>
                            <button
                                onClick={() => handleLike(true)}
                                disabled={likeLoading}
                                className="text-gray-600 hover:text-green-400 transition-colors
                                           disabled:opacity-40 ml-1"
                                title="J'aime"
                            >
                                <ThumbsUp className="w-3.5 h-3.5" />
                            </button>
                            <button
                                onClick={() => handleLike(false)}
                                disabled={likeLoading}
                                className="text-gray-600 hover:text-red-400 transition-colors
                                           disabled:opacity-40"
                                title="Je n'aime pas"
                            >
                                <ThumbsDown className="w-3.5 h-3.5" />
                            </button>

                            {/* Signalement */}
                            <div className="ml-auto">
                                {reported ? (
                                    <span className="flex items-center gap-1 text-xs text-green-500">
                                        <CheckCircle className="w-3 h-3" />
                                        Signalé
                                    </span>
                                ) : (
                                    <button
                                        onClick={() => setReportOpen(true)}
                                        className="flex items-center gap-1 text-xs text-gray-600
                                                   hover:text-orange-400 transition-colors"
                                    >
                                        <Flag className="w-3 h-3" />
                                        Signaler
                                    </button>
                                )}
                            </div>
                        </>
                    )}
                </div>
            </div>
        </>
    );
}

// ── Carte note ────────────────────────────────────────────────────────────────

function NoteCard({ note }) {
    return (
        <div className="bg-secondary-black border border-gray-800 rounded-xl p-4 flex items-center gap-3">
            <div className="flex-1 min-w-0">
                <Link
                    to={`/bibliotheque/${note.jeuId}`}
                    className="text-white text-sm font-medium truncate hover:text-primary-red
                               transition-colors block"
                >
                    {note.jeuTitre}
                </Link>
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

// ── Page principale ───────────────────────────────────────────────────────────

export const ProfilUtilisateur = () => {
    const { id }           = useParams();
    const navigate         = useNavigate();
    const { isAuth, user } = useAuth();

    const [profil, setProfil]         = useState(null);
    const [loading, setLoading]       = useState(true);
    const [activeTab, setActiveTab]   = useState('avis');
    const [feedback, setFeedback]     = useState('');
    const [error, setError]           = useState('');

    const toast = (msg, isErr = false) => {
        isErr ? setError(msg) : setFeedback(msg);
        setTimeout(() => { setFeedback(''); setError(''); }, 3000);
    };

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

    const handleLike = async (avisId, like) => {
        try {
            return await avisApi.liker(avisId, like);
        } catch (err) {
            toast(err.message || 'Erreur', true);
            return null;
        }
    };

    const handleReport = async (avisId, raison, details) => {
        try {
            await reports.soumettre({
                typeContenu: 'AVIS',
                idContenu: avisId,
                raison,
                details,
            });
            toast('Signalement envoyé, merci !');
        } catch (err) {
            toast(err.message || 'Erreur lors du signalement', true);
            throw err; // re-throw pour que le modal ne se ferme pas si erreur
        }
    };

    // ── Rendus d'état ──

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
            {/* Toasts */}
            {feedback && (
                <div className="fixed bottom-6 right-6 z-50 px-4 py-3 bg-green-900 border
                                border-green-700 text-green-300 text-sm font-medium rounded-xl
                                shadow-xl flex items-center gap-2">
                    <CheckCircle className="w-4 h-4" />{feedback}
                </div>
            )}
            {error && (
                <div className="fixed bottom-6 right-6 z-50 px-4 py-3 bg-red-950 border
                                border-red-800 text-red-300 text-sm font-medium rounded-xl
                                shadow-xl flex items-center gap-2">
                    <AlertCircle className="w-4 h-4" />{error}
                </div>
            )}

            <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Retour */}
                <button
                    onClick={() => navigate(-1)}
                    className="flex items-center gap-2 text-gray-500 hover:text-white
                               text-sm mb-6 transition-colors"
                >
                    <ArrowLeft className="w-4 h-4" />
                    Retour
                </button>

                {/* En-tête profil */}
                <div className="bg-secondary-black border border-gray-800 rounded-2xl p-6 mb-6">
                    <div className="flex items-start gap-5">
                        {/* Avatar initial */}
                        <div className="w-20 h-20 rounded-2xl bg-accent-black border border-gray-700
                                        flex-shrink-0 flex items-center justify-center overflow-hidden">
                            <span className="text-3xl font-bold text-gray-500">
                                {profil.pseudo?.charAt(0).toUpperCase()}
                            </span>
                        </div>

                        {/* Infos */}
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
                                    Membre depuis{' '}
                                    {new Date(profil.dateCompte).toLocaleDateString('fr-FR', {
                                        month: 'long', year: 'numeric',
                                    })}
                                </div>
                            )}

                            {/* Stats */}
                            <div className="grid grid-cols-3 gap-3">
                                {[
                                    { val: profil.nombreJeux,  icon: BookMarked,    label: 'Jeux' },
                                    { val: profil.nombreAvis,  icon: MessageSquare, label: 'Avis' },
                                    { val: profil.nombreNotes, icon: Star,          label: 'Notes' },
                                ].map(({ val, icon: Icon, label }) => (
                                    <div key={label} className="text-center p-3 bg-accent-black rounded-xl">
                                        <p className="text-xl font-black text-white">{val}</p>
                                        <p className="text-xs text-gray-500 mt-0.5 flex items-center
                                                      justify-center gap-1">
                                            <Icon className="w-3 h-3" />{label}
                                        </p>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>

                {/* Onglets */}
                <div className="flex gap-1 mb-5 bg-secondary-black border border-gray-800 rounded-xl p-1">
                    {[
                        { id: 'avis',  label: 'Derniers avis'  },
                        { id: 'notes', label: 'Dernières notes' },
                    ].map(({ id: tabId, label }) => (
                        <button
                            key={tabId}
                            onClick={() => setActiveTab(tabId)}
                            className={`flex-1 py-2 rounded-lg text-sm font-medium transition-all
                                ${activeTab === tabId
                                ? 'bg-primary-red text-white'
                                : 'text-gray-400 hover:text-white'
                            }`}
                        >
                            {label}
                        </button>
                    ))}
                </div>

                {/* Contenu onglet Avis */}
                {activeTab === 'avis' && (
                    <div className="space-y-3">
                        {!isAuth && profil.derniersAvis?.length > 0 && (
                            <p className="text-xs text-gray-600 pb-1">
                                Connectez-vous pour interagir avec les avis.
                            </p>
                        )}
                        {(profil.derniersAvis || []).length === 0 ? (
                            <div className="text-center py-12 text-gray-600 border border-gray-800 rounded-xl">
                                Aucun avis pour l'instant
                            </div>
                        ) : (
                            (profil.derniersAvis).map((a) => (
                                <AvisCard
                                    key={a.id}
                                    avis={a}
                                    isAuth={isAuth}
                                    currentUserId={user?.id}
                                    onLike={handleLike}
                                    onReport={handleReport}
                                />
                            ))
                        )}
                    </div>
                )}

                {/* Contenu onglet Notes */}
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