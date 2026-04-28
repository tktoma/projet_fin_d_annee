import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
    BookMarked, Star, Loader2, Trash2, RefreshCw, Library,
} from 'lucide-react';
import { bibliotheque } from '../api.js';
import { useAuth } from '../context/AuthContext.jsx';

const STATUTS = [
    { value: 'ALL', label: 'Tous' },
    { value: 'JOUER', label: 'En cours', color: 'text-yellow-400', bg: 'bg-yellow-950 border-yellow-700' },
    { value: 'A_JOUER', label: 'À jouer', color: 'text-blue-400', bg: 'bg-blue-950 border-blue-700' },
    { value: 'FINIT', label: 'Terminé', color: 'text-green-400', bg: 'bg-green-950 border-green-700' },
    { value: 'ABANDONNER', label: 'Abandonné', color: 'text-gray-400', bg: 'bg-gray-900 border-gray-700' },
];

function StatutBadge({ statut }) {
    const s = STATUTS.find((x) => x.value === statut);
    if (!s || !s.color) return null;
    return (
        <span className={`text-xs font-medium px-2 py-0.5 rounded border ${s.bg} ${s.color}`}>
      {s.label}
    </span>
    );
}

function GameRow({ entry, onDelete, onChangeStatut }) {
    const [loadingStatut, setLoadingStatut] = useState(false);
    const [loadingDelete, setLoadingDelete] = useState(false);
    const [showMenu, setShowMenu] = useState(false);

    const handleChangeStatut = async (statut) => {
        setLoadingStatut(true);
        setShowMenu(false);
        try {
            await onChangeStatut(entry.jeuId, statut);
        } finally {
            setLoadingStatut(false);
        }
    };

    const handleDelete = async () => {
        setLoadingDelete(true);
        try {
            await onDelete(entry.jeuId);
        } finally {
            setLoadingDelete(false);
        }
    };

    return (
        <div className="flex items-center gap-4 p-3 sm:p-4 bg-secondary-black border border-gray-800
                    rounded-xl hover:border-gray-700 transition-colors group">
            {/* Cover */}
            <div className="w-14 h-20 sm:w-16 sm:h-24 flex-shrink-0 rounded-lg overflow-hidden bg-accent-black">
                {entry.jeuCoverUrl ? (
                    <img src={entry.jeuCoverUrl} alt={entry.jeuTitre} className="w-full h-full object-cover" />
                ) : (
                    <div className="w-full h-full flex items-center justify-center text-gray-700 text-2xl">🎮</div>
                )}
            </div>

            {/* Info */}
            <div className="flex-1 min-w-0">
                <h3 className="text-white font-semibold text-sm sm:text-base truncate mb-1">
                    {entry.jeuTitre}
                </h3>
                <StatutBadge statut={entry.statut} />
                <p className="text-gray-600 text-xs mt-1.5">
                    Ajouté le {new Date(entry.date).toLocaleDateString('fr-FR')}
                </p>
            </div>

            {/* Actions */}
            <div className="flex items-center gap-2 flex-shrink-0">
                {/* Change statut */}
                <div className="relative">
                    <button
                        onClick={() => setShowMenu(!showMenu)}
                        disabled={loadingStatut}
                        className="p-2 rounded-lg border border-gray-700 text-gray-400
                       hover:border-gray-500 hover:text-gray-300 transition-colors
                       disabled:opacity-40"
                        title="Changer le statut"
                    >
                        {loadingStatut ? (
                            <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                            <RefreshCw className="w-4 h-4" />
                        )}
                    </button>
                    {showMenu && (
                        <div className="absolute right-0 bottom-full mb-1 z-10 w-36
                            bg-secondary-black border border-gray-700 rounded-xl overflow-hidden shadow-xl">
                            {STATUTS.filter((s) => s.value !== 'ALL' && s.value !== entry.statut).map(({ value, label, color }) => (
                                <button
                                    key={value}
                                    onClick={() => handleChangeStatut(value)}
                                    className={`w-full px-3 py-2 text-left text-xs font-medium hover:bg-gray-800 transition-colors ${color}`}
                                >
                                    {label}
                                </button>
                            ))}
                        </div>
                    )}
                </div>

                {/* Delete */}
                <button
                    onClick={handleDelete}
                    disabled={loadingDelete}
                    className="p-2 rounded-lg border border-gray-700 text-gray-600
                     hover:border-red-800 hover:text-red-400 transition-colors
                     disabled:opacity-40"
                    title="Supprimer"
                >
                    {loadingDelete ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                        <Trash2 className="w-4 h-4" />
                    )}
                </button>
            </div>
        </div>
    );
}

export const MaBibliotheque = () => {
    const { isAuth } = useAuth();
    const navigate = useNavigate();
    const [entries, setEntries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activeStatut, setActiveStatut] = useState('ALL');

    useEffect(() => {
        if (!isAuth) {
            navigate('/connexion');
            return;
        }
        fetchBibliotheque();
    }, [isAuth]);

    const fetchBibliotheque = async () => {
        setLoading(true);
        try {
            const data = await bibliotheque.maBibliotheque();
            setEntries(data || []);
        } catch {
            setEntries([]);
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (jeuId) => {
        await bibliotheque.supprimer(jeuId);
        setEntries((prev) => prev.filter((e) => e.jeuId !== jeuId));
    };

    const handleChangeStatut = async (jeuId, statut) => {
        await bibliotheque.changerStatut(jeuId, statut);
        setEntries((prev) =>
            prev.map((e) => (e.jeuId === jeuId ? { ...e, statut } : e))
        );
    };

    const filtered = activeStatut === 'ALL'
        ? entries
        : entries.filter((e) => e.statut === activeStatut);

    // Stats
    const stats = STATUTS.filter((s) => s.value !== 'ALL').map((s) => ({
        ...s,
        count: entries.filter((e) => e.statut === s.value).length,
    }));

    if (loading) {
        return (
            <div className="min-h-screen bg-primary-black flex items-center justify-center">
                <Loader2 className="w-8 h-8 text-primary-red animate-spin" />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-primary-black">
            <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

                {/* Header */}
                <div className="mb-8">
                    <div className="flex items-center gap-3 mb-1">
                        <BookMarked className="w-6 h-6 text-primary-red" />
                        <h1 className="text-3xl font-black text-white">Ma Bibliothèque</h1>
                    </div>
                    <p className="text-gray-500 text-sm">
                        {entries.length} jeu{entries.length !== 1 ? 'x' : ''} dans votre collection
                    </p>
                </div>

                {entries.length === 0 ? (
                    /* État vide */
                    <div className="text-center py-20 border border-gray-800 rounded-2xl bg-secondary-black">
                        <Library className="w-12 h-12 text-gray-700 mx-auto mb-4" />
                        <p className="text-gray-400 font-semibold mb-2">Votre bibliothèque est vide</p>
                        <p className="text-gray-600 text-sm mb-6">
                            Parcourez le catalogue et ajoutez vos jeux favoris
                        </p>
                        <Link
                            to="/bibliotheque"
                            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg
                         bg-primary-red hover:bg-secondary-red text-white text-sm font-semibold
                         transition-colors"
                        >
                            <Library className="w-4 h-4" />
                            Explorer le catalogue
                        </Link>
                    </div>
                ) : (
                    <>
                        {/* Stats */}
                        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
                            {stats.map(({ label, count, color, bg }) => (
                                <div key={label} className={`p-3 rounded-xl border ${bg} text-center`}>
                                    <p className={`text-2xl font-black ${color}`}>{count}</p>
                                    <p className="text-xs text-gray-500 mt-0.5">{label}</p>
                                </div>
                            ))}
                        </div>

                        {/* Filtres */}
                        <div className="flex flex-wrap gap-2 mb-5">
                            {STATUTS.map(({ value, label }) => (
                                <button
                                    key={value}
                                    onClick={() => setActiveStatut(value)}
                                    className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all
                    ${activeStatut === value
                                        ? 'bg-primary-red text-white'
                                        : 'border border-gray-700 text-gray-400 hover:border-gray-500 hover:text-gray-300'
                                    }`}
                                >
                                    {label}
                                    {value !== 'ALL' && (
                                        <span className={`ml-1.5 text-xs ${activeStatut === value ? 'text-red-200' : 'text-gray-600'}`}>
                      {entries.filter((e) => e.statut === value).length}
                    </span>
                                    )}
                                </button>
                            ))}
                        </div>

                        {/* Liste */}
                        {filtered.length === 0 ? (
                            <div className="text-center py-12 text-gray-600">
                                Aucun jeu dans cette catégorie
                            </div>
                        ) : (
                            <div className="space-y-2">
                                {filtered.map((entry) => (
                                    <GameRow
                                        key={entry.id}
                                        entry={entry}
                                        onDelete={handleDelete}
                                        onChangeStatut={handleChangeStatut}
                                    />
                                ))}
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
};