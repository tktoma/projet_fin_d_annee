import { useState, useEffect, useCallback } from 'react';
import { Search, Filter, Star, SlidersHorizontal, ChevronLeft, ChevronRight, Plus, Loader2 } from 'lucide-react';
import { jeux, bibliotheque } from '../api.js';
import { useAuth } from '../context/AuthContext.jsx';

const STATUTS = [
    { value: 'JOUER', label: 'En cours', color: 'text-yellow-400 border-yellow-600 bg-yellow-950' },
    { value: 'A_JOUER', label: 'À jouer', color: 'text-blue-400 border-blue-600 bg-blue-950' },
    { value: 'FINIT', label: 'Terminé', color: 'text-green-400 border-green-600 bg-green-950' },
    { value: 'ABANDONNER', label: 'Abandonné', color: 'text-gray-400 border-gray-600 bg-gray-900' },
];

function GameCard({ jeu, isAuth, onAddToBibliotheque }) {
    const [showMenu, setShowMenu] = useState(false);
    const [added, setAdded] = useState(false);

    const handleAdd = async (statut) => {
        try {
            await onAddToBibliotheque(jeu.id, statut);
            setAdded(true);
            setShowMenu(false);
        } catch {
            setShowMenu(false);
        }
    };

    const coverUrl = jeu.coverUrl || null;
    const note = jeu.noteMoyenne > 0 ? jeu.noteMoyenne.toFixed(1) : null;

    return (
        <div className="group relative bg-secondary-black border border-gray-800 rounded-xl overflow-hidden
                    hover:border-gray-600 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl">
            {/* Cover */}
            <div className="relative aspect-[3/4] bg-accent-black overflow-hidden">
                {coverUrl ? (
                    <img
                        src={coverUrl}
                        alt={jeu.titre}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    />
                ) : (
                    <div className="w-full h-full flex items-center justify-center text-gray-700">
                        <span className="text-4xl">🎮</span>
                    </div>
                )}
                {/* Note badge */}
                {note && (
                    <div className="absolute top-2 right-2 flex items-center gap-1 px-2 py-1
                          bg-black/70 backdrop-blur-sm rounded-md">
                        <Star className="w-3 h-3 text-yellow-400 fill-yellow-400" />
                        <span className="text-white text-xs font-semibold">{note}</span>
                    </div>
                )}
            </div>

            {/* Info */}
            <div className="p-3">
                <h3 className="text-white font-semibold text-sm leading-tight mb-1 line-clamp-2">
                    {jeu.titre}
                </h3>
                <div className="flex flex-wrap gap-1 mb-2">
                    {jeu.genre && (
                        <span className="text-xs text-gray-500 bg-gray-800 px-2 py-0.5 rounded">
              {jeu.genre}
            </span>
                    )}
                    {jeu.plateforme && (
                        <span className="text-xs text-gray-500 bg-gray-800 px-2 py-0.5 rounded">
              {jeu.plateforme}
            </span>
                    )}
                </div>

                {/* Add to library */}
                {isAuth && (
                    <div className="relative">
                        {added ? (
                            <div className="w-full py-1.5 text-xs font-medium text-green-400 text-center">
                                ✓ Ajouté
                            </div>
                        ) : (
                            <>
                                <button
                                    onClick={() => setShowMenu(!showMenu)}
                                    className="w-full flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-lg
                             text-xs font-medium border border-gray-700 text-gray-400
                             hover:border-primary-red hover:text-primary-red transition-colors"
                                >
                                    <Plus className="w-3 h-3" />
                                    Ajouter
                                </button>
                                {showMenu && (
                                    <div className="absolute bottom-full left-0 right-0 mb-1 z-10
                                  bg-secondary-black border border-gray-700 rounded-lg overflow-hidden shadow-xl">
                                        {STATUTS.map(({ value, label, color }) => (
                                            <button
                                                key={value}
                                                onClick={() => handleAdd(value)}
                                                className={`w-full px-3 py-2 text-left text-xs font-medium transition-colors
                                    hover:bg-gray-800 ${color.split(' ')[0]}`}
                                            >
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
    );
}

export const Bibliotheque = () => {
    const { isAuth } = useAuth();
    const [gameList, setGameList] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filters, setFilters] = useState({ titre: '', genre: '', plateforme: '', noteMin: '' });
    const [pending, setPending] = useState(filters);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [showFilters, setShowFilters] = useState(false);
    const [feedback, setFeedback] = useState('');

    const fetchJeux = useCallback(async (f, p) => {
        setLoading(true);
        try {
            const params = { ...f, page: p, size: 20, sort: 'titre' };
            Object.keys(params).forEach((k) => params[k] === '' && delete params[k]);
            const data = await jeux.lister(params);
            setGameList(data.content || []);
            setTotalPages(data.totalPages || 0);
            setTotalElements(data.totalElements || 0);
        } catch {
            setGameList([]);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchJeux(filters, page);
    }, [filters, page, fetchJeux]);

    const handleSearch = (e) => {
        e.preventDefault();
        setFilters(pending);
        setPage(0);
    };

    const handleAddToBibliotheque = async (jeuId, statut) => {
        await bibliotheque.ajouter(jeuId, statut);
        setFeedback('Jeu ajouté à votre bibliothèque !');
        setTimeout(() => setFeedback(''), 2500);
    };

    return (
        <div className="min-h-screen bg-primary-black">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

                {/* Header */}
                <div className="mb-6">
                    <h1 className="text-3xl font-black text-white mb-1">Catalogue</h1>
                    <p className="text-gray-500 text-sm">
                        {totalElements > 0 ? `${totalElements} jeux disponibles` : 'Explorez notre collection'}
                    </p>
                </div>

                {/* Feedback toast */}
                {feedback && (
                    <div className="fixed bottom-6 right-6 z-50 px-4 py-3 bg-green-900 border border-green-700
                          text-green-300 text-sm font-medium rounded-xl shadow-xl">
                        {feedback}
                    </div>
                )}

                {/* Search & filters */}
                <form onSubmit={handleSearch} className="mb-6">
                    <div className="flex gap-2">
                        <div className="relative flex-1">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                            <input
                                type="text"
                                value={pending.titre}
                                onChange={(e) => setPending({ ...pending, titre: e.target.value })}
                                placeholder="Rechercher un jeu…"
                                className="w-full bg-secondary-black border border-gray-700 rounded-lg
                           pl-10 pr-4 py-2.5 text-white text-sm placeholder-gray-600
                           focus:outline-none focus:border-primary-red focus:ring-1 focus:ring-primary-red"
                            />
                        </div>
                        <button
                            type="submit"
                            className="px-4 py-2.5 bg-primary-red hover:bg-secondary-red text-white text-sm
                         font-medium rounded-lg transition-colors"
                        >
                            Chercher
                        </button>
                        <button
                            type="button"
                            onClick={() => setShowFilters(!showFilters)}
                            className={`px-3 py-2.5 rounded-lg border text-sm font-medium transition-colors flex items-center gap-1.5
                ${showFilters
                                ? 'border-primary-red text-primary-red bg-red-950'
                                : 'border-gray-700 text-gray-400 hover:border-gray-500 hover:text-gray-300'
                            }`}
                        >
                            <SlidersHorizontal className="w-4 h-4" />
                            <span className="hidden sm:inline">Filtres</span>
                        </button>
                    </div>

                    {/* Filtres avancés */}
                    {showFilters && (
                        <div className="mt-3 p-4 bg-secondary-black border border-gray-800 rounded-xl grid grid-cols-1 sm:grid-cols-3 gap-3">
                            <div>
                                <label className="block text-xs font-medium text-gray-400 mb-1">Genre</label>
                                <input
                                    type="text"
                                    value={pending.genre}
                                    onChange={(e) => setPending({ ...pending, genre: e.target.value })}
                                    placeholder="RPG, Action…"
                                    className="w-full bg-accent-black border border-gray-700 rounded-lg px-3 py-2
                             text-white text-sm placeholder-gray-600 focus:outline-none
                             focus:border-primary-red"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-medium text-gray-400 mb-1">Plateforme</label>
                                <input
                                    type="text"
                                    value={pending.plateforme}
                                    onChange={(e) => setPending({ ...pending, plateforme: e.target.value })}
                                    placeholder="PC, PlayStation…"
                                    className="w-full bg-accent-black border border-gray-700 rounded-lg px-3 py-2
                             text-white text-sm placeholder-gray-600 focus:outline-none
                             focus:border-primary-red"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-medium text-gray-400 mb-1">
                                    Note min. <span className="text-gray-600">({pending.noteMin || '0'}/10)</span>
                                </label>
                                <input
                                    type="range"
                                    min="0"
                                    max="10"
                                    step="0.5"
                                    value={pending.noteMin || 0}
                                    onChange={(e) => setPending({ ...pending, noteMin: e.target.value })}
                                    className="w-full accent-primary-red"
                                />
                            </div>
                            <div className="sm:col-span-3 flex gap-2 pt-1">
                                <button
                                    type="submit"
                                    className="px-4 py-2 bg-primary-red hover:bg-secondary-red text-white text-sm
                             font-medium rounded-lg transition-colors"
                                >
                                    <Filter className="w-3.5 h-3.5 inline mr-1.5" />
                                    Appliquer
                                </button>
                                <button
                                    type="button"
                                    onClick={() => {
                                        const reset = { titre: '', genre: '', plateforme: '', noteMin: '' };
                                        setPending(reset);
                                        setFilters(reset);
                                        setPage(0);
                                    }}
                                    className="px-4 py-2 border border-gray-700 text-gray-400 text-sm
                             font-medium rounded-lg hover:border-gray-500 hover:text-gray-300 transition-colors"
                                >
                                    Réinitialiser
                                </button>
                            </div>
                        </div>
                    )}
                </form>

                {/* Grid */}
                {loading ? (
                    <div className="flex items-center justify-center py-24">
                        <Loader2 className="w-8 h-8 text-primary-red animate-spin" />
                    </div>
                ) : gameList.length === 0 ? (
                    <div className="text-center py-24">
                        <p className="text-gray-500 text-lg">Aucun jeu trouvé</p>
                        <p className="text-gray-600 text-sm mt-2">Essayez d'autres critères de recherche</p>
                    </div>
                ) : (
                    <>
                        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
                            {gameList.map((jeu) => (
                                <GameCard
                                    key={jeu.id}
                                    jeu={jeu}
                                    isAuth={isAuth}
                                    onAddToBibliotheque={handleAddToBibliotheque}
                                />
                            ))}
                        </div>

                        {/* Pagination */}
                        {totalPages > 1 && (
                            <div className="flex items-center justify-center gap-3 mt-8">
                                <button
                                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                                    disabled={page === 0}
                                    className="flex items-center gap-1 px-3 py-2 rounded-lg border border-gray-700
                             text-gray-400 text-sm font-medium hover:border-gray-500 hover:text-gray-300
                             transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                                >
                                    <ChevronLeft className="w-4 h-4" />
                                    Précédent
                                </button>
                                <span className="text-gray-500 text-sm">
                  Page <span className="text-white font-semibold">{page + 1}</span> / {totalPages}
                </span>
                                <button
                                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                                    disabled={page >= totalPages - 1}
                                    className="flex items-center gap-1 px-3 py-2 rounded-lg border border-gray-700
                             text-gray-400 text-sm font-medium hover:border-gray-500 hover:text-gray-300
                             transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                                >
                                    Suivant
                                    <ChevronRight className="w-4 h-4" />
                                </button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
};