import { Link } from 'react-router-dom';
import { Gamepad2, Star, BookOpen, ArrowRight, Zap, Shield, Users } from 'lucide-react';
import { useAuth } from '../context/AuthContext.jsx';

const FEATURES = [
    {
        icon: Gamepad2,
        title: '1 000+ Jeux',
        desc: 'Catalogue complet alimenté par IGDB',
        color: 'text-primary-red',
        bg: 'bg-red-950',
    },
    {
        icon: Star,
        title: 'Notes & Avis',
        desc: 'Notez et partagez vos impressions',
        color: 'text-yellow-400',
        bg: 'bg-yellow-950',
    },
    {
        icon: BookOpen,
        title: 'Bibliothèque',
        desc: 'Suivez vos statuts de jeu',
        color: 'text-blue-400',
        bg: 'bg-blue-950',
    },
];

const STATS = [
    { label: 'Jeux indexés', value: '1 000+' },
    { label: 'Genres couverts', value: '30+' },
    { label: 'Plateformes', value: '15+' },
];

const STATUTS = [
    { label: 'À jouer', color: 'border-blue-500 text-blue-400' },
    { label: 'En cours', color: 'border-yellow-500 text-yellow-400' },
    { label: 'Terminé', color: 'border-green-500 text-green-400' },
    { label: 'Abandonné', color: 'border-gray-500 text-gray-400' },
];

export const HomePage = () => {
    const { isAuth } = useAuth();

    return (
        <div className="min-h-screen bg-primary-black">

            {/* Hero */}
            <section className="relative overflow-hidden">
                {/* Background glow */}
                <div className="absolute inset-0 pointer-events-none">
                    <div className="absolute top-[-10%] left-[20%] w-96 h-96 bg-red-900 opacity-20 rounded-full blur-3xl" />
                    <div className="absolute top-[30%] right-[10%] w-64 h-64 bg-red-800 opacity-10 rounded-full blur-3xl" />
                </div>

                <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 pb-24">
                    <div className="max-w-3xl">
                        {/* Badge */}
                        <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full
                            bg-red-950 border border-red-800 text-primary-red text-xs font-semibold
                            uppercase tracking-widest mb-6">
                            <Zap className="w-3 h-3" />
                            Powered by IGDB
                        </div>

                        <h1 className="text-5xl sm:text-6xl lg:text-7xl font-black text-white leading-none tracking-tight mb-6">
                            Votre univers<br />
                            <span className="text-primary-red">gaming</span>,<br />
                            organisé.
                        </h1>

                        <p className="text-lg text-gray-400 max-w-xl mb-10 leading-relaxed">
                            Découvrez des milliers de jeux, construisez votre bibliothèque personnelle,
                            notez et partagez vos avis avec la communauté.
                        </p>

                        <div className="flex flex-wrap gap-4">
                            {isAuth ? (
                                <Link
                                    to="/bibliotheque"
                                    className="inline-flex items-center gap-2 px-6 py-3 rounded-lg
                             bg-primary-red hover:bg-secondary-red text-white font-semibold
                             transition-all duration-200 hover:scale-105"
                                >
                                    Explorer le catalogue
                                    <ArrowRight className="w-4 h-4" />
                                </Link>
                            ) : (
                                <>
                                    <Link
                                        to="/inscription"
                                        className="inline-flex items-center gap-2 px-6 py-3 rounded-lg
                               bg-primary-red hover:bg-secondary-red text-white font-semibold
                               transition-all duration-200 hover:scale-105"
                                    >
                                        Commencer gratuitement
                                        <ArrowRight className="w-4 h-4" />
                                    </Link>
                                    <Link
                                        to="/bibliotheque"
                                        className="inline-flex items-center gap-2 px-6 py-3 rounded-lg
                               border border-gray-700 text-gray-300 font-semibold
                               hover:border-gray-500 hover:text-white transition-all duration-200"
                                    >
                                        Explorer le catalogue
                                    </Link>
                                </>
                            )}
                        </div>
                    </div>

                    {/* Stats bar */}
                    <div className="mt-16 flex flex-wrap gap-8">
                        {STATS.map(({ label, value }) => (
                            <div key={label}>
                                <p className="text-2xl font-black text-white">{value}</p>
                                <p className="text-sm text-gray-500 mt-0.5">{label}</p>
                            </div>
                        ))}
                    </div>
                </div>
            </section>

            {/* Features */}
            <section className="border-t border-gray-900">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
                    <h2 className="text-2xl font-bold text-white mb-10">
                        Tout ce dont vous avez besoin
                    </h2>
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
                        {FEATURES.map(({ icon: Icon, title, desc, color, bg }) => (
                            <div
                                key={title}
                                className="group p-6 rounded-xl border border-gray-800 bg-secondary-black
                           hover:border-gray-700 transition-all duration-200 hover:-translate-y-1"
                            >
                                <div className={`w-11 h-11 ${bg} rounded-lg flex items-center justify-center mb-4`}>
                                    <Icon className={`w-5 h-5 ${color}`} />
                                </div>
                                <h3 className="text-white font-semibold text-lg mb-1">{title}</h3>
                                <p className="text-gray-500 text-sm leading-relaxed">{desc}</p>
                            </div>
                        ))}
                    </div>
                </div>
            </section>

            {/* Statuts section */}
            <section className="border-t border-gray-900">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
                        <div>
                            <div className="inline-flex items-center gap-2 text-xs font-semibold uppercase
                              tracking-widest text-gray-500 mb-4">
                                <Shield className="w-3 h-3" />
                                Bibliothèque personnelle
                            </div>
                            <h2 className="text-3xl font-bold text-white mb-4">
                                Suivez chaque jeu à votre rythme
                            </h2>
                            <p className="text-gray-400 leading-relaxed mb-6">
                                Organisez votre collection avec 4 statuts distincts. Retrouvez en un coup d'œil
                                ce que vous jouez, ce qui vous attend, et ce que vous avez accompli.
                            </p>
                            {!isAuth && (
                                <Link
                                    to="/inscription"
                                    className="inline-flex items-center gap-2 text-primary-red font-semibold
                             hover:text-accent-red transition-colors text-sm"
                                >
                                    Créer mon compte
                                    <ArrowRight className="w-4 h-4" />
                                </Link>
                            )}
                        </div>
                        <div className="grid grid-cols-2 gap-3">
                            {STATUTS.map(({ label, color }) => (
                                <div
                                    key={label}
                                    className={`p-4 rounded-xl border ${color} bg-secondary-black
                              flex items-center gap-3`}
                                >
                                    <div className={`w-2 h-2 rounded-full border-2 ${color.split(' ')[0]}`} />
                                    <span className={`font-medium text-sm ${color.split(' ')[1]}`}>{label}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </section>

            {/* CTA final */}
            {!isAuth && (
                <section className="border-t border-gray-900">
                    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 text-center">
                        <Users className="w-8 h-8 text-primary-red mx-auto mb-4" />
                        <h2 className="text-3xl font-bold text-white mb-3">
                            Rejoignez la communauté
                        </h2>
                        <p className="text-gray-400 mb-8 max-w-md mx-auto">
                            Créez votre compte gratuitement et commencez à construire votre bibliothèque gaming.
                        </p>
                        <Link
                            to="/inscription"
                            className="inline-flex items-center gap-2 px-8 py-3 rounded-lg
                         bg-primary-red hover:bg-secondary-red text-white font-semibold
                         transition-all duration-200 hover:scale-105"
                        >
                            S'inscrire maintenant
                            <ArrowRight className="w-4 h-4" />
                        </Link>
                    </div>
                </section>
            )}
        </div>
    );
};