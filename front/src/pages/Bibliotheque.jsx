import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
    Search, Filter, Star, SlidersHorizontal, ChevronLeft, ChevronRight,
    Plus, Loader2, ChevronDown, Flag, AlertCircle, CheckCircle
} from 'lucide-react';
import { jeux, bibliotheque, reports } from '../api.js';
import { useAuth } from '../context/AuthContext.jsx';

const STATUTS = [
    { value: 'JOUER',      label: 'En cours',  color: 'text-yellow-400 border-yellow-600 bg-yellow-950' },
    { value: 'A_JOUER',    label: 'À jouer',   color: 'text-blue-400 border-blue-600 bg-blue-950' },
    { value: 'FINIT',      label: 'Terminé',   color: 'text-green-400 border-green-600 bg-green-950' },
    { value: 'ABANDONNER', label: 'Abandonné', color: 'text-gray-400 border-gray-600 bg-gray-900' },
];

const RAISONS = [
    { value: 'CONTENU_INAPPROPRIE', label: 'Contenu inapproprié' },
    { value: 'SPAM',                label: 'Spam' },
    { value: 'HARCELEMENT',         label: 'Harcèlement' },
    { value: 'FAUSSE_INFORMATION',  label: 'Fausse information' },
    { value: 'AUTRE',               label: 'Autre' },
];

const SORT_OPTIONS = [
    { value: 'titre',       label: 'Titre (A-Z)' },
    { value: 'noteMoyenne', label: 'Mieux notés' },
    { value: 'dateSortie',  label: 'Plus récents' },
    { value: 'vues',        label: 'Plus consultés' },
    { value: 'popularite',  label: 'Plus populaires' },
];

const CURRENT_YEAR = new Date().getFullYear();
const YEARS = Array.from({ length: CURRENT_YEAR - 1979 }, (_, i) => CURRENT_YEAR - i);

const selectCls = `w-full bg-accent-black border border-gray-700 rounded-lg px-3 py-2
    text-white text-sm focus:outline-none focus:border-primary-red
    focus:ring-1 focus:ring-primary-red transition-colors appearance-none cursor-pointer`;

function SelectFilter({ label, value, onChange, options, placeholder }) {
    return (
        <div>
            <label className="block text-xs font-medium text-gray-400 mb-1">{label}</label>
            <div className="relative">
                <select value={value} onChange={(e) => onChange(e.target.value)} className={selectCls}>
                    <option value="">{placeholder}</option>
                    {options.map((opt) => (
                        <option key={typeof opt === 'object' ? opt.value : opt}
                                value={typeof opt === 'object' ? opt.value : opt}>
                            {typeof opt === 'object' ? opt.label : opt}
                        </option>
                    ))}
                </select>
                <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5
                                        text-gray-500 pointer-events-none" />
            </div>
        </div>
    );
}

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
                            className="w-full bg-accent-black border border-gray-700 rounded-lg px-3 py-2.5 text-white text-sm focus:outline-none focus:border-primary-red">
                        {RAISONS.map(({ value, label }) => <option key={value} value={value}>{label}</option>)}
                    </select>
                    <textarea value={details} onChange={(e) => setDetails(e.target.value)} rows={3}
                              placeholder="Détails optionnels…"
                              className="w-full bg-accent-black border border-gray-700 rounded-lg px-3 py-2 text-white text-sm placeholder-gray-600 resize-none focus:outline-none focus:border-primary-red" />
                    <div className="flex gap-2">
                        <button type="button" onClick={onClose}
                                className="flex-1 py-2 rounded-lg border border-gray-700 text-gray-400 text-sm font-medium hover:border-gray-500 transition-colors">
                            Annuler
                        </button>
                        <button type="submit" disabled={loading}
                                className="flex-1 py-2 rounded-lg bg-primary-red hover:bg-secondary-red text-white text-sm font-medium transition-colors disabled:opacity-50">
                            {loading ? <Loader2 className="w-4 h-4 animate-spin mx-auto" /> : 'Signaler'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

function GameCard({ jeu: game, isAuth, onAddToBibliotheque, onToast }) {
    const [showMenu, setShowMenu]     = useState(false);
    const [added, setAdded]           = useState(false);
    const [alreadyIn, setAlreadyIn]   = useState(false);
    const [reportOpen, setReportOpen] = useState(false);

    const handleAdd = async (statut) => {
        try { await onAddToBibliotheque(game.id, statut); setAdded(true); setShowMenu(false); }
        catch (err) { setShowMenu(false); if (err.status === 409) setAlreadyIn(true); }
    };
    const handleReport = async (raison, details) => {
        try { await reports.soumettre({ typeContenu: 'JEU', idContenu: game.id, raison, details }); onToast('Jeu signalé !'); }
        catch (err) { onToast(err.message || 'Erreur', true); }
    };

    const note = game.noteMoyenne > 0 ? game.noteMoyenne.toFixed(1) : null;

    return (
        <>
            {reportOpen && <ReportModal titre={game.titre} onClose={() => setReportOpen(false)} onSubmit={handleReport} />}
            <div className="group relative bg-secondary-black border border-gray-800 rounded-xl overflow-hidden
                            hover:border-gray-600 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl">
                <Link to={`/bibliotheque/${game.id}`} className="block">
                    <div className="relative aspect-[3/4] bg-accent-black overflow-hidden">
                        {game.coverUrl
                            ? <img src={game.coverUrl} alt={game.titre} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                            : <div className="w-full h-full flex items-center justify-center text-gray-700 text-4xl">🎮</div>}
                        {note && (
                            <div className="absolute top-2 right-2 flex items-center gap-1 px-2 py-1 bg-black/70 backdrop-blur-sm rounded-md">
                                <Star className="w-3 h-3 text-yellow-400 fill-yellow-400" />
                                <span className="text-white text-xs font-semibold">{note}</span>
                            </div>
                        )}
                        {game.source === 'manuel' && (
                            <div className="absolute top-2 left-2 px-1.5 py-0.5 bg-purple-900/80 backdrop-blur-sm rounded text-purple-300 text-xs font-medium">
                                Manuel
                            </div>
                        )}
                    </div>
                </Link>
                <div className="p-3">
                    <Link to={`/bibliotheque/${game.id}`} className="block">
                        <h3 className="text-white font-semibold text-sm leading-tight mb-1 line-clamp-2 hover:text-primary-red transition-colors">
                            {game.titre}
                        </h3>
                    </Link>
                    <div className="flex flex-wrap gap-1 mb-2">
                        {game.genre     && <span className="text-xs text-gray-500 bg-gray-800 px-2 py-0.5 rounded">{game.genre}</span>}
                        {game.plateforme && <span className="text-xs text-gray-500 bg-gray-800 px-2 py-0.5 rounded">{game.plateforme}</span>}
                    </div>
                    {isAuth && (
                        <div className="space-y-1.5">
                            <div className="relative">
                                {alreadyIn ? (
                                    <div className="w-full py-1.5 text-xs font-medium text-orange-400 text-center">Déjà dans votre bibliothèque</div>
                                ) : added ? (
                                    <div className="w-full py-1.5 text-xs font-medium text-green-400 text-center">✓ Ajouté</div>
                                ) : (
                                    <>
                                        <button onClick={() => setShowMenu(!showMenu)}
                                                className="w-full flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-lg text-xs font-medium border border-gray-700 text-gray-400 hover:border-primary-red hover:text-primary-red transition-colors">
                                            <Plus className="w-3 h-3" />Ajouter
                                        </button>
                                        {showMenu && (
                                            <div className="absolute bottom-full left-0 right-0 mb-1 z-10 bg-secondary-black border border-gray-700 rounded-lg overflow-hidden shadow-xl">
                                                {STATUTS.map(({ value, label, color }) => (
                                                    <button key={value} onClick={() => handleAdd(value)}
                                                            className={`w-full px-3 py-2 text-left text-xs font-medium transition-colors hover:bg-gray-800 ${color.split(' ')[0]}`}>
                                                        {label}
                                                    </button>
                                                ))}
                                            </div>
                                        )}
                                    </>
                                )}
                            </div>
                            <button onClick={() => setReportOpen(true)}
                                    className="w-full flex items-center justify-center gap-1.5 py-1 text-xs text-gray-600 hover:text-orange-400 transition-colors">
                                <Flag className="w-3 h-3" />Signaler
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </>
    );
}

export const Bibliotheque = () => {
    const { isAuth } = useAuth();
    const [gameList, setGameList]             = useState([]);
    const [loading, setLoading]               = useState(true);
    const [genres, setGenres]                 = useState([]);
    const [plateformes, setPlateformes]       = useState([]);
    const [loadingFilters, setLoadingFilters] = useState(true);
    const [filters, setFilters] = useState({ titre: '', genre: '', plateforme: '', noteMin: '', anneeMin: '', anneeMax: '', sort: 'titre' });
    const [pending, setPending] = useState(filters);
    const [page, setPage]       = useState(0);
    const [totalPages, setTotalPages]         = useState(0);
    const [totalElements, setTotalElements]   = useState(0);
    const [showFilters, setShowFilters]       = useState(false);
    const [feedback, setFeedback]             = useState('');
    const [feedbackErr, setFeedbackErr]       = useState('');

    const toast = (msg, err = false) => {
        err ? setFeedbackErr(msg) : setFeedback(msg);
        setTimeout(() => { setFeedback(''); setFeedbackErr(''); }, 3000);
    };

    useEffect(() => {
        const load = async () => {
            setLoadingFilters(true);
            try {
                const [g, p] = await Promise.all([jeux.getGenres(), jeux.getPlateformes()]);
                setGenres(g || []);
                setPlateformes(p || []);
            } catch { /* silencieux */ }
            finally { setLoadingFilters(false); }
        };
        load();
    }, []);

    const fetchJeux = useCallback(async (f, p) => {
        setLoading(true);
        try {
            const params = { ...f, page: p, size: 20 };
            Object.keys(params).forEach((k) => (params[k] === '' || params[k] === null) && delete params[k]);
            const data = await jeux.lister(params);
            setGameList(data.content || []);
            setTotalPages(data.totalPages || 0);
            setTotalElements(data.totalElements || 0);
        } catch { setGameList([]); }
        finally { setLoading(false); }
    }, []);

    useEffect(() => { fetchJeux(filters, page); }, [filters, page, fetchJeux]);

    const handleSearch = (e) => { e.preventDefault(); setFilters(pending); setPage(0); };
    const handleReset  = () => {
        const reset = { titre: '', genre: '', plateforme: '', noteMin: '', anneeMin: '', anneeMax: '', sort: 'titre' };
        setPending(reset); setFilters(reset); setPage(0);
    };
    const handleAddToBibliotheque = async (jeuId, statut) => {
        await bibliotheque.ajouter(jeuId, statut);
        toast('Jeu ajouté à votre bibliothèque !');
    };

    const activeFilterCount = [filters.genre, filters.plateforme, filters.noteMin, filters.anneeMin, filters.anneeMax]
        .filter(Boolean).length;

    return (
        <div className="min-h-screen bg-primary-black">
            {feedback    && <div className="fixed bottom-6 right-6 z-50 px-4 py-3 bg-green-900 border border-green-700 text-green-300 text-sm font-medium rounded-xl shadow-xl flex items-center gap-2"><CheckCircle className="w-4 h-4" />{feedback}</div>}
            {feedbackErr && <div className="fixed bottom-6 right-6 z-50 px-4 py-3 bg-red-950 border border-red-800 text-red-300 text-sm font-medium rounded-xl shadow-xl flex items-center gap-2"><AlertCircle className="w-4 h-4" />{feedbackErr}</div>}

            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="mb-6">
                    <h1 className="text-3xl font-black text-white mb-1">Catalogue</h1>
                    <p className="text-gray-500 text-sm">
                        {totalElements > 0 ? `${totalElements.toLocaleString('fr-FR')} jeux disponibles` : 'Explorez notre collection'}
                    </p>
                </div>

                <form onSubmit={handleSearch} className="mb-4">
                    <div className="flex gap-2">
                        <div className="relative flex-1">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                            <input type="text" value={pending.titre}
                                   onChange={(e) => setPending({ ...pending, titre: e.target.value })}
                                   placeholder="Rechercher un jeu…" autoComplete="off"
                                   className="w-full bg-secondary-black border border-gray-700 rounded-lg pl-10 pr-4 py-2.5 text-white text-sm placeholder-gray-600 focus:outline-none focus:border-primary-red focus:ring-1 focus:ring-primary-red" />
                        </div>
                        {/* Tri rapide */}
                        <div className="relative hidden sm:block">
                            <select value={pending.sort}
                                    onChange={(e) => { const v = { ...pending, sort: e.target.value }; setPending(v); setFilters(v); setPage(0); }}
                                    className="bg-secondary-black border border-gray-700 rounded-lg pl-3 pr-8 py-2.5 text-white text-sm focus:outline-none focus:border-primary-red appearance-none cursor-pointer">
                                {SORT_OPTIONS.map(({ value, label }) => <option key={value} value={value}>{label}</option>)}
                            </select>
                            <ChevronDown className="absolute right-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-500 pointer-events-none" />
                        </div>
                        <button type="submit" className="px-4 py-2.5 bg-primary-red hover:bg-secondary-red text-white text-sm font-medium rounded-lg transition-colors">
                            Chercher
                        </button>
                        <button type="button" onClick={() => setShowFilters(!showFilters)}
                                className={`px-3 py-2.5 rounded-lg border text-sm font-medium transition-colors flex items-center gap-1.5 relative
                                    ${showFilters ? 'border-primary-red text-primary-red bg-red-950' : 'border-gray-700 text-gray-400 hover:border-gray-500 hover:text-gray-300'}`}>
                            <SlidersHorizontal className="w-4 h-4" />
                            <span className="hidden sm:inline">Filtres</span>
                            {activeFilterCount > 0 && (
                                <span className="absolute -top-1.5 -right-1.5 w-4 h-4 bg-primary-red text-white text-xs rounded-full flex items-center justify-center">
                                    {activeFilterCount}
                                </span>
                            )}
                        </button>
                    </div>

                    {showFilters && (
                        <div className="mt-3 p-4 bg-secondary-black border border-gray-800 rounded-xl grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
                            {loadingFilters ? (
                                <div className="flex items-center gap-2 text-gray-500 text-sm col-span-2">
                                    <Loader2 className="w-4 h-4 animate-spin" />Chargement des filtres…
                                </div>
                            ) : (
                                <>
                                    <SelectFilter label="Genre" value={pending.genre}
                                                  onChange={(v) => setPending({ ...pending, genre: v })}
                                                  options={genres} placeholder="Tous les genres" />
                                    <SelectFilter label="Plateforme" value={pending.plateforme}
                                                  onChange={(v) => setPending({ ...pending, plateforme: v })}
                                                  options={plateformes} placeholder="Toutes les plateformes" />
                                </>
                            )}
                            <SelectFilter label="Année min." value={pending.anneeMin}
                                          onChange={(v) => setPending({ ...pending, anneeMin: v })}
                                          options={YEARS} placeholder="Depuis…" />
                            <SelectFilter label="Année max." value={pending.anneeMax}
                                          onChange={(v) => setPending({ ...pending, anneeMax: v })}
                                          options={YEARS} placeholder="Jusqu'à…" />
                            <div>
                                <label className="block text-xs font-medium text-gray-400 mb-1">
                                    Note min. <span className="text-gray-600">({pending.noteMin || '0'}/10)</span>
                                </label>
                                <input type="range" min="0" max="10" step="0.5"
                                       value={pending.noteMin || 0}
                                       onChange={(e) => setPending({ ...pending, noteMin: e.target.value })}
                                       className="w-full accent-primary-red" />
                            </div>
                            <div className="sm:col-span-2 lg:col-span-4 flex gap-2 pt-1">
                                <button type="submit" className="px-4 py-2 bg-primary-red hover:bg-secondary-red text-white text-sm font-medium rounded-lg transition-colors">
                                    <Filter className="w-3.5 h-3.5 inline mr-1.5" />Appliquer
                                </button>
                                <button type="button" onClick={handleReset}
                                        className="px-4 py-2 border border-gray-700 text-gray-400 text-sm font-medium rounded-lg hover:border-gray-500 hover:text-gray-300 transition-colors">
                                    Réinitialiser
                                </button>
                            </div>
                        </div>
                    )}
                </form>

                {/* Chips filtres actifs */}
                {(filters.genre || filters.plateforme || filters.anneeMin || filters.anneeMax || (filters.noteMin && filters.noteMin !== '0')) && (
                    <div className="flex flex-wrap gap-2 mb-4">
                        {filters.genre     && <button onClick={() => { setFilters({ ...filters, genre: '' }); setPending({ ...pending, genre: '' }); setPage(0); }} className="px-2.5 py-1 bg-red-950 border border-red-800 text-red-300 text-xs rounded-lg hover:bg-red-900 transition-colors">{filters.genre} ×</button>}
                        {filters.plateforme && <button onClick={() => { setFilters({ ...filters, plateforme: '' }); setPending({ ...pending, plateforme: '' }); setPage(0); }} className="px-2.5 py-1 bg-red-950 border border-red-800 text-red-300 text-xs rounded-lg hover:bg-red-900 transition-colors">{filters.plateforme} ×</button>}
                        {filters.anneeMin  && <button onClick={() => { setFilters({ ...filters, anneeMin: '' }); setPending({ ...pending, anneeMin: '' }); setPage(0); }} className="px-2.5 py-1 bg-red-950 border border-red-800 text-red-300 text-xs rounded-lg hover:bg-red-900 transition-colors">Depuis {filters.anneeMin} ×</button>}
                        {filters.anneeMax  && <button onClick={() => { setFilters({ ...filters, anneeMax: '' }); setPending({ ...pending, anneeMax: '' }); setPage(0); }} className="px-2.5 py-1 bg-red-950 border border-red-800 text-red-300 text-xs rounded-lg hover:bg-red-900 transition-colors">Jusqu'à {filters.anneeMax} ×</button>}
                        {filters.noteMin && filters.noteMin !== '0' && <button onClick={() => { setFilters({ ...filters, noteMin: '' }); setPending({ ...pending, noteMin: '' }); setPage(0); }} className="px-2.5 py-1 bg-red-950 border border-red-800 text-red-300 text-xs rounded-lg hover:bg-red-900 transition-colors">Note ≥ {filters.noteMin} ×</button>}
                    </div>
                )}

                {loading ? (
                    <div className="flex items-center justify-center py-24"><Loader2 className="w-8 h-8 text-primary-red animate-spin" /></div>
                ) : gameList.length === 0 ? (
                    <div className="text-center py-24"><p className="text-gray-500 text-lg">Aucun jeu trouvé</p><p className="text-gray-600 text-sm mt-2">Essayez d'autres critères</p></div>
                ) : (
                    <>
                        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
                            {gameList.map((game) => (
                                <GameCard key={game.id} jeu={game} isAuth={isAuth}
                                          onAddToBibliotheque={handleAddToBibliotheque} onToast={toast} />
                            ))}
                        </div>
                        {totalPages > 1 && (
                            <div className="flex items-center justify-center gap-3 mt-8">
                                <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}
                                        className="flex items-center gap-1 px-3 py-2 rounded-lg border border-gray-700 text-gray-400 text-sm font-medium hover:border-gray-500 hover:text-gray-300 transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
                                    <ChevronLeft className="w-4 h-4" />Précédent
                                </button>
                                <span className="text-gray-500 text-sm">
                                    Page <span className="text-white font-semibold">{page + 1}</span> / {totalPages}
                                </span>
                                <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
                                        className="flex items-center gap-1 px-3 py-2 rounded-lg border border-gray-700 text-gray-400 text-sm font-medium hover:border-gray-500 hover:text-gray-300 transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
                                    Suivant<ChevronRight className="w-4 h-4" />
                                </button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
};