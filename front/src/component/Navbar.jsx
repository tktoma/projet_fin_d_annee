import { HeaderItem } from "./HeaderItem";

export const Navbar = () => {

	return (
		<div>
			<nav>
				<div>
					<ul>
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
						<li>
							<HeaderItem href="/inscription">Inscription</HeaderItem>
						</li>
					</ul>
				</div>
			</nav>
		</div>
	);
};
