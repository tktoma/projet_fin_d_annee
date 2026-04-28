import { useState } from 'react';
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
} from 'lucide-react';

const NAV_LINKS = [
	{ href: '/bibliotheque', label: 'Catalogue', icon: Library },
];

const AUTH_LINKS = [
	{ href: '/ma_bibliotheque', label: 'Ma Bibliothèque', icon: BookMarked },
];

export const Navbar = () => {
	const { pathname } = useLocation();
	const navigate = useNavigate();
	const { user, isAuth, logout } = useAuth();
	const [mobileOpen, setMobileOpen] = useState(false);

	const handleLogout = () => {
		logout();
		navigate('/');
		setMobileOpen(false);
	};

	const isActive = (href) => pathname === href;

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
                  ${isActive(href)
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
					</div>

					{/* Desktop auth */}
					<div className="hidden md:flex items-center gap-2">
						{isAuth ? (
							<>
								<div className="flex items-center gap-2 px-3 py-1.5 rounded-md bg-gray-800 text-gray-300 text-sm">
									<User className="w-3.5 h-3.5 text-primary-red" />
									<span className="font-medium">{user?.pseudo}</span>
								</div>
								<button
									onClick={handleLogout}
									className="flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium
                             text-gray-400 hover:text-white hover:bg-gray-800 transition-all duration-200"
								>
									<LogOut className="w-4 h-4" />
									Déconnexion
								</button>
							</>
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
									className={`flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-all duration-200
                    ${isActive('/inscription')
										? 'bg-primary-red text-white'
										: 'bg-primary-red text-white hover:bg-secondary-red'
									}`}
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
                  ${isActive(href)
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

						<div className="pt-2 border-t border-gray-800 space-y-1">
							{isAuth ? (
								<>
									<div className="flex items-center gap-2 px-3 py-2 text-gray-300 text-sm">
										<User className="w-4 h-4 text-primary-red" />
										<span className="font-medium">{user?.pseudo}</span>
									</div>
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