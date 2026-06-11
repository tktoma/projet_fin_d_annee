import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Flag, Trash2, Loader2, CheckCircle, AlertCircle,
    ChevronDown, ChevronUp, Clock, XCircle, BadgeCheck,
    Layers, Gamepad2, MessageSquare, User, StickyNote,
} from 'lucide-react';
import { reports as reportsApi } from '../api.js';
import { useAuth } from '../context/AuthContext.jsx';

// ─────────────────────────────────────────────────────────────────────────────
// Constantes
// ─────────────────────────────────────────────────────────────────────────────

const ADMIN_ROLES = ['ADMIN', 'SUPERADMIN'];
const BASE_URL    = 'http://localhost:8080/api';

function getRoleFromToken() {
    const token = localStorage.getItem('token');
    if (!token) return null;
    try { return JSON.parse(atob(token.split('.')[1])).role || null; }
    catch { return null; }
}

const RAISONS_LABELS = {
    CONTENU_INAPPROPRIE : 'Contenu inapproprié',
    SPAM                : 'Spam',
    HARCELEMENT         : 'Harcèlement',
    FAUSSE_INFORMATION  : 'Fausse information',
    AUTRE               : 'Autre',
};

const TYPE_META = {
    AVIS        : { label: 'Avis',         icon: MessageSquare, color: 'text-purple-400', bg: 'bg-purple-950 border-purple-800' },
    JEU         : { label: 'Jeu',          icon: Gamepad2,      color: 'text-blue-400',   bg: 'bg-blue-950 border-blue-800'   },
    UTILISATEUR : { label: 'Utilisateur',  icon: User,          color: 'text-orange-400', bg: 'bg-orange-950 border-orange-800' },
    NOTE        : { label: 'Note',         icon: StickyNote,    color: 'text-gray-400',   bg: 'bg-gray-800 border-gray-700'   },
};

// ─────────────────────────────────────────────────────────────────────────────
// Helper : supprimer le contenu via l'API admin
// ─────────────────────────────────────────────────────────────────────────────

async function deleteContent(typeContenu, idContenu) {
    const token = localStorage.getItem('token');
    const headers = { Authorization: `Bearer ${token}` };

    const urlMap = {
        AVIS        : `${BASE_URL}/admin/avis/${idContenu}`,
        JEU         : `${BASE_URL}/jeux/${idContenu}`,
        UTILISATEUR : `${BASE_URL}/admin/utilisateurs/${idContenu}`,
    };

    const url = urlMap[typeContenu];
    if (!url) throw new Error(`Suppression non supportée pour le type : ${typeContenu}`);

    const res = await fetch(url, { method: 'DELETE', headers });
    // 204 ou 404 sont acceptables (déjà supprimé)
    if (!res.ok && res.status !== 404) {
        let msg = `Erreur ${res.status}`;
        try { const body = await res.json(); msg = body.message || msg; } catch { /* ignore */ }
        throw new Error(msg);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Regroupement : (typeContenu, idContenu) → groupe
// ─────────────────────────────────────────────────────────────────────────────

function grouperReports(list) {
    const map = new Map();
    for (const r of list) {
        const key = `${r.typeContenu}__${r.idContenu}`;
        if (!map.has(key)) {
            map.set(key, {
                key,
                typeContenu  : r.typeContenu,
                idContenu    : r.idContenu,
                reports      : [],
                statut       : r.statut,
                premierDate  : r.date,
            });
        }
        const g = map.get(key);
        g.reports.push(r);

        // Statut du groupe = le plus prioritaire
        const prio = { EN_ATTENTE: 3, EN_COURS: 2, RESOLU: 1, REJETE: 0 };
        if ((prio[r.statut] ?? 0) > (prio[g.statut] ?? 0)) g.statut = r.statut;

        // Date la plus ancienne
        if (new Date(r.date) < new Date(g.premierDate)) g.premierDate = r.date;
    }

    // Tri : urgent d'abord, puis par date desc
    return [...map.values()].sort((a, b) => {
        const prio = { EN_ATTENTE: 3, EN_COURS: 2, RESOLU: 1, REJETE: 0 };
        const d = (prio[b.statut] ?? 0) - (prio[a.statut] ?? 0);
        return d !== 0 ? d : new Date(b.premierDate) - new Date(a.premierDate);
    });
}

// ─────────────────────────────────────────────────────────────────────────────
// Sous-composants
// ─────────────────────────────────────────────────────────────────────────────

function StatutBadge({ statut }) {
    const cfg = {
        EN_ATTENTE : { label: 'En attente', cls: 'text-yellow-400 bg-yellow-950 border-yellow-800' },
        EN_COURS   : { label: 'En cours',   cls: 'text-blue-400 bg-blue-950 border-blue-800' },
        RESOLU     : { label: 'Résolu',      cls: 'text-green-400 bg-green-950 border-green-800' },
        REJETE     : { label: 'Rejeté',     cls: 'text-gray-400 bg-gray-900 border-gray-700' },
    };
    const { label, cls } = cfg[statut] ?? cfg.EN_ATTENTE;
    return <span className={`text-xs font-medium px-2 py-0.5 rounded border ${cls}`}>{label}</span>;
}

// ── Modal résoudre / rejeter ──────────────────────────────────────────────────

function ActionModal({ typeContenu, idContenu, action, onClose, onConfirm }) {
    const [note, setNote]       = useState('');
    const [loading, setLoading] = useState(false);
    const isResolve = action === 'RESOLU';

    const handleConfirm = async () => {
        setLoading(true);
        try   { await onConfirm(note); onClose(); }
        catch { /* erreur gérée par parent, ne pas fermer */ }
        finally { setLoading(false); }
    };

    const { label } = TYPE_META[typeContenu] ?? { label: typeContenu };

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center px-4"
            style={{ background: 'rgba(0,0,0,0.82)' }}
            onClick={(e) => e.target === e.currentTarget && onClose()}
        >
            <div className="bg-secondary-black border border-gray-700 rounded-2xl p-6 w-full max-w-md shadow-2xl">
                {/* Titre modal */}
                <div className="flex items-center gap-3 mb-5">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center border ${
                        isResolve
                            ? 'bg-green-950 border-green-800'
                            : 'bg-gray-800 border-gray-700'
                    }`}>
                        {isResolve
                            ? <BadgeCheck className="w-5 h-5 text-green-400" />
                            : <XCircle   className="w-5 h-5 text-gray-400" />
                        }
                    </div>
                    <div>
                        <h3 className="text-white font-bold text-base">
                            {isResolve ? 'Résoudre le signalement' : 'Rejeter le signalement'}
                        </h3>
                        <p className="text-gray-500 text-xs">
                            {label} #{idContenu}
                        </p>
                    </div>
                </div>

                {/* Champ note */}
                <label className="block text-xs font-semibold text-gray-400 mb-1.5 uppercase tracking-wider">
                    Commentaire modérateur
                    {isResolve && <span className="text-primary-red ml-1">*</span>}
                    {!isResolve && <span className="text-gray-600 font-normal ml-1">(optionnel)</span>}
                </label>
                <textarea
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    rows={4}
                    autoFocus
                    placeholder={
                        isResolve
                            ? 'Décrivez l\'action prise : contenu retiré, utilisateur averti, contexte…'
                            : 'Raison du rejet (ex : signalement non fondé, contenu conforme…)'
                    }
                    className="w-full bg-accent-black border border-gray-700 rounded-xl
                               px-4 py-3 text-white text-sm placeholder-gray-600
                               resize-none focus:outline-none focus:border-primary-red
                               transition-colors mb-1"
                />
                {isResolve && (
                    <p className="text-xs text-gray-600 mb-5">
                        Ce commentaire sera enregistré et visible dans l'historique du signalement.
                    </p>
                )}
                {!isResolve && <div className="mb-5" />}

                {/* Boutons */}
                <div className="flex gap-3">
                    <button
                        onClick={onClose}
                        className="flex-1 py-2.5 rounded-xl border border-gray-700 text-gray-400
                                   text-sm font-medium hover:border-gray-500 hover:text-gray-300
                                   transition-colors"
                    >
                        Annuler
                    </button>
                    <button
                        onClick={handleConfirm}
                        disabled={loading || (isResolve && !note.trim())}
                        className={`flex-1 py-2.5 rounded-xl text-white text-sm font-semibold
                                    transition-all disabled:opacity-40 disabled:cursor-not-allowed
                                    flex items-center justify-center gap-2
                                    ${isResolve
                            ? 'bg-green-700 hover:bg-green-600'
                            : 'bg-gray-700 hover:bg-gray-600'
                        }`}
                    >
                        {loading
                            ? <Loader2 className="w-4 h-4 animate-spin" />
                            : isResolve
                                ? <><BadgeCheck className="w-4 h-4" />Confirmer résolu</>
                                : <><XCircle   className="w-4 h-4" />Rejeter</>
                        }
                    </button>
                </div>
            </div>
        </div>
    );
}

// ── Carte groupe de reports ───────────────────────────────────────────────────

function GroupeCard({ groupe, onTraiterGroupe, onSupprimerContenu, onSupprimerReports }) {
    const [expanded, setExpanded]         = useState(false);
    const [actionModal, setActionModal]   = useState(null); // null | 'RESOLU' | 'REJETE'
    const [loadingDelContent, setLoadingDelContent] = useState(false);
    const [loadingDelReports, setLoadingDelReports] = useState(false);

    const meta       = TYPE_META[groupe.typeContenu] ?? TYPE_META.NOTE;
    const TypeIcon   = meta.icon;
    const nbReports  = groupe.reports.length;
    const estTraite  = groupe.statut === 'RESOLU' || groupe.statut === 'REJETE';
    const canDelete  = groupe.typeContenu !== 'NOTE'; // Notes pas supprimables via admin

    // Données agrégées du groupe
    const raisonsUniques     = [...new Set(groupe.reports.map((r) => r.raison))];
    const signalantsUniques  = [...new Set(groupe.reports.map((r) => r.auteurPseudo))];
    const detailsNonVides    = groupe.reports.filter((r) => r.details?.trim());

    // Dernière note de modérateur (pour affichage après traitement)
    const dernierTraitement = groupe.reports
        .filter((r) => r.noteModerateur || r.moderateurPseudo)
        .sort((a, b) => new Date(b.date) - new Date(a.date))[0];

    // ── Actions ──

    const handleConfirmAction = async (note) => {
        await onTraiterGroupe(
            groupe.reports.map((r) => r.id),
            actionModal,
            note,
        );
    };

    const handleSupprimerContenu = async () => {
        setLoadingDelContent(true);
        try {
            await onSupprimerContenu(
                groupe.typeContenu,
                groupe.idContenu,
                groupe.reports.map((r) => r.id),
            );
        } finally {
            setLoadingDelContent(false);
        }
    };

    const handleSupprimerReports = async () => {
        setLoadingDelReports(true);
        try   { await onSupprimerReports(groupe.reports.map((r) => r.id)); }
        finally { setLoadingDelReports(false); }
    };

    return (
        <>
            {actionModal && (
                <ActionModal
                    typeContenu={groupe.typeContenu}
                    idContenu={groupe.idContenu}
                    action={actionModal}
                    onClose={() => setActionModal(null)}
                    onConfirm={handleConfirmAction}
                />
            )}

            <div className={`rounded-2xl border overflow-hidden transition-all duration-200 ${
                groupe.statut === 'EN_ATTENTE' ? 'border-yellow-900/60 bg-secondary-black' :
                    groupe.statut === 'EN_COURS'   ? 'border-blue-900/60 bg-secondary-black'   :
                        groupe.statut === 'RESOLU'     ? 'border-green-900/40 bg-secondary-black'  :
                            'border-gray-800 bg-secondary-black'
            }`}>

                {/* ── En-tête ── */}
                <div className="p-4 sm:p-5">
                    <div className="flex items-start gap-3">

                        {/* Icône type */}
                        <div className={`w-10 h-10 rounded-xl flex items-center justify-center
                                         flex-shrink-0 border ${meta.bg}`}>
                            <TypeIcon className={`w-4.5 h-4.5 ${meta.color}`} />
                        </div>

                        {/* Infos principales */}
                        <div className="flex-1 min-w-0">
                            <div className="flex flex-wrap items-center gap-2 mb-1">
                                <span className="text-white font-bold text-sm">
                                    {meta.label} #{groupe.idContenu}
                                </span>
                                <StatutBadge statut={groupe.statut} />
                                {nbReports > 1 && (
                                    <span className="flex items-center gap-1 text-xs font-semibold
                                                     text-red-400 bg-red-950/80 border border-red-900
                                                     px-2 py-0.5 rounded-full">
                                        <Layers className="w-3 h-3" />
                                        {nbReports} signalements
                                    </span>
                                )}
                            </div>

                            {/* Raisons */}
                            <div className="flex flex-wrap gap-1.5 mb-2">
                                {raisonsUniques.map((r) => (
                                    <span key={r}
                                          className="text-xs text-gray-500 bg-gray-800/70
                                                     px-2 py-0.5 rounded-md">
                                        {RAISONS_LABELS[r] ?? r}
                                    </span>
                                ))}
                            </div>

                            {/* Signalants + date */}
                            <p className="text-xs text-gray-600 leading-relaxed">
                                Signalé par{' '}
                                <span className="text-gray-400">
                                    {signalantsUniques.slice(0, 3).join(', ')}
                                    {signalantsUniques.length > 3 && (
                                        <span className="text-gray-600">
                                            {' '}+{signalantsUniques.length - 3} autre{signalantsUniques.length - 3 > 1 ? 's' : ''}
                                        </span>
                                    )}
                                </span>
                                {' · '}
                                {new Date(groupe.premierDate).toLocaleDateString('fr-FR', {
                                    day: '2-digit', month: 'short', year: 'numeric',
                                })}
                            </p>
                        </div>

                        {/* Bouton expand */}
                        <button
                            onClick={() => setExpanded((v) => !v)}
                            className="text-gray-600 hover:text-gray-300 transition-colors p-1 flex-shrink-0"
                            title={expanded ? 'Réduire' : 'Voir le détail'}
                        >
                            {expanded
                                ? <ChevronUp   className="w-4 h-4" />
                                : <ChevronDown className="w-4 h-4" />
                            }
                        </button>
                    </div>

                    {/* Note du modérateur (si traité) */}
                    {estTraite && dernierTraitement?.noteModerateur && (
                        <div className={`mt-4 p-3 rounded-xl border ${
                            groupe.statut === 'RESOLU'
                                ? 'bg-green-950/30 border-green-900/60'
                                : 'bg-gray-800/50 border-gray-700'
                        }`}>
                            <p className="text-xs font-semibold mb-1 ${
                                groupe.statut === 'RESOLU' ? 'text-green-500' : 'text-gray-500'
                            }">
                                {groupe.statut === 'RESOLU' ? '✓ Résolu' : '✗ Rejeté'}
                                {dernierTraitement.moderateurPseudo && (
                                    <span className="font-normal text-gray-500">
                                        {' '}par{' '}
                                        <span className="text-gray-300">
                                            {dernierTraitement.moderateurPseudo}
                                        </span>
                                    </span>
                                )}
                            </p>
                            <p className="text-sm text-gray-300 leading-relaxed">
                                {dernierTraitement.noteModerateur}
                            </p>
                        </div>
                    )}

                    {/* ── Barre d'actions ── */}
                    <div className="flex flex-wrap items-center gap-2 mt-4 pt-4 border-t border-gray-800/70">
                        {!estTraite && (
                            <>
                                {/* Résoudre */}
                                <button
                                    onClick={() => setActionModal('RESOLU')}
                                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg
                                               text-xs font-semibold bg-green-950 border border-green-800
                                               text-green-400 hover:bg-green-900 transition-colors"
                                >
                                    <BadgeCheck className="w-3.5 h-3.5" />
                                    Résoudre
                                </button>

                                {/* Rejeter */}
                                <button
                                    onClick={() => setActionModal('REJETE')}
                                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg
                                               text-xs font-semibold border border-gray-700 text-gray-400
                                               hover:border-gray-500 hover:text-gray-300 transition-colors"
                                >
                                    <XCircle className="w-3.5 h-3.5" />
                                    Rejeter
                                </button>

                                {/* Supprimer le contenu */}
                                {canDelete && (
                                    <button
                                        onClick={handleSupprimerContenu}
                                        disabled={loadingDelContent}
                                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg
                                                   text-xs font-semibold bg-red-950 border border-red-900
                                                   text-red-400 hover:bg-red-900 transition-colors
                                                   disabled:opacity-50"
                                        title={`Supprimer ce ${meta.label.toLowerCase()} et marquer résolu`}
                                    >
                                        {loadingDelContent
                                            ? <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                            : <Trash2 className="w-3.5 h-3.5" />
                                        }
                                        Supprimer le {meta.label.toLowerCase()}
                                    </button>
                                )}
                            </>
                        )}

                        {/* Supprimer les reports (toujours visible) */}
                        <button
                            onClick={handleSupprimerReports}
                            disabled={loadingDelReports}
                            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs
                                       font-medium border border-gray-700/60 text-gray-600
                                       hover:border-gray-600 hover:text-gray-400 transition-colors
                                       disabled:opacity-50 ml-auto"
                            title="Supprimer tous les reports de ce groupe (sans toucher au contenu)"
                        >
                            {loadingDelReports
                                ? <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                : <Trash2 className="w-3.5 h-3.5" />
                            }
                            Supprimer les reports
                        </button>
                    </div>
                </div>

                {/* ── Détail expandable ── */}
                {expanded && (
                    <div className="border-t border-gray-800/60 bg-black/20 px-4 sm:px-5 py-4">
                        <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">
                            {nbReports} signalement{nbReports > 1 ? 's' : ''} individuels
                        </p>
                        <div className="space-y-2">
                            {groupe.reports.map((r) => (
                                <div key={r.id}
                                     className="bg-secondary-black border border-gray-800 rounded-xl p-3">
                                    <div className="flex items-start justify-between gap-2 mb-1">
                                        <div className="flex flex-wrap items-center gap-2">
                                            <span className="text-white text-xs font-semibold">
                                                {r.auteurPseudo}
                                            </span>
                                            <span className="text-xs text-gray-500 bg-gray-800
                                                             px-2 py-0.5 rounded">
                                                {RAISONS_LABELS[r.raison] ?? r.raison}
                                            </span>
                                            <StatutBadge statut={r.statut} />
                                        </div>
                                        <span className="text-xs text-gray-600 flex-shrink-0">
                                            {new Date(r.date).toLocaleDateString('fr-FR', {
                                                day: '2-digit', month: 'short',
                                                hour: '2-digit', minute: '2-digit',
                                            })}
                                        </span>
                                    </div>
                                    {r.details && (
                                        <p className="text-xs text-gray-500 bg-gray-800/50
                                                      rounded-lg px-3 py-2 leading-relaxed mt-2">
                                            "{r.details}"
                                        </p>
                                    )}
                                    {r.noteModerateur && (
                                        <p className="text-xs text-green-500 mt-2 flex items-center gap-1">
                                            <CheckCircle className="w-3 h-3 flex-shrink-0" />
                                            {r.noteModerateur}
                                        </p>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </>
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// Page principale
// ─────────────────────────────────────────────────────────────────────────────

const TABS = [
    {
        id      : 'pending',
        label   : 'En attente',
        icon    : Clock,
        statuts : ['EN_ATTENTE', 'EN_COURS'],
        empty   : 'Aucun signalement en attente — tout est calme !',
    },
    {
        id      : 'accepted',
        label   : 'Acceptés',
        icon    : BadgeCheck,
        statuts : ['RESOLU'],
        empty   : 'Aucun signalement résolu pour l\'instant.',
    },
    {
        id      : 'rejected',
        label   : 'Rejetés',
        icon    : XCircle,
        statuts : ['REJETE'],
        empty   : 'Aucun signalement rejeté.',
    },
    {
        id      : 'all',
        label   : 'Tous',
        icon    : Layers,
        statuts : null,
        empty   : 'Aucun signalement.',
    },
];

const TYPE_FILTERS = [
    { value: '',            label: 'Tous les types' },
    { value: 'AVIS',        label: 'Avis' },
    { value: 'JEU',         label: 'Jeux' },
    { value: 'UTILISATEUR', label: 'Utilisateurs' },
];

export const AdminReports = () => {
    const { isAuth }   = useAuth();
    const navigate     = useNavigate();
    const role         = getRoleFromToken();
    const isAdmin      = isAuth && ADMIN_ROLES.includes(role);

    const [reportList, setReportList] = useState([]);
    const [loading, setLoading]       = useState(true);
    const [activeTab, setActiveTab]   = useState('pending');
    const [filtreType, setFiltreType] = useState('');
    const [feedback, setFeedback]     = useState('');
    const [error, setError]           = useState('');

    const toast = (msg, isErr = false) => {
        isErr ? setError(msg) : setFeedback(msg);
        setTimeout(() => { setFeedback(''); setError(''); }, 4000);
    };

    useEffect(() => {
        if (!isAdmin) { navigate('/'); return; }
        loadReports();
    }, [isAdmin]);

    const loadReports = async () => {
        setLoading(true);
        try {
            const data = await reportsApi.listerTous();
            setReportList(data ?? []);
        } catch {
            setReportList([]);
            toast('Impossible de charger les signalements', true);
        } finally {
            setLoading(false);
        }
    };

    // ── Handlers passés aux cartes ────────────────────────────────────────────

    /**
     * Marque tous les reports d'un groupe avec le statut donné + note modérateur.
     */
    const handleTraiterGroupe = async (ids, statut, note) => {
        try {
            const updated = await Promise.all(
                ids.map((id) => reportsApi.traiter(id, { statut, note }))
            );
            setReportList((prev) =>
                prev.map((r) => updated.find((u) => u.id === r.id) ?? r)
            );
            toast(
                statut === 'RESOLU'
                    ? '✓ Signalement(s) marqué(s) comme résolu(s)'
                    : 'Signalement(s) rejeté(s)'
            );
        } catch (err) {
            toast(err.message ?? 'Erreur lors du traitement', true);
            throw err;
        }
    };

    /**
     * Supprime le contenu (jeu / avis / utilisateur) via l'API admin,
     * puis marque automatiquement tous les reports du groupe comme RESOLU
     * avec une note générée.
     */
    const handleSupprimerContenu = async (typeContenu, idContenu, reportIds) => {
        try {
            await deleteContent(typeContenu, idContenu);

            const noteAuto = `${TYPE_META[typeContenu]?.label ?? typeContenu} #${idContenu} supprimé par modération.`;
            await Promise.all(
                reportIds.map((id) =>
                    reportsApi.traiter(id, { statut: 'RESOLU', note: noteAuto })
                )
            );

            setReportList((prev) =>
                prev.map((r) =>
                    reportIds.includes(r.id)
                        ? { ...r, statut: 'RESOLU', noteModerateur: noteAuto }
                        : r
                )
            );
            toast(`✓ ${TYPE_META[typeContenu]?.label ?? typeContenu} supprimé — reports résolus`);
        } catch (err) {
            toast(err.message ?? 'Erreur lors de la suppression du contenu', true);
            throw err;
        }
    };

    /**
     * Supprime uniquement les reports (pas le contenu).
     */
    const handleSupprimerReports = async (ids) => {
        try {
            await Promise.all(ids.map((id) => reportsApi.supprimer(id)));
            setReportList((prev) => prev.filter((r) => !ids.includes(r.id)));
            toast('Reports supprimés');
        } catch (err) {
            toast(err.message ?? 'Erreur lors de la suppression des reports', true);
        }
    };

    // ── Filtrage + regroupement ───────────────────────────────────────────────

    const tabConfig = TABS.find((t) => t.id === activeTab) ?? TABS[0];

    const filtered = reportList.filter((r) => {
        const matchTab  = !tabConfig.statuts || tabConfig.statuts.includes(r.statut);
        const matchType = !filtreType || r.typeContenu === filtreType;
        return matchTab && matchType;
    });

    const groupes = grouperReports(filtered);

    // Compteurs par onglet (en groupes, pas en reports bruts)
    const tabCounts = {
        pending  : grouperReports(reportList.filter((r) => ['EN_ATTENTE', 'EN_COURS'].includes(r.statut))).length,
        accepted : grouperReports(reportList.filter((r) => r.statut === 'RESOLU')).length,
        rejected : grouperReports(reportList.filter((r) => r.statut === 'REJETE')).length,
        all      : grouperReports(reportList).length,
    };

    if (!isAdmin) return null;

    return (
        <div className="min-h-screen bg-primary-black">

            {/* ── Toasts ── */}
            {feedback && (
                <div className="fixed bottom-6 right-6 z-50 px-4 py-3 bg-green-900 border
                                border-green-700 text-green-300 text-sm font-medium rounded-xl
                                shadow-2xl flex items-center gap-2">
                    <CheckCircle className="w-4 h-4 flex-shrink-0" />
                    {feedback}
                </div>
            )}
            {error && (
                <div className="fixed bottom-6 right-6 z-50 px-4 py-3 bg-red-950 border
                                border-red-800 text-red-300 text-sm font-medium rounded-xl
                                shadow-2xl flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 flex-shrink-0" />
                    {error}
                </div>
            )}

            <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

                {/* ── En-tête ── */}
                <div className="flex items-center gap-4 mb-8">
                    <div className="w-11 h-11 bg-red-950 border border-red-900 rounded-xl
                                    flex items-center justify-center flex-shrink-0">
                        <Flag className="w-5 h-5 text-primary-red" />
                    </div>
                    <div>
                        <h1 className="text-2xl font-black text-white">Signalements</h1>
                        <p className="text-gray-500 text-sm mt-0.5">
                            {tabCounts.pending > 0
                                ? `${tabCounts.pending} groupe${tabCounts.pending > 1 ? 's' : ''} en attente de traitement`
                                : 'Aucun signalement en attente'
                            }
                        </p>
                    </div>
                </div>

                {/* ── Onglets ── */}
                <div className="flex gap-1 p-1 bg-secondary-black border border-gray-800 rounded-xl mb-5">
                    {TABS.map(({ id, label, icon: Icon }) => (
                        <button
                            key={id}
                            onClick={() => { setActiveTab(id); setFiltreType(''); }}
                            className={`flex-1 flex items-center justify-center gap-1.5 py-2.5
                                        rounded-lg text-sm font-medium transition-all
                                        ${activeTab === id
                                ? 'bg-primary-red text-white shadow-sm'
                                : 'text-gray-400 hover:text-white'
                            }`}
                        >
                            <Icon className="w-3.5 h-3.5 flex-shrink-0" />
                            <span className="hidden sm:inline">{label}</span>
                            {tabCounts[id] > 0 && (
                                <span className={`text-xs font-bold ${
                                    activeTab === id ? 'text-red-200' : 'text-gray-500'
                                }`}>
                                    {tabCounts[id]}
                                </span>
                            )}
                        </button>
                    ))}
                </div>

                {/* ── Filtres type ── */}
                <div className="flex flex-wrap gap-2 mb-6">
                    {TYPE_FILTERS.map(({ value, label }) => (
                        <button
                            key={value}
                            onClick={() => setFiltreType(value)}
                            className={`px-3 py-1.5 rounded-lg text-xs font-medium
                                        transition-all border
                                        ${filtreType === value
                                ? 'bg-primary-red border-primary-red text-white'
                                : 'border-gray-700 text-gray-400 hover:border-gray-500 hover:text-gray-300'
                            }`}
                        >
                            {label}
                        </button>
                    ))}
                </div>

                {/* ── Contenu ── */}
                {loading ? (
                    <div className="flex items-center justify-center py-24">
                        <Loader2 className="w-8 h-8 text-primary-red animate-spin" />
                    </div>
                ) : groupes.length === 0 ? (
                    <div className="text-center py-24 border border-gray-800 rounded-2xl
                                    bg-secondary-black">
                        <div className="w-14 h-14 bg-gray-800 border border-gray-700 rounded-2xl
                                        flex items-center justify-center mx-auto mb-4">
                            <Flag className="w-6 h-6 text-gray-600" />
                        </div>
                        <p className="text-gray-300 font-semibold mb-1">Aucun signalement</p>
                        <p className="text-gray-600 text-sm">{tabConfig.empty}</p>
                    </div>
                ) : (
                    <div className="space-y-3">
                        {groupes.map((groupe) => (
                            <GroupeCard
                                key={groupe.key}
                                groupe={groupe}
                                onTraiterGroupe={handleTraiterGroupe}
                                onSupprimerContenu={handleSupprimerContenu}
                                onSupprimerReports={handleSupprimerReports}
                            />
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};