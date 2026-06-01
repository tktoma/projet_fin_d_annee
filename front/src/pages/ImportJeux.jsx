import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
    Search, Download, CheckCircle, AlertCircle, Loader2,
    Shield, Gamepad2, Calendar, Tag, Monitor, Lock,
    PenLine, Zap, ChevronRight, X, SkipForward
} from 'lucide-react';
import { jeux } from '../api.js';
import { useAuth } from '../context/AuthContext.jsx';

const ALLOWED_ROLES = ['POSTER', 'ADMIN', 'SUPERADMIN'];
const ADMIN_ROLES   = ['ADMIN', 'SUPERADMIN'];

function getRoleFromToken() {
    const token = localStorage.getItem('token');
    if (!token) return null;
    try { return JSON.parse(atob(token.split('.')[1])).role || null; }
    catch { return null; }
}

const inputCls = `w-full bg-accent-black border border-gray-700 rounded-lg px-3 py-2.5
    text-white text-sm placeholder-gray-600 focus:outline-none focus:border-primary-red
    focus:ring-1 focus:ring-primary-red transition-colors`;

function RoleBadge({ role }) {
    const map = {
        POSTER:     { label: 'Contributeur',   cls: 'text-blue-400 bg-blue-950 border-blue-800' },
        ADMIN:      { label: 'Administrateur', cls: 'text-orange-400 bg-orange-950 border-orange-800' },
        SUPERADMIN: { label: 'Super Admin',    cls: 'text-red-400 bg-red-950 border-red-800' },
        USER:       { label: 'Membre',         cls: 'text-gray-400 bg-gray-900 border-gray-700' },
    };
    const { label, cls } = map[role] || map.USER;
    return (
        <span className={`text-xs font-medium px-2.5 py-1 rounded-full border ${cls}`}>
            {label}
        </span>
    );
}

function Chip({ icon, label }) {
    return (
        <span className="flex items-center gap-1 text-xs text-gray-400 bg-gray-800 px-2 py-0.5 rounded">
            {icon}{label}
        </span>
    );
}

function Toast({ msg, error }) {
    return (
        <div className={`fixed bottom-6 right-6 z-50 px-4 py-3 border text-sm font-medium
                         rounded-xl shadow-xl flex items-center gap-2
                         ${error
            ? 'bg-red-950 border-red-800 text-red-300'
            : 'bg-green-900 border-green-700 text-green-300'}`}>
            {error ? <AlertCircle className="w-4 h-4" /> : <CheckCircle className="w-4 h-4" />}
            {msg}
        </div>
    );
}

function ProgressBar({ value, max, color = 'bg-primary-red' }) {
    const pct = max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0;
    return (
        <div className="w-full bg-gray-800 rounded-full h-2.5 overflow-hidden">
            <div
                className={`h-full rounded-full transition-all duration-700 ${color}`}
                style={{ width: `${pct}%` }}
            />
        </div>
    );
}

// ── Onglet 1 : Recherche IGDB ─────────────────────────────────────────────────

function TabIgdb() {
    const [query, setQuery]               = useState('');
    const [results, setResults]           = useState([]);
    const [searching, setSearching]       = useState(false);
    const [searchError, setSearchError]   = useState('');
    const [hasSearched, setHasSearched]   = useState(false);
    const [importStates, setImportStates] = useState({});
    const [feedback, setFeedback]         = useState('');
    const [feedbackErr, setFeedbackErr]   = useState('');

    const toast = (msg, err = false) => {
        err ? setFeedbackErr(msg) : setFeedback(msg);
        setTimeout(() => { setFeedback(''); setFeedbackErr(''); }, 3000);
    };

    const handleSearch = async (e) => {
        e.preventDefault();
        if (!query.trim()) return;
        setSearching(true); setSearchError(''); setResults([]); setHasSearched(true);
        try {
            const data = await jeux.rechercher(query.trim());
            setResults(data || []);
            if (!data?.length) setSearchError('Aucun résultat pour cette recherche.');
        } catch (err) { setSearchError(err.message || 'Erreur IGDB.'); }
        finally { setSearching(false); }
    };

    const handleImport = async (igdbId) => {
        setImportStates((p) => ({ ...p, [igdbId]: 'importing' }));
        try {
            await jeux.importer(igdbId);
            setImportStates((p) => ({ ...p, [igdbId]: 'done' }));
            const g = results.find((r) => r.id === igdbId);
            toast(`"${g?.name}" importé !`);
        } catch (err) {
            setImportStates((p) => ({ ...p, [igdbId]: null }));
            toast(err.message || 'Erreur import.', true);
        }
    };

    return (
        <div>
            {feedback    && <Toast msg={feedback} />}
            {feedbackErr && <Toast msg={feedbackErr} error />}

            <form onSubmit={handleSearch} className="flex gap-2 mb-5" autoComplete="off">
                <div className="relative flex-1">
                    <Gamepad2 className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                    <input
                        type="text" value={query}
                        onChange={(e) => setQuery(e.target.value)}
                        placeholder="Titre du jeu (ex: Dark Souls, Zelda…)"
                        autoComplete="off"
                        className={`${inputCls} pl-10`}
                        autoFocus
                    />
                </div>
                <button type="submit" disabled={searching || !query.trim()}
                        className="px-5 py-3 bg-primary-red hover:bg-secondary-red text-white text-sm
                                   font-semibold rounded-lg transition-all disabled:opacity-50
                                   disabled:cursor-not-allowed flex items-center gap-2">
                    {searching
                        ? <Loader2 className="w-4 h-4 animate-spin" />
                        : <Search className="w-4 h-4" />}
                    Rechercher
                </button>
            </form>

            {!hasSearched && (
                <div className="flex items-start gap-3 p-4 bg-secondary-black border border-gray-800
                                rounded-xl text-sm text-gray-500">
                    <AlertCircle className="w-4 h-4 text-gray-600 flex-shrink-0 mt-0.5" />
                    <p>
                        Les résultats proviennent de{' '}
                        <span className="text-gray-300 font-medium">IGDB</span>.
                        Un jeu déjà présent sera mis à jour si vous le réimportez.
                    </p>
                </div>
            )}

            {searching && (
                <div className="flex items-center justify-center py-16">
                    <div className="text-center">
                        <Loader2 className="w-8 h-8 text-primary-red animate-spin mx-auto mb-3" />
                        <p className="text-gray-500 text-sm">Recherche sur IGDB…</p>
                    </div>
                </div>
            )}

            {searchError && !searching && (
                <div className="flex items-center gap-2 p-4 bg-red-950/50 border border-red-900
                                rounded-xl text-red-400 text-sm">
                    <AlertCircle className="w-4 h-4 flex-shrink-0" />{searchError}
                </div>
            )}

            {!searching && results.length > 0 && (
                <div>
                    <p className="text-gray-500 text-xs mb-3">
                        {results.length} résultat{results.length > 1 ? 's' : ''} pour{' '}
                        <span className="text-gray-300 font-medium">"{query}"</span>
                    </p>
                    <div className="space-y-2">
                        {results.map((game) => {
                            const coverUrl = game.cover?.url
                                ? 'https:' + game.cover.url.replace('t_thumb', 't_cover_big')
                                : null;
                            const genre    = game.genres?.[0]?.name;
                            const platform = game.platforms?.[0]?.name;
                            const year     = game.firstReleaseDate
                                ? new Date(game.firstReleaseDate * 1000).getFullYear()
                                : null;
                            const state = importStates[game.id];

                            return (
                                <div key={game.id}
                                     className={`flex gap-4 p-4 rounded-xl border transition-all
                                         ${state === 'done'
                                         ? 'bg-green-950/30 border-green-800'
                                         : 'bg-secondary-black border-gray-800 hover:border-gray-600'}`}>
                                    <div className="w-16 flex-shrink-0 rounded-lg overflow-hidden bg-accent-black"
                                         style={{ height: 88 }}>
                                        {coverUrl
                                            ? <img src={coverUrl} alt={game.name} className="w-full h-full object-cover" />
                                            : <div className="w-full h-full flex items-center justify-center text-2xl text-gray-700">🎮</div>
                                        }
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <h3 className="text-white font-semibold text-sm truncate mb-1.5">
                                            {game.name}
                                        </h3>
                                        <div className="flex flex-wrap gap-1.5 mb-2">
                                            {genre    && <Chip icon={<Tag      className="w-2.5 h-2.5" />} label={genre} />}
                                            {platform && <Chip icon={<Monitor  className="w-2.5 h-2.5" />} label={platform} />}
                                            {year     && <Chip icon={<Calendar className="w-2.5 h-2.5" />} label={year} />}
                                        </div>
                                        {game.summary && (
                                            <p className="text-gray-500 text-xs leading-relaxed line-clamp-2">
                                                {game.summary}
                                            </p>
                                        )}
                                    </div>
                                    <div className="flex-shrink-0 flex items-center">
                                        {state === 'done' ? (
                                            <span className="flex items-center gap-1.5 text-green-400 text-xs font-medium">
                                                <CheckCircle className="w-4 h-4" />
                                                <span className="hidden sm:inline">Importé</span>
                                            </span>
                                        ) : (
                                            <button
                                                onClick={() => handleImport(game.id)}
                                                disabled={state === 'importing'}
                                                className="flex items-center gap-1.5 px-3 py-2 rounded-lg
                                                           bg-primary-red hover:bg-secondary-red text-white
                                                           text-xs font-semibold transition-all
                                                           disabled:opacity-50 hover:scale-105">
                                                {state === 'importing'
                                                    ? <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                                    : <Download className="w-3.5 h-3.5" />}
                                                <span className="hidden sm:inline">Importer</span>
                                            </button>
                                        )}
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

// ── Onglet 2 : Formulaire manuel ──────────────────────────────────────────────

function TabManuel() {
    const empty = { titre: '', genre: '', plateforme: '', coverUrl: '', dateSortie: '', description: '' };
    const [form, setForm]       = useState(empty);
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(null);
    const [error, setError]     = useState('');

    const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true); setError(''); setSuccess(null);
        try {
            const result = await jeux.creerManuellement(form);
            setSuccess(result);
            setForm(empty);
        } catch (err) {
            setError(err.message || 'Erreur lors de la création.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            {success && (
                <div className="flex items-center justify-between p-4 mb-5 bg-green-950/40
                                border border-green-800 rounded-xl">
                    <div className="flex items-center gap-2 text-green-400 text-sm font-medium">
                        <CheckCircle className="w-4 h-4" />
                        <span>"{success.titre}" créé !</span>
                        <Link to={`/bibliotheque/${success.id}`}
                              className="underline text-green-300 hover:text-green-200 transition-colors">
                            Voir la fiche
                        </Link>
                    </div>
                    <button onClick={() => setSuccess(null)} className="text-green-600 hover:text-green-400">
                        <X className="w-4 h-4" />
                    </button>
                </div>
            )}

            {error && (
                <div className="flex items-center gap-2 p-4 mb-5 bg-red-950/50 border border-red-900
                                rounded-xl text-red-400 text-sm">
                    <AlertCircle className="w-4 h-4 flex-shrink-0" />{error}
                </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4" autoComplete="off">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="sm:col-span-2">
                        <label className="block text-xs font-medium text-gray-400 mb-1.5">
                            Titre <span className="text-primary-red">*</span>
                        </label>
                        <input type="text" name="titre" value={form.titre}
                               onChange={handleChange} required
                               autoComplete="off"
                               placeholder="ex: My Indie Game" className={inputCls} />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-gray-400 mb-1.5">Genre</label>
                        <input type="text" name="genre" value={form.genre}
                               onChange={handleChange} autoComplete="off"
                               placeholder="ex: RPG, Action…" className={inputCls} />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-gray-400 mb-1.5">Plateforme</label>
                        <input type="text" name="plateforme" value={form.plateforme}
                               onChange={handleChange} autoComplete="off"
                               placeholder="ex: PC, PS5…" className={inputCls} />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-gray-400 mb-1.5">Date de sortie</label>
                        <input type="date" name="dateSortie" value={form.dateSortie}
                               onChange={handleChange} autoComplete="off"
                               className={inputCls} />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-gray-400 mb-1.5">URL de la cover</label>
                        <input type="url" name="coverUrl" value={form.coverUrl}
                               onChange={handleChange} autoComplete="off"
                               placeholder="https://…" className={inputCls} />
                    </div>
                    <div className="sm:col-span-2">
                        <label className="block text-xs font-medium text-gray-400 mb-1.5">Description</label>
                        <textarea name="description" value={form.description}
                                  onChange={handleChange} rows={4}
                                  placeholder="Décrivez le jeu…"
                                  className={`${inputCls} resize-none`} />
                    </div>
                </div>

                {form.coverUrl && (
                    <div className="flex items-center gap-3 p-3 bg-accent-black border border-gray-700 rounded-xl">
                        <img src={form.coverUrl} alt="preview"
                             className="w-12 h-16 object-cover rounded-lg"
                             onError={(e) => { e.target.style.display = 'none'; }} />
                        <p className="text-gray-400 text-xs">Aperçu de la cover</p>
                    </div>
                )}

                <button type="submit" disabled={loading || !form.titre.trim()}
                        className="w-full py-3 rounded-lg bg-primary-red hover:bg-secondary-red
                                   text-white font-semibold text-sm transition-all
                                   disabled:opacity-50 disabled:cursor-not-allowed
                                   flex items-center justify-center gap-2 hover:scale-[1.01]">
                    {loading
                        ? <Loader2 className="w-4 h-4 animate-spin" />
                        : <PenLine className="w-4 h-4" />}
                    Créer le jeu
                </button>
            </form>
        </div>
    );
}

// ── Onglet 3 : Import auto ────────────────────────────────────────────────────

function TabAuto({ role }) {
    const isAdmin               = ADMIN_ROLES.includes(role);
    const [launching, setLaunching] = useState(false);
    const [prog, setProg]           = useState(null);
    const [error, setError]         = useState('');
    const intervalRef               = useRef(null);

    const stopPolling = () => clearInterval(intervalRef.current);

    const startPolling = useCallback(() => {
        stopPolling();
        intervalRef.current = setInterval(async () => {
            try {
                const data = await jeux.getProgression();
                setProg({ ...data });
                if (data.done || !data.running) stopPolling();
            } catch {
                // ignore erreurs réseau temporaires
            }
        }, 2000);
    }, []);

    useEffect(() => () => stopPolling(), []);

    const handleLancer = async () => {
        setLaunching(true);
        setError('');
        try {
            // POST retourne l'état initial (running=true déjà positionné côté serveur)
            const initial = await jeux.importerAuto();
            setProg({ ...initial });
            startPolling();
        } catch (err) {
            setError(err.message || "Erreur lors du lancement.");
        } finally {
            setLaunching(false);
        }
    };

    if (!isAdmin) {
        return (
            <div className="flex flex-col items-center py-12 text-center">
                <div className="w-14 h-14 bg-gray-900 border border-gray-700 rounded-2xl
                                flex items-center justify-center mb-4">
                    <Shield className="w-6 h-6 text-gray-500" />
                </div>
                <p className="text-white font-semibold mb-2">Réservé aux administrateurs</p>
                <p className="text-gray-500 text-sm max-w-xs mb-4">
                    Disponible uniquement pour les rôles Admin et Super Admin.
                </p>
                <RoleBadge role={role} />
            </div>
        );
    }

    const isRunning   = prog?.running === true;
    const isDone      = prog?.done    === true;
    const hasError    = !!prog?.error;
    const total       = prog?.total    ?? 0;
    const imported    = prog?.imported ?? 0;
    const skipped     = prog?.skipped  ?? 0;
    const pctImported = total > 0 ? Math.round((imported / total) * 100) : 0;
    const pctSkipped  = total > 0 ? Math.round((skipped  / total) * 100) : 0;

    return (
        <div className="space-y-5">
            {/* Avertissement */}
            <div className="flex items-start gap-3 p-4 bg-yellow-950/40 border border-yellow-800/60
                            rounded-xl text-yellow-400 text-sm">
                <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
                <div>
                    <p className="font-semibold mb-1">Action lourde</p>
                    <p className="text-yellow-500/80 text-xs">
                        Cette opération importe l'intégralité du catalogue IGDB en arrière-plan.
                        Elle peut durer plusieurs minutes. Ne relancez pas si un import est déjà en cours.
                    </p>
                </div>
            </div>

            {/* Description */}
            <div className="bg-accent-black border border-gray-800 rounded-xl p-5 space-y-2">
                <h3 className="text-white font-semibold flex items-center gap-2 mb-3">
                    <Zap className="w-4 h-4 text-primary-red" />
                    Import automatique complet
                </h3>
                {[
                    'Parcourt toutes les pages IGDB (500 jeux / page)',
                    'Ignore les jeux déjà présents dans le catalogue',
                    'Récupère titre, cover, genre, plateforme, description',
                    'Pause automatique entre les pages (rate limit IGDB)',
                ].map((item) => (
                    <p key={item} className="flex items-center gap-2 text-sm text-gray-400">
                        <ChevronRight className="w-3 h-3 text-primary-red flex-shrink-0" />
                        {item}
                    </p>
                ))}
            </div>

            {error && (
                <div className="flex items-center gap-2 p-4 bg-red-950/50 border border-red-900
                                rounded-xl text-red-400 text-sm">
                    <AlertCircle className="w-4 h-4 flex-shrink-0" />{error}
                </div>
            )}

            {/* Bloc progression — visible dès que prog est non-null */}
            {prog && (
                <div className="bg-secondary-black border border-gray-800 rounded-xl p-5 space-y-4">
                    <div className="flex items-center justify-between">
                        <h4 className="text-white text-sm font-semibold">Progression</h4>
                        {isRunning && (
                            <span className="flex items-center gap-1.5 text-xs text-yellow-400">
                                <Loader2 className="w-3 h-3 animate-spin" />
                                En cours — page {prog.currentPage}
                            </span>
                        )}
                        {isDone && !hasError && (
                            <span className="flex items-center gap-1.5 text-xs text-green-400">
                                <CheckCircle className="w-3 h-3" />Terminé
                            </span>
                        )}
                        {hasError && (
                            <span className="flex items-center gap-1.5 text-xs text-red-400">
                                <AlertCircle className="w-3 h-3" />Erreur
                            </span>
                        )}
                    </div>

                    <div>
                        <div className="flex justify-between text-xs text-gray-400 mb-1.5">
                            <span className="flex items-center gap-1">
                                <Download className="w-3 h-3 text-green-400" />
                                Importés
                            </span>
                            <span className="text-green-400 font-semibold">
                                {imported.toLocaleString('fr-FR')} ({pctImported}%)
                            </span>
                        </div>
                        <ProgressBar value={imported} max={total} color="bg-green-500" />
                    </div>

                    <div>
                        <div className="flex justify-between text-xs text-gray-400 mb-1.5">
                            <span className="flex items-center gap-1">
                                <SkipForward className="w-3 h-3 text-gray-500" />
                                Ignorés (déjà présents)
                            </span>
                            <span className="text-gray-400 font-semibold">
                                {skipped.toLocaleString('fr-FR')} ({pctSkipped}%)
                            </span>
                        </div>
                        <ProgressBar value={skipped} max={total} color="bg-gray-600" />
                    </div>

                    <div className="pt-2 border-t border-gray-800 flex justify-between text-xs text-gray-500">
                        <span>Total traités</span>
                        <span className="text-gray-300 font-medium">
                            {total.toLocaleString('fr-FR')} jeux
                        </span>
                    </div>

                    {hasError && (
                        <p className="text-red-400 text-xs bg-red-950/40 border border-red-900
                                      rounded-lg p-3">{prog.error}</p>
                    )}
                </div>
            )}

            {!isRunning && (
                <button onClick={handleLancer} disabled={launching}
                        className="w-full py-3 rounded-lg bg-primary-red hover:bg-secondary-red
                                   text-white font-semibold text-sm transition-all
                                   disabled:opacity-50 disabled:cursor-not-allowed
                                   flex items-center justify-center gap-2 hover:scale-[1.01]">
                    {launching
                        ? <Loader2 className="w-4 h-4 animate-spin" />
                        : <Zap className="w-4 h-4" />}
                    {isDone ? "Relancer l'import" : "Lancer l'import automatique"}
                </button>
            )}

            {isRunning && (
                <p className="text-center text-xs text-gray-600">
                    Import en cours — mise à jour toutes les 2 secondes.
                </p>
            )}
        </div>
    );
}

// ── Page principale ───────────────────────────────────────────────────────────

const TABS = [
    { id: 'igdb',   label: 'Recherche IGDB', icon: Search  },
    { id: 'manuel', label: 'Ajout manuel',   icon: PenLine },
    { id: 'auto',   label: 'Import auto',    icon: Zap     },
];

function AccessDenied({ icon, bg, title, desc, extra, sub, action }) {
    return (
        <div className="min-h-screen bg-primary-black flex items-center justify-center px-4">
            <div className="text-center max-w-sm">
                <div className={`w-16 h-16 border rounded-2xl flex items-center justify-center mx-auto mb-4 ${bg}`}>
                    {icon}
                </div>
                <h2 className="text-white font-bold text-xl mb-2">{title}</h2>
                <p className="text-gray-500 text-sm mb-3">{desc}</p>
                {extra}
                {sub && <p className="text-gray-600 text-xs mb-4">{sub}</p>}
                <div className="mt-4">{action}</div>
            </div>
        </div>
    );
}

export const ImportJeux = () => {
    const { isAuth } = useAuth();
    const navigate   = useNavigate();
    const role       = getRoleFromToken();
    const authorized = isAuth && ALLOWED_ROLES.includes(role);
    const [tab, setTab] = useState('igdb');

    if (!isAuth) {
        return (
            <AccessDenied
                icon={<Lock className="w-7 h-7 text-gray-500" />}
                bg="bg-gray-900 border-gray-700"
                title="Connexion requise"
                desc="Vous devez être connecté pour accéder à cette page."
                action={
                    <button onClick={() => navigate('/connexion')}
                            className="px-5 py-2.5 rounded-lg bg-primary-red hover:bg-secondary-red
                                       text-white text-sm font-semibold transition-colors">
                        Se connecter
                    </button>
                }
            />
        );
    }

    if (!authorized) {
        return (
            <AccessDenied
                icon={<Shield className="w-7 h-7 text-red-500" />}
                bg="bg-red-950 border-red-900"
                title="Accès refusé"
                desc="Vous n'avez pas les droits pour importer des jeux."
                extra={
                    <div className="flex items-center justify-center gap-2 mb-3">
                        <span className="text-gray-600 text-xs">Votre rôle :</span>
                        <RoleBadge role={role || 'USER'} />
                    </div>
                }
                sub="Rôles requis : Contributeur, Administrateur, Super Admin."
                action={
                    <button onClick={() => navigate(-1)}
                            className="px-5 py-2.5 rounded-lg border border-gray-700 text-gray-400
                                       hover:border-gray-500 hover:text-white text-sm font-medium transition-colors">
                        Retour
                    </button>
                }
            />
        );
    }

    return (
        <div className="min-h-screen bg-primary-black">
            <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="mb-7">
                    <div className="flex items-center gap-3 mb-1">
                        <div className="w-9 h-9 bg-primary-red/20 border border-primary-red/30 rounded-lg
                                        flex items-center justify-center">
                            <Download className="w-4 h-4 text-primary-red" />
                        </div>
                        <h1 className="text-3xl font-black text-white">Importer un jeu</h1>
                    </div>
                    <div className="flex items-center gap-2 mt-1.5">
                        <p className="text-gray-500 text-sm">
                            Gérez le catalogue depuis IGDB ou manuellement.
                        </p>
                        <RoleBadge role={role} />
                    </div>
                </div>

                <div className="flex gap-1 p-1 bg-secondary-black border border-gray-800 rounded-xl mb-6">
                    {TABS.map(({ id, label, icon: Icon }) => (
                        <button key={id} onClick={() => setTab(id)}
                                className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg
                                            text-sm font-medium transition-all
                                            ${tab === id
                                    ? 'bg-primary-red text-white shadow-sm'
                                    : 'text-gray-400 hover:text-white'}`}>
                            <Icon className="w-3.5 h-3.5" />
                            <span className="hidden sm:inline">{label}</span>
                        </button>
                    ))}
                </div>

                <div className="bg-secondary-black border border-gray-800 rounded-2xl p-5 sm:p-6">
                    {tab === 'igdb'   && <TabIgdb />}
                    {tab === 'manuel' && <TabManuel />}
                    {tab === 'auto'   && <TabAuto role={role} />}
                </div>
            </div>
        </div>
    );
};