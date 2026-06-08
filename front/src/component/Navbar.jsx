import { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import {
	Gamepad2,
	Library,
	BookMarked,
	LogIn,
	UserPlus,
	LogOut,
	Menu,
	X,
	User,
	ChevronDown,
	Download,
	Flag,
} from 'lucide-react';
import { avatar as avatarApi } from '../api.js';

const NAV_LINKS = [
	{ href: '/bibliotheque', label: 'Catalogue', icon: Library },
];

const AUTH_LINKS = [
	{ href: '/ma_bibliotheque', label: 'Ma Bibliothèque', icon: BookMarked },
];

const IMPORT_ROLES = ['POSTER', 'ADMIN', 'SUPERADMIN'];
const ADMIN_ROLES  = ['ADMIN', 'SUPERADMIN'];

function getRoleFromToken() {
	const token = localStorage.getItem('token');
	if (!token) return null;
	try {
		const payload = JSON.parse(atob(token.split('.')[1]));
		return payload.role || null;
	} catch {
		return null;
	}
}

export const Navbar = () => {
	const { pathname } = useLocation();
	const navigate = useNavigate();
	const { user, isAuth, logout } = useAuth();
	const [mobileOpen, setMobileOpen] = useState(false);
	const [userMenuOpen, setUserMenuOpen] = useState(false);
	const [avatarUrl, setAvatarUrl] = useState(null);

	const role      = isAuth ? getRoleFromToken() : null;
	const canImport = role && IMPORT_ROLES.includes(role);
	const isAdmin   = role && ADMIN_ROLES.includes(role);

	useEffect(() => {
		if (!isAuth || !user) { setAvatarUrl(null); return; }
		avatarApi.get(user.id)
			.then((data) => { if (data?.url) setAvatarUrl(data.url); })
			.catch(() => setAvatarUrl(null));
	}, [isAuth, user]);

	const handleLogout = () => {
		logout();
		setAvatarUrl(null);
		navigate('/');
		setMobileOpen(false);
		setUserMenuOpen(false);
	};

	const isActive = (href) => pathname === href || pathname.startsWith(href + '/');

	return (
		<nav className="sticky top-0 z-50 bg-secondary-black border-b border-gray-800">
			<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
				<div className="flex items-center justify-between h-16">

					{/* Logo */}
					<Link
						to="/"
						className="flex items-center gap-2 group"
						onClick={() => setMobileOpen(false)}
					>
						<div className="w-9 h-9 bg-primary-red rounded-lg flex items-center justify-center
                                        group-hover:bg-secondary-red transition-colors duration-200">
							<Gamepad2 className="w-5 h-5 text-white" />
						</div>
						<span className="text-white font-bold text-xl tracking-tight">
							Game<span className="text-primary-red">Lib</span>
						</span>
					</Link>

					{/* Desktop nav */}
					<div className="hidden md:flex items-center gap-1">
						{NAV_LINKS.map(({ href, label, icon: Icon }) => (
							<Link
								key={href}
								to={href}
								className={`flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-all duration-200
                                    ${isActive(href) && pathname !== '/'
									? 'bg-primary-red text-white'
									: 'text-gray-400 hover:text-white hover:bg-gray-800'
								}`}
							>
								<Icon className="w-4 h-4" />
								{label}
							</Link>
						))}

						{isAuth && AUTH_LINKS.map(({ href, label, icon: Icon }) => (
							<Link
								key={href}
								to={href}
								className={`flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-all duration-200
                                    ${isActive(href)
									? 'bg-primary-red text-white'
									: 'text-gray-400 hover:text-white hover:bg-gray-800'
								}`}
							>
								<Icon className="w-4 h-4" />
								{label}
							</Link>
						))}

						{isAuth && canImport && (
							<Link
								to="/import"
								className={`flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-all duration-200
                                    ${isActive('/import')
									? 'bg-primary-red text-white'
									: 'text-gray-400 hover:text-white hover:bg-gray-800'
								}`}
							>
								<Download className="w-4 h-4" />
								Importer
							</Link>
						)}
					</div>

					{/* Desktop auth */}
					<div className="hidden md:flex items-center gap-2">
						{isAuth ? (
							<div className="relative">
								<button
									onClick={() => setUserMenuOpen(!userMenuOpen)}
									className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-gray-800
                                               hover:bg-gray-700 text-gray-300 text-sm transition-colors"
								>
									<div className="w-6 h-6 rounded-full bg-accent-black border border-gray-600
                                                    flex items-center justify-center overflow-hidden flex-shrink-0">
										{avatarUrl ? (
											<img src={avatarUrl} alt="avatar" className="w-full h-full object-cover" />
										) : (
											<span className="text-xs font-bold text-gray-400">
												{user?.pseudo?.charAt(0).toUpperCase()}
											</span>
										)}
									</div>
									<span className="font-medium">{user?.pseudo}</span>
									<ChevronDown className={`w-3.5 h-3.5 transition-transform ${userMenuOpen ? 'rotate-180' : ''}`} />
								</button>

								{userMenuOpen && (
									<div className="absolute right-0 top-full mt-1 w-48 bg-secondary-black
                                                    border border-gray-700 rounded-xl overflow-hidden shadow-xl z-50">
										<Link
											to="/mon-profil"
											onClick={() => setUserMenuOpen(false)}
											className="flex items-center gap-2.5 px-4 py-2.5 text-sm text-gray-300
                                                       hover:bg-gray-800 hover:text-white transition-colors"
										>
											<User className="w-4 h-4" />
											Mon profil
										</Link>

										{canImport && (
											<Link
												to="/import"
												onClick={() => setUserMenuOpen(false)}
												className="flex items-center gap-2.5 px-4 py-2.5 text-sm text-gray-300
                                                           hover:bg-gray-800 hover:text-white transition-colors"
											>
												<Download className="w-4 h-4" />
												Importer un jeu
											</Link>
										)}

										{isAdmin && (
											<Link
												to="/admin/reports"
												onClick={() => setUserMenuOpen(false)}
												className={`flex items-center gap-2.5 px-4 py-2.5 text-sm transition-colors
                                                    ${isActive('/admin/reports')
													? 'bg-red-950 text-primary-red'
													: 'text-gray-300 hover:bg-gray-800 hover:text-white'
												}`}
											>
												<Flag className="w-4 h-4" />
												Signalements
											</Link>
										)}

										<div className="border-t border-gray-800" />
										<button
											onClick={handleLogout}
											className="w-full flex items-center gap-2.5 px-4 py-2.5 text-sm
                                                       text-gray-400 hover:bg-gray-800 hover:text-red-400 transition-colors"
										>
											<LogOut className="w-4 h-4" />
											Déconnexion
										</button>
									</div>
								)}
							</div>
						) : (
							<>
								<Link
									to="/connexion"
									className={`flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-all duration-200
                                        ${isActive('/connexion')
										? 'bg-gray-700 text-white'
										: 'text-gray-400 hover:text-white hover:bg-gray-800'
									}`}
								>
									<LogIn className="w-4 h-4" />
									Connexion
								</Link>
								<Link
									to="/inscription"
									className="flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium
                                               bg-primary-red text-white hover:bg-secondary-red transition-all duration-200"
								>
									<UserPlus className="w-4 h-4" />
									S'inscrire
								</Link>
							</>
						)}
					</div>

					{/* Mobile toggle */}
					<button
						className="md:hidden p-2 rounded-md text-gray-400 hover:text-white hover:bg-gray-800 transition-colors"
						onClick={() => setMobileOpen(!mobileOpen)}
					>
						{mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
					</button>
				</div>
			</div>

			{/* Mobile menu */}
			{mobileOpen && (
				<div className="md:hidden border-t border-gray-800 bg-secondary-black">
					<div className="px-4 py-3 space-y-1">
						{NAV_LINKS.map(({ href, label, icon: Icon }) => (
							<Link
								key={href}
								to={href}
								onClick={() => setMobileOpen(false)}
								className={`flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium transition-colors
                                    ${isActive(href) && pathname !== '/'
									? 'bg-primary-red text-white'
									: 'text-gray-400 hover:text-white hover:bg-gray-800'
								}`}
							>
								<Icon className="w-4 h-4" />
								{label}
							</Link>
						))}

						{isAuth && AUTH_LINKS.map(({ href, label, icon: Icon }) => (
							<Link
								key={href}
								to={href}
								onClick={() => setMobileOpen(false)}
								className={`flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium transition-colors
                                    ${isActive(href)
									? 'bg-primary-red text-white'
									: 'text-gray-400 hover:text-white hover:bg-gray-800'
								}`}
							>
								<Icon className="w-4 h-4" />
								{label}
							</Link>
						))}

						{isAuth && canImport && (
							<Link
								to="/import"
								onClick={() => setMobileOpen(false)}
								className={`flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium transition-colors
                                    ${isActive('/import')
									? 'bg-primary-red text-white'
									: 'text-gray-400 hover:text-white hover:bg-gray-800'
								}`}
							>
								<Download className="w-4 h-4" />
								Importer
							</Link>
						)}

						{isAuth && isAdmin && (
							<Link
								to="/admin/reports"
								onClick={() => setMobileOpen(false)}
								className={`flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium transition-colors
                                    ${isActive('/admin/reports')
									? 'bg-primary-red text-white'
									: 'text-gray-400 hover:text-white hover:bg-gray-800'
								}`}
							>
								<Flag className="w-4 h-4" />
								Signalements
							</Link>
						)}

						<div className="pt-2 border-t border-gray-800 space-y-1">
							{isAuth ? (
								<>
									<div className="flex items-center gap-3 px-3 py-2 text-gray-300 text-sm">
										<div className="w-7 h-7 rounded-full bg-accent-black border border-gray-600
                                                        flex items-center justify-center overflow-hidden flex-shrink-0">
											{avatarUrl ? (
												<img src={avatarUrl} alt="avatar" className="w-full h-full object-cover" />
											) : (
												<span className="text-xs font-bold text-gray-400">
													{user?.pseudo?.charAt(0).toUpperCase()}
												</span>
											)}
										</div>
										<span className="font-medium">{user?.pseudo}</span>
									</div>
									<Link
										to="/mon-profil"
										onClick={() => setMobileOpen(false)}
										className="flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium
                                                   text-gray-400 hover:text-white hover:bg-gray-800 transition-colors"
									>
										<User className="w-4 h-4" />
										Mon profil
									</Link>
									<button
										onClick={handleLogout}
										className="w-full flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium
                                                   text-gray-400 hover:text-white hover:bg-gray-800 transition-colors"
									>
										<LogOut className="w-4 h-4" />
										Déconnexion
									</button>
								</>
							) : (
								<>
									<Link
										to="/connexion"
										onClick={() => setMobileOpen(false)}
										className="flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium
                                                   text-gray-400 hover:text-white hover:bg-gray-800 transition-colors"
									>
										<LogIn className="w-4 h-4" />
										Connexion
									</Link>
									<Link
										to="/inscription"
										onClick={() => setMobileOpen(false)}
										className="flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium
                                                   bg-primary-red text-white hover:bg-secondary-red transition-colors"
									>
										<UserPlus className="w-4 h-4" />
										S'inscrire
									</Link>
								</>
							)}
						</div>
					</div>
				</div>
			)}
		</nav>
	);
};