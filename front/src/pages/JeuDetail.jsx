import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
    Star, ArrowLeft, Plus, ThumbsUp, ThumbsDown, Trash2,
    Loader2, Flag, BookMarked, Send, ChevronDown, ChevronUp,
    Eye, Users, Library
} from 'lucide-react';
import { jeux, avis as avisApi, notes as notesApi, bibliotheque, reports } from '../api.js';
import { useAuth } from '../context/AuthContext.jsx';

const STATUTS = [
    { value: 'JOUER',      label: 'En cours',  color: 'text-yellow-400', bg: 'bg-yellow-950 border-yellow-800' },
    { value: 'A_JOUER',    label: 'À jouer',   color: 'text-blue-400',   bg: 'bg-blue-950 border-blue-800' },
    { value: 'FINIT',      label: 'Terminé',   color: 'text-green-400',  bg: 'bg-green-950 border-green-800' },
    { value: 'ABANDONNER', label: 'Abandonné', color: 'text-gray-400',   bg: 'bg-gray-900 border-gray-700' },
];

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
        e.preventDefault(); setLoading(true);
        try { await onSubmit(raison, details); onClose(); }
        finally { setLoading(false); }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4"
             style={{ background: 'rgba(0,0,0,0.75)' }}
             onClick={(e) => e.target === e.currentTarget && onClose()}>
            <div className="bg-secondary-black border border-gray-700 rounded-2xl p-6 w-full max-w-sm">
                <h3 className="text-white font-semibold text-lg mb-1 flex items-center gap-2">
                    <Flag className="w-4 h-4 text-primary-red" />Signaler
                </h3>
                {titre && <p className="text-gray-500 text-xs mb-4 truncate">{titre}</p>}
                <form onSubmit={handleSubmit} className="space-y-4">
                    <select value={raison} onChange={(e) => setRaison(e.target.value)}
                            className="w-full bg-accent-black border border-gray-700 rounded-lg
                                       px-3 py-2.5 text-white text-sm focus:outline-none focus:border-primary-red">
                        {RAISONS.map(({ value, label }) => (
                            <option key={value} value={value}>{label}</option>
                        ))}
                    </select>
                    <textarea value={details} onChange={(e) => setDetails(e.target.value)}
                              rows={3} placeholder="Précisez si nécessaire…"
                              className="w-full bg-accent-black border border-gray-700 rounded-lg px-3 py-2
                                         text-white text-sm placeholder-gray-600 resize-none
                                         focus:outline-none focus:border-primary-red" />
                    <div className="flex gap-2">
                        <button type="button" onClick={onClose}
                                className="flex-1 py-2 rounded-lg border border-gray-700 text-gray-400
                                           text-sm font-medium hover:border-gray-500 transition-colors">
                            Annuler
                        </button>
                        <button type="submit" disabled={loading}
                                className="flex-1 py-2 rounded-lg bg-primary-red hover:bg-secondary-red
                                           text-white text-sm font-medium transition-colors disabled:opacity-50">
                            {loading ? <Loader2 className="w-4 h-4 animate-spin mx-auto" /> : 'Signaler'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

// ── Étoiles ───────────────────────────────────────────────────────────────────

function StarRating({ value, onChange }) {
    const [hover, setHover] = useState(0);
    return (
        <div className="flex gap-0.5">
            {[1,2,3,4,5,6,7,8,9,10].map((n) => (
                <button key={n} type="button"
                        onClick={() => onChange && onChange(n)}
                        onMouseEnter={() => setHover(n)}
                        onMouseLeave={() => setHover(0)}
                        className="cursor-pointer transition-colors">
                    <Star className={`w-4 h-4 ${n <= (hover || value)
                        ? 'text-yellow-400 fill-yellow-400' : 'text-gray-600'}`} />
                </button>
            ))}
        </div>
    );
}

// ── Description ───────────────────────────────────────────────────────────────

function Description({ text }) {
    const [expanded, setExpanded] = useState(false);
    const LIMIT = 300;
    const isLong = text && text.length > LIMIT;
    const displayed = isLong && !expanded ? text.slice(0, LIMIT) + '…' : text;
    return (
        <div className="bg-secondary-black border border-gray-800 rounded-xl p-5 mb-6">
            <h2 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Description</h2>
            <p className="text-gray-300 text-sm leading-relaxed">{displayed}</p>
            {isLong && (
                <button onClick={() => setExpanded(!expanded)}
                        className="mt-2 flex items-center gap-1 text-xs text-primary-red
                                   hover:text-accent-red transition-colors font-medium">
                    {expanded
                        ? <><ChevronUp className="w-3.5 h-3.5" />Voir moins</>
                        : <><ChevronDown className="w-3.5 h-3.5" />Voir plus</>}
                </button>
            )}
        </div>
    );
}

// ── Bloc statistiques ─────────────────────────────────────────────────────────

function StatsBibliotheque({ jeu }) {
    const total = jeu.nbBibliotheque || 0;
    const statuts = jeu.statutStats || {};

    if (total === 0 && (jeu.vues || 0) === 0) return null;

    return (
        <div className="bg-secondary-black border border-gray-800 rounded-xl p-5 mb-6">
            <h2 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-4">
                Statistiques
            </h2>

            {/* Compteurs principaux */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mb-4">
                <div className="bg-accent-black rounded-xl p-3 text-center">
                    <div className="flex items-center justify-center gap-1.5 mb-1">
                        <Eye className="w-3.5 h-3.5 text-gray-500" />
                        <span className="text-xs text-gray-500">Consultations</span>
                    </div>
                    <p className="text-white font-black text-xl">
                        {(jeu.vues || 0).toLocaleString('fr-FR')}
                    </p>
                </div>

                <div className="bg-accent-black rounded-xl p-3 text-center">
                    <div className="flex items-center justify-center gap-1.5 mb-1">
                        <Library className="w-3.5 h-3.5 text-gray-500" />
                        <span className="text-xs text-gray-500">En collection</span>
                    </div>
                    <p className="text-white font-black text-xl">
                        {total.toLocaleString('fr-FR')}
                    </p>
                </div>

                <div className="bg-accent-black rounded-xl p-3 text-center col-span-2 sm:col-span-1">
                    <div className="flex items-center justify-center gap-1.5 mb-1">
                        <Users className="w-3.5 h-3.5 text-gray-500" />
                        <span className="text-xs text-gray-500">Terminé</span>
                    </div>
                    <p className="text-white font-black text-xl">
                        {(statuts['FINIT'] || 0).toLocaleString('fr-FR')}
                    </p>
                </div>
            </div>

            {/* Répartition des statuts */}
            {total > 0 && (
                <div>
                    <p className="text-xs text-gray-500 mb-2">Répartition dans les bibliothèques</p>
                    <div className="space-y-2">
                        {STATUTS.map(({ value, label, color, bg }) => {
                            const count = statuts[value] || 0;
                            const pct   = total > 0 ? Math.round((count / total) * 100) : 0;
                            if (count === 0) return null;
                            return (
                                <div key={value}>
                                    <div className="flex justify-between text-xs mb-1">
                                        <span className={`font-medium ${color}`}>{label}</span>
                                        <span className="text-gray-500">
                                            {count.toLocaleString('fr-FR')} ({pct}%)
                                        </span>
                                    </div>
                                    <div className="w-full bg-gray-800 rounded-full h-1.5 overflow-hidden">
                                        <div
                                            className={`h-full rounded-full transition-all duration-700 ${
                                                value === 'JOUER'      ? 'bg-yellow-400' :
                                                    value === 'A_JOUER'    ? 'bg-blue-400'   :
                                                        value === 'FINIT'      ? 'bg-green-400'  :
                                                            'bg-gray-500'
                                            }`}
                                            style={{ width: `${pct}%` }}
                                        />
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}
        </div>
    );
}

// ── Carte avis ────────────────────────────────────────────────────────────────

function AvisCard({ avis, isOwn, onLike, onDelete, isAuth, onReport }) {
    const [reportOpen, setReportOpen] = useState(false);
    return (
        <div className="bg-secondary-black border border-gray-800 rounded-xl p-4">
            <div className="flex items-start justify-between gap-3 mb-2">
                <Link to={`/profil/${avis.utilisateurId}`}
                      className="font-semibold text-white text-sm hover:text-primary-red transition-colors">
                    {avis.utilisateurPseudo}
                </Link>
                <span className="text-xs text-gray-600 flex-shrink-0">
                    {new Date(avis.date).toLocaleDateString('fr-FR')}
                </span>
            </div>
            <p className="text-gray-300 text-sm leading-relaxed mb-3">{avis.texte}</p>
            <div className="flex items-center gap-3">
                <span className="flex items-center gap-1 text-xs text-gray-600">
                    <ThumbsUp className="w-3.5 h-3.5" />{avis.likes}
                </span>
                <span className="flex items-center gap-1 text-xs text-gray-600">
                    <ThumbsDown className="w-3.5 h-3.5" />{avis.dislikes}
                </span>
                {isAuth && !isOwn && (
                    <>
                        <button onClick={() => onLike(avis.id, true)}
                                className="text-gray-500 hover:text-green-400 transition-colors ml-1">
                            <ThumbsUp className="w-3.5 h-3.5" />
                        </button>
                        <button onClick={() => onLike(avis.id, false)}
                                className="text-gray-500 hover:text-red-400 transition-colors">
                            <ThumbsDown className="w-3.5 h-3.5" />
                        </button>
                        <button onClick={() => setReportOpen(true)}
                                className="ml-auto flex items-center gap-1 text-xs text-gray-600
                                           hover:text-orange-400 transition-colors">
                            <Flag className="w-3 h-3" />Signaler
                        </button>
                    </>
                )}
                {isOwn && (
                    <button onClick={() => onDelete(avis.id)}
                            className="ml-auto flex items-center gap-1 text-xs text-gray-600
                                       hover:text-red-400 transition-colors">
                        <Trash2 className="w-3 h-3" />Supprimer
                    </button>
                )}
            </div>
            {reportOpen && (
                <ReportModal
                    titre={`Avis de ${avis.utilisateurPseudo}`}
                    onClose={() => setReportOpen(false)}
                    onSubmit={(r, d) => onReport(avis.id, r, d)}
                />
            )}
        </div>
    );
}

// ── Page principale ───────────────────────────────────────────────────────────

export const JeuDetail = () => {
    const { id }           = useParams();
    const navigate         = useNavigate();
    const { isAuth, user } = useAuth();

    const [jeu, setJeu]             = useState(null);
    const [avisList, setAvisList]   = useState([]);
    const [loading, setLoading]     = useState(true);
    const [monAvis, setMonAvis]     = useState('');
    const [maNote, setMaNote]       = useState(0);
    const [sendingAvis, setSendingAvis] = useState(false);
    const [sendingNote, setSendingNote] = useState(false);
    const [showBibMenu, setShowBibMenu] = useState(false);
    const [bibStatus, setBibStatus]     = useState(null);
    const [feedback, setFeedback]       = useState('');
    const [error, setError]             = useState('');
    const [jeuReportOpen, setJeuReportOpen] = useState(false);

    const toast = (msg, isErr = false) => {
        isErr ? setError(msg) : setFeedback(msg);
        setTimeout(() => { setFeedback(''); setError(''); }, 3000);
    };

    useEffect(() => {
        const load = async () => {
            setLoading(true);
            try {
                const [jeuData, avisData] = await Promise.all([
                    jeux.getById(id),
                    avisApi.duJeu(id),
                ]);
                setJeu(jeuData);
                setAvisList(avisData || []);
            } catch { setJeu(null); }
            finally { setLoading(false); }
        };
        load();
    }, [id]);

    useEffect(() => {
        const close = () => setShowBibMenu(false);
        if (showBibMenu) document.addEventListener('click', close);
        return () => document.removeEventListener('click', close);
    }, [showBibMenu]);

    const handleAddBib = async (statut) => {
        try {
            await bibliotheque.ajouter(Number(id), statut);
            setBibStatus(statut); setShowBibMenu(false);
            toast('Jeu ajouté à votre bibliothèque !');
        } catch (err) {
            setShowBibMenu(false);
            toast(err.status === 409 ? 'Déjà dans votre bibliothèque' : "Erreur lors de l'ajout", true);
        }
    };

    const handleSendAvis = async (e) => {
        e.preventDefault();
        if (!monAvis.trim() || monAvis.length < 10) return;
        setSendingAvis(true);
        try {
            const newAvis = await avisApi.ajouter(Number(id), monAvis);
            setAvisList((prev) => [newAvis, ...prev.filter((a) => a.utilisateurId !== user.id)]);
            setMonAvis(''); toast('Avis publié !');
        } catch (err) { toast(err.message || 'Erreur', true); }
        finally { setSendingAvis(false); }
    };

    const handleNote = async (val) => {
        setMaNote(val); setSendingNote(true);
        try {
            await notesApi.noter(Number(id), val);
            toast(`Note ${val}/10 enregistrée !`);
            const updated = await jeux.getById(id);
            setJeu(updated);
        } catch (err) { toast(err.message || 'Erreur', true); }
        finally { setSendingNote(false); }
    };

    const handleLike = async (avisId, like) => {
        try {
            const updated = await avisApi.liker(avisId, like);
            setAvisList((prev) => prev.map((a) => a.id === avisId ? updated : a));
        } catch (err) { toast(err.message || 'Erreur', true); }
    };

    const handleDeleteAvis = async (avisId) => {
        try {
            await avisApi.supprimer(avisId);
            setAvisList((prev) => prev.filter((a) => a.id !== avisId));
            toast('Avis supprimé');
        } catch { toast('Erreur lors de la suppression', true); }
    };

    const handleReportAvis = async (avisId, raison, details) => {
        try {
            await reports.soumettre({ typeContenu: 'AVIS', idContenu: avisId, raison, details });
            toast('Signalement envoyé');
        } catch (err) { toast(err.message || 'Erreur', true); }
    };

    const handleReportJeu = async (raison, details) => {
        try {
            await reports.soumettre({ typeContenu: 'JEU', idContenu: Number(id), raison, details });
            toast('Jeu signalé, merci !');
        } catch (err) { toast(err.message || 'Erreur', true); }
    };

    if (loading) return (
        <div className="min-h-screen bg-primary-black flex items-center justify-center">
            <Loader2 className="w-8 h-8 text-primary-red animate-spin" />
        </div>
    );

    if (!jeu) return (
        <div className="min-h-screen bg-primary-black flex flex-col items-center justify-center gap-4">
            <p className="text-gray-400">Jeu introuvable</p>
            <button onClick={() => navigate(-1)} className="text-primary-red text-sm hover:underline">
                ← Retour
            </button>
        </div>
    );

    const monAvisExistant = avisList.find((a) => a.utilisateurId === user?.id);

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

            {jeuReportOpen && (
                <ReportModal
                    titre={jeu.titre}
                    onClose={() => setJeuReportOpen(false)}
                    onSubmit={handleReportJeu}
                />
            )}

            <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <button onClick={() => navigate(-1)}
                        className="flex items-center gap-2 text-gray-500 hover:text-white text-sm mb-6 transition-colors">
                    <ArrowLeft className="w-4 h-4" />Retour
                </button>

                {/* Header */}
                <div className="flex flex-col sm:flex-row gap-6 mb-8">
                    <div className="w-40 h-56 flex-shrink-0 rounded-xl overflow-hidden bg-accent-black self-start">
                        {jeu.coverUrl
                            ? <img src={jeu.coverUrl} alt={jeu.titre} className="w-full h-full object-cover" />
                            : <div className="w-full h-full flex items-center justify-center text-5xl text-gray-700">🎮</div>
                        }
                    </div>

                    <div className="flex-1 min-w-0">
                        {/* Titre + signaler */}
                        <div className="flex items-start justify-between gap-3 mb-2">
                            <h1 className="text-3xl font-black text-white">{jeu.titre}</h1>
                            {isAuth && (
                                <button onClick={() => setJeuReportOpen(true)}
                                        className="flex-shrink-0 flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg
                                                   border border-gray-700 text-gray-500 text-xs font-medium
                                                   hover:border-orange-700 hover:text-orange-400 transition-colors mt-1">
                                    <Flag className="w-3 h-3" />
                                    <span className="hidden sm:inline">Signaler</span>
                                </button>
                            )}
                        </div>

                        {/* Badges */}
                        <div className="flex flex-wrap items-center gap-2 mb-3">
                            {jeu.source && (
                                <span className={`text-xs px-2 py-0.5 rounded-full border font-medium
                                    ${jeu.source === 'igdb'
                                    ? 'text-blue-400 bg-blue-950 border-blue-800'
                                    : 'text-purple-400 bg-purple-950 border-purple-800'}`}>
                                    {jeu.source === 'igdb' ? 'IGDB' : 'Ajout manuel'}
                                </span>
                            )}
                            {jeu.genre      && <span className="text-xs px-2.5 py-1 rounded-full bg-gray-800 text-gray-300">{jeu.genre}</span>}
                            {jeu.plateforme && <span className="text-xs px-2.5 py-1 rounded-full bg-gray-800 text-gray-300">{jeu.plateforme}</span>}
                            {jeu.dateSortie && <span className="text-xs px-2.5 py-1 rounded-full bg-gray-800 text-gray-400">{new Date(jeu.dateSortie).getFullYear()}</span>}
                        </div>

                        {/* Note + vues rapides */}
                        <div className="flex flex-wrap items-center gap-4 mb-5">
                            <div className="flex items-center gap-2">
                                <Star className="w-5 h-5 text-yellow-400 fill-yellow-400" />
                                <span className="text-white font-bold text-xl">
                                    {jeu.noteMoyenne > 0 ? jeu.noteMoyenne.toFixed(1) : '—'}
                                </span>
                                <span className="text-gray-500 text-sm">/10 · {avisList.length} avis</span>
                            </div>
                            {(jeu.vues || 0) > 0 && (
                                <div className="flex items-center gap-1.5 text-gray-500 text-xs">
                                    <Eye className="w-3.5 h-3.5" />
                                    {(jeu.vues).toLocaleString('fr-FR')} vues
                                </div>
                            )}
                            {(jeu.nbBibliotheque || 0) > 0 && (
                                <div className="flex items-center gap-1.5 text-gray-500 text-xs">
                                    <Library className="w-3.5 h-3.5" />
                                    {(jeu.nbBibliotheque).toLocaleString('fr-FR')} collections
                                </div>
                            )}
                        </div>

                        {/* Ma note */}
                        {isAuth && (
                            <div className="mb-5">
                                <p className="text-xs text-gray-500 mb-1.5">Ma note</p>
                                <div className="flex items-center gap-3">
                                    <StarRating value={maNote} onChange={handleNote} />
                                    {sendingNote && <Loader2 className="w-3.5 h-3.5 text-gray-500 animate-spin" />}
                                    {maNote > 0 && !sendingNote && (
                                        <span className="text-xs text-yellow-400 font-medium">{maNote}/10</span>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* Ajouter bibliothèque */}
                        {isAuth && (
                            <div className="relative inline-block">
                                {bibStatus ? (
                                    <div className="flex items-center gap-2 text-sm text-green-400">
                                        <BookMarked className="w-4 h-4" />
                                        {STATUTS.find((s) => s.value === bibStatus)?.label}
                                    </div>
                                ) : (
                                    <>
                                        <button onClick={(e) => { e.stopPropagation(); setShowBibMenu(!showBibMenu); }}
                                                className="flex items-center gap-2 px-4 py-2 rounded-lg
                                                           bg-primary-red hover:bg-secondary-red text-white
                                                           text-sm font-medium transition-colors">
                                            <Plus className="w-4 h-4" />Ajouter à ma bibliothèque
                                        </button>
                                        {showBibMenu && (
                                            <div className="absolute top-full left-0 mt-1 z-10 w-48 bg-secondary-black
                                                            border border-gray-700 rounded-xl overflow-hidden shadow-xl"
                                                 onClick={(e) => e.stopPropagation()}>
                                                {STATUTS.map(({ value, label, color }) => (
                                                    <button key={value} onClick={() => handleAddBib(value)}
                                                            className={`w-full px-4 py-2.5 text-left text-sm font-medium
                                                                        hover:bg-gray-800 transition-colors ${color}`}>
                                                        {label}
                                                    </button>
                                                ))}
                                            </div>
                                        )}
                                    </>
                                )}
                            </div>
                        )}
                    </div>
                </div>

                {/* Description */}
                {jeu.description && <Description text={jeu.description} />}

                {/* Stats bibliothèque */}
                <StatsBibliotheque jeu={jeu} />

                {/* Avis */}
                <div>
                    <h2 className="text-xl font-bold text-white mb-5">
                        Avis
                        {avisList.length > 0 && (
                            <span className="text-gray-500 font-normal text-base ml-2">
                                ({avisList.length})
                            </span>
                        )}
                    </h2>

                    {isAuth && !monAvisExistant && (
                        <form onSubmit={handleSendAvis} className="mb-6">
                            <div className="bg-secondary-black border border-gray-800 rounded-xl p-4">
                                <textarea value={monAvis} onChange={(e) => setMonAvis(e.target.value)}
                                          rows={3} placeholder="Partagez votre avis… (10 caractères min.)"
                                          className="w-full bg-transparent text-white text-sm
                                                     placeholder-gray-600 resize-none focus:outline-none" />
                                <div className="flex items-center justify-between mt-2 pt-2 border-t border-gray-800">
                                    <span className={`text-xs ${monAvis.length < 10 ? 'text-gray-600' : 'text-gray-400'}`}>
                                        {monAvis.length}/2000
                                    </span>
                                    <button type="submit" disabled={sendingAvis || monAvis.length < 10}
                                            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg
                                                       bg-primary-red hover:bg-secondary-red text-white
                                                       text-xs font-medium transition-colors
                                                       disabled:opacity-50 disabled:cursor-not-allowed">
                                        {sendingAvis ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                                        Publier
                                    </button>
                                </div>
                            </div>
                        </form>
                    )}

                    {isAuth && monAvisExistant && (
                        <div className="mb-4 p-3 bg-gray-900 border border-gray-800 rounded-lg text-xs text-gray-500">
                            Vous avez déjà publié un avis pour ce jeu.
                        </div>
                    )}

                    {!isAuth && (
                        <div className="mb-6 p-4 bg-secondary-black border border-gray-800 rounded-xl text-sm text-gray-500">
                            <Link to="/connexion" className="text-primary-red hover:underline">Connectez-vous</Link>{' '}
                            pour laisser un avis.
                        </div>
                    )}

                    {avisList.length === 0 ? (
                        <div className="text-center py-12 text-gray-600 border border-gray-800 rounded-xl">
                            Aucun avis pour l'instant. Soyez le premier !
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {avisList.map((a) => (
                                <AvisCard key={a.id} avis={a}
                                          isOwn={a.utilisateurId === user?.id}
                                          isAuth={isAuth}
                                          onLike={handleLike}
                                          onDelete={handleDeleteAvis}
                                          onReport={handleReportAvis} />
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};