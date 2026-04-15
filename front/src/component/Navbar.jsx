import { HeaderItem } from "./HeaderItem";

export const Navbar = () => {

	return (
		<div className="navbar bg-secondary-black border-b-2 border-primary-red">
			<nav>
				<div className="container mx-auto px-4">
					<ul className="flex flex-row items-center justify-between h-16 space-x-8">
						<li>
							<HeaderItem href="/">Accueil</HeaderItem>
						</li>
						<li>
							<HeaderItem href="/bibliotheque">Bibliotheque</HeaderItem>
						</li>
						<li>
							<HeaderItem href="/ma_bibliotheque">Ma bibliotheque</HeaderItem>
						</li>
						<li>
							<HeaderItem href="/connexion">Connexion</HeaderItem>
						</li>
					</ul>
				</div>
			</nav>
		</div>
	);
};
