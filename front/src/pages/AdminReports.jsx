import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Flag, Trash2, Loader2, Filter, ChevronDown,
    CheckCircle, AlertCircle, Clock, Shield
} from 'lucide-react';
import { reports } from '../api.js';
import { useAuth } from '../context/AuthContext.jsx';

const ADMIN_ROLES = ['ADMIN', 'SUPERADMIN'];

function getRoleFromToken() {
    const token = localStorage.getItem('token');
    if (!token) return null;
    try { return JSON.parse(atob(token.split('.')[1])).role || null; }
    catch { return null; }
}

const STATUTS = [
    { value: '',           label: 'Tous les statuts' },
    { value: 'EN_ATTENTE', label: 'En attente',  color: 'text-yellow-400 bg-yellow-950 border-yellow-800' },
    { value: 'EN_COURS',   label: 'En cours',    color: 'text-blue-400 bg-blue-950 border-blue-800' },
    { value: 'RESOLU',     label: 'Résolu',      color: 'text-green-400 bg-green-950 border-green-800' },
    { value: 'REJETE',     label: 'Rejeté',      color: 'text-gray-400 bg-gray-900 border-gray-700' },
];

const TYPES = [
    { value: '',           label: 'Tous les types' },
    { value: 'AVIS',       label: 'Avis' },
    { value: 'NOTE',       label: 'Note' },
    { value: 'UTILISATEUR',label: 'Utilisateur' },
    { value: 'JEU',        label: 'Jeu' },
];

const RAISONS_LABELS = {
    CONTENU_INAPPROPRIE: 'Contenu inapproprié',
    SPAM:                'Spam',
    HARCELEMENT:         'Harcèlement',
    FAUSSE_INFORMATION:  'Fausse information',
    AUTRE:               'Autre',
};

function StatutBadge({ statut }) {
    const s = STATUTS.find(x => x.value === statut);
    if (!s?.color) return null;
    return (
        <span className={`text-xs font-medium px-2 py-0.5 rounded border ${s.color}`}>
            {s.label}
        </span>
    );
}

function ReportCard({ report, onDelete, onTraiter }) {
    const [deleting, setDeleting]   = useState(false);
    const [resolving, setResolving] = useState(false);
    const [note, setNote]           = useState('');
    const [showNote, setShowNote]   = useState(false);

    const handleDelete = async () => {
        setDeleting(true);
        try { await onDelete(report.id); }
        finally { setDeleting(false); }
    };

    const handleResoudre = async (statut) => {
        setResolving(true);
        try {
            await onTraiter(report.id, { statut, note });
            setShowNote(false);
        } finally { setResolving(false); }
    };

    return (
        <div className="bg-secondary-black border border-gray-800 rounded-xl p-4">
            <div className="flex items-start justify-between gap-3 mb-3">
                <div className="flex flex-wrap items-center gap-2">
                    <StatutBadge statut={report.statut} />
                    <span className="text-xs px-2 py-0.5 rounded border text-purple-400 bg-purple-950 border-purple-800 font-medium">
                        {report.typeContenu}
                    </span>
                    <span className="text-xs text-gray-500">
                        {RAISONS_LABELS[report.raison] || report.raison}
                    </span>
                </div>
                <span className="text-xs text-gray-600 flex-shrink-0">
                    {new Date(report.date).toLocaleDateString('fr-FR', {
                        day: '2-digit', month: 'short', year: 'numeric',
                        hour: '2-digit', minute: '2-digit'
                    })}
                </span>
            </div>

            <div className="grid grid-cols-2 gap-2 mb-3 text-xs">
                <div className="bg-accent-black rounded-lg p-2">
                    <span className="text-gray-500 block mb-0.5">Signalé par</span>
                    <span className="text-white font-medium">{report.auteurPseudo}</span>
                </div>
                <div className="bg-accent-black rounded-lg p-2">
                    <span className="text-gray-500 block mb-0.5">Contenu #{report.idContenu}</span>
                    <span className="text-white font-medium">{report.typeContenu}</span>
                </div>
            </div>

            {report.details && (
                <p className="text-gray-400 text-xs bg-accent-black rounded-lg p-2 mb-3 leading-relaxed">
                    {report.details}
                </p>
            )}

            {report.moderateurPseudo && (
                <p className="text-xs text-gray-600 mb-3">
                    Traité par <span className="text-gray-400">{report.moderateurPseudo}</span>
                    {report.noteModerateur && ` — ${report.noteModerateur}`}
                </p>
            )}

            {showNote && (
                <div className="mb-3">
                    <input
                        type="text"
                        value={note}
                        onChange={e => setNote(e.target.value)}
                        placeholder="Note du modérateur (optionnel)"
                        className="w-full bg-accent-black border border-gray-700 rounded-lg px-3 py-2
                                   text-white text-xs placeholder-gray-600
                                   focus:outline-none focus:border-primary-red"
                    />
                </div>
            )}

            <div className="flex flex-wrap items-center gap-2">
                {report.statut === 'EN_ATTENTE' || report.statut === 'EN_COURS' ? (
                    <>
                        <button
                            onClick={() => setShowNote(!showNote)}
                            className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs
                                       border border-gray-700 text-gray-400
                                       hover:border-gray-500 hover:text-gray-300 transition-colors"
                        >
                            <CheckCircle className="w-3 h-3" />
                            Résoudre
                        </button>
                        {showNote && (
                            <>
                                <button
                                    onClick={() => handleResoudre('RESOLU')}
                                    disabled={resolving}
                                    className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs
                                               bg-green-950 border border-green-800 text-green-400
                                               hover:bg-green-900 transition-colors disabled:opacity-50"
                                >
                                    {resolving ? <Loader2 className="w-3 h-3 animate-spin" /> : <CheckCircle className="w-3 h-3" />}
                                    Confirmer résolu
                                </button>
                                <button
                                    onClick={() => handleResoudre('REJETE')}
                                    disabled={resolving}
                                    className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs
                                               border border-gray-700 text-gray-400
                                               hover:border-gray-500 hover:text-gray-300 transition-colors disabled:opacity-50"
                                >
                                    Rejeter
                                </button>
                            </>
                        )}
                    </>
                ) : null}

                <button
                    onClick={handleDelete}
                    disabled={deleting}
                    className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs
                               border border-gray-700 text-gray-600
                               hover:border-red-800 hover:text-red-400 transition-colors
                               disabled:opacity-50 ml-auto"
                >
                    {deleting
                        ? <Loader2 className="w-3 h-3 animate-spin" />
                        : <Trash2 className="w-3 h-3" />}
                    Supprimer
                </button>
            </div>
        </div>
    );
}

export const AdminReports = () => {
    const { isAuth }   = useAuth();
    const navigate     = useNavigate();
    const role         = getRoleFromToken();
    const isAdmin      = isAuth && ADMIN_ROLES.includes(role);

    const [reportList, setReportList] = useState([]);
    const [loading, setLoading]       = useState(true);
    const [filtreStatut, setFiltreStatut] = useState('');
    const [filtreType,   setFiltreType]   = useState('');
    const [feedback, setFeedback]         = useState('');
    const [error, setError]               = useState('');

    const toast = (msg, isErr = false) => {
        isErr ? setError(msg) : setFeedback(msg);
        setTimeout(() => { setFeedback(''); setError(''); }, 3000);
    };

    useEffect(() => {
        if (!isAdmin) { navigate('/'); return; }
        fetchReports();
    }, [isAdmin]);

    const fetchReports = async () => {
        setLoading(true);
        try {
            const data = await reports.listerTous();
            setReportList(data || []);
        } catch { setReportList([]); }
        finally { setLoading(false); }
    };

    const handleDelete = async (id) => {
        try {
            await reports.supprimer(id);
            setReportList(prev => prev.filter(r => r.id !== id));
            toast('Report supprimé');
        } catch (err) { toast(err.message || 'Erreur', true); }
    };

    const handleTraiter = async (id, data) => {
        try {
            const updated = await reports.traiter(id, data);
            setReportList(prev => prev.map(r => r.id === id ? updated : r));
            toast('Report mis à jour');
        } catch (err) { toast(err.message || 'Erreur', true); }
    };

    const filtered = reportList.filter(r => {
        if (filtreStatut && r.statut !== filtreStatut) return false;
        if (filtreType   && r.typeContenu !== filtreType) return false;
        return true;
    });

    const counts = {
        EN_ATTENTE: reportList.filter(r => r.statut === 'EN_ATTENTE').length,
        EN_COURS:   reportList.filter(r => r.statut === 'EN_COURS').length,
        RESOLU:     reportList.filter(r => r.statut === 'RESOLU').length,
        REJETE:     reportList.filter(r => r.statut === 'REJETE').length,
    };

    if (!isAdmin) return null;

    return (
        <div className="min-h-screen bg-primary-black">
            {feedback && (
                <div className="fixed bottom-6 right-6 z-50 px-4 py-3 bg-green-900 border border-green-700
                                text-green-300 text-sm font-medium rounded-xl shadow-xl flex items-center gap-2">
                    <CheckCircle className="w-4 h-4" />{feedback}
                </div>
            )}
            {error && (
                <div className="fixed bottom-6 right-6 z-50 px-4 py-3 bg-red-950 border border-red-800
                                text-red-300 text-sm font-medium rounded-xl shadow-xl flex items-center gap-2">
                    <AlertCircle className="w-4 h-4" />{error}
                </div>
            )}

            <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="flex items-center gap-3 mb-6">
                    <div className="w-9 h-9 bg-red-950 border border-red-900 rounded-lg flex items-center justify-center">
                        <Flag className="w-4 h-4 text-primary-red" />
                    </div>
                    <div>
                        <h1 className="text-2xl font-black text-white">Signalements</h1>
                        <p className="text-gray-500 text-sm">{reportList.length} report{reportList.length !== 1 ? 's' : ''} au total</p>
                    </div>
                </div>

                {/* Stats */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
                    {[
                        { key: 'EN_ATTENTE', label: 'En attente', color: 'text-yellow-400', bg: 'bg-yellow-950 border-yellow-800' },
                        { key: 'EN_COURS',   label: 'En cours',   color: 'text-blue-400',   bg: 'bg-blue-950 border-blue-800' },
                        { key: 'RESOLU',     label: 'Résolus',    color: 'text-green-400',  bg: 'bg-green-950 border-green-800' },
                        { key: 'REJETE',     label: 'Rejetés',    color: 'text-gray-400',   bg: 'bg-gray-900 border-gray-700' },
                    ].map(({ key, label, color, bg }) => (
                        <button
                            key={key}
                            onClick={() => setFiltreStatut(filtreStatut === key ? '' : key)}
                            className={`p-3 rounded-xl border text-center transition-all
                                ${filtreStatut === key ? `${bg} ring-1 ring-offset-1 ring-offset-black` : 'bg-secondary-black border-gray-800 hover:border-gray-600'}`}
                        >
                            <p className={`text-2xl font-black ${color}`}>{counts[key]}</p>
                            <p className="text-xs text-gray-500 mt-0.5">{label}</p>
                        </button>
                    ))}
                </div>

                {/* Filtres */}
                <div className="flex flex-wrap gap-2 mb-5">
                    {TYPES.slice(1).map(({ value, label }) => (
                        <button
                            key={value}
                            onClick={() => setFiltreType(filtreType === value ? '' : value)}
                            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all border
                                ${filtreType === value
                                ? 'bg-primary-red border-primary-red text-white'
                                : 'border-gray-700 text-gray-400 hover:border-gray-500 hover:text-gray-300'}`}
                        >
                            {label}
                        </button>
                    ))}
                    {(filtreStatut || filtreType) && (
                        <button
                            onClick={() => { setFiltreStatut(''); setFiltreType(''); }}
                            className="px-3 py-1.5 rounded-lg text-xs font-medium border border-gray-700
                                       text-gray-500 hover:text-gray-300 transition-colors"
                        >
                            Réinitialiser
                        </button>
                    )}
                </div>

                {loading ? (
                    <div className="flex items-center justify-center py-20">
                        <Loader2 className="w-8 h-8 text-primary-red animate-spin" />
                    </div>
                ) : filtered.length === 0 ? (
                    <div className="text-center py-20 border border-gray-800 rounded-2xl bg-secondary-black">
                        <Flag className="w-10 h-10 text-gray-700 mx-auto mb-3" />
                        <p className="text-gray-400 font-semibold">Aucun signalement</p>
                        <p className="text-gray-600 text-sm mt-1">
                            {filtreStatut || filtreType ? 'Essayez d\'autres filtres' : 'Tout est calme !'}
                        </p>
                    </div>
                ) : (
                    <div className="space-y-3">
                        {filtered.map(r => (
                            <ReportCard
                                key={r.id}
                                report={r}
                                onDelete={handleDelete}
                                onTraiter={handleTraiter}
                            />
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};