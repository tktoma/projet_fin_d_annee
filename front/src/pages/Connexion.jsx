export const Connexion = () => {
    return (
        <div>
            <div>
                <fieldset>
                    <h2>Connexion</h2>

                    <div>
                        <div>
                            <label>Email</label>
                            <input type="email" className="input placeholder-gray-400" placeholder="Email" required />
                        </div>

                        <div>
                            <label>Mot de passe</label>
                            <input type="password" className="input placeholder-gray-400" placeholder="Mot de passe" required />
                        </div>
                    </div>

                    <div>
                        <button className>
                            Connexion
                        </button>
                    </div>
                </fieldset>
            </div>
        </div>
    );
};
