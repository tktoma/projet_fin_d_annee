export const Inscription = () => {
    return (
        <div>
            <div>
                <fieldset>
                    <h2>Inscription</h2>
                    <div>
                        <div>
                            <label>Nom</label>
                            <input type="text" className="input placeholder-gray-400" placeholder="Nom" required />
                        </div>
                        <div>
                            <label>Prénom</label>
                            <input type="text" className="input placeholder-gray-400" placeholder="Prénom" required />
                        </div>

                        <div>
                            <label>Email</label>
                            <input type="email" className="input placeholder-gray-400" placeholder="Email" required />
                        </div>

                        <div>
                            <label>Mot de passe</label>
                            <input type="password" className="input placeholder-gray-400" placeholder="Mot de passe" required />
                        </div>

                        <div>
                            <label>Confirmation du mot de passe</label>
                            <input type="password" className="input placeholder-gray-400" placeholder="Confirmation du mot de passe" required />
                        </div>
                    </div>
                    <div>
                        <button>
                            Connexion
                        </button>
                        <button>
                            S'inscrire
                        </button>
                    </div>
                </fieldset>
            </div>
        </div>
    );
};