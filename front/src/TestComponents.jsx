import { useState } from 'react'
import { User, Mail, Lock, Eye, EyeOff, Menu, X, ChevronDown, Search, ShoppingCart, Heart, Star, AlertCircle, CheckCircle, Info } from 'lucide-react'
import './input.css'

function TestComponents() {
  const [showPassword, setShowPassword] = useState(false)
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    message: ''
  })
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [cartCount, setCartCount] = useState(0)

  const handleInputChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    })
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    console.log('Form submitted:', formData)
  }

  return (
    <div className="min-h-screen bg-gray-50 p-4 md:p-8">
      {/* Header responsive avec menu mobile */}
      <header className="bg-white shadow-sm rounded-lg mb-8">
        <nav className="flex justify-between items-center p-4">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold">T</span>
            </div>
            <span className="text-xl font-semibold">TestApp</span>
          </div>
          
          {/* Desktop menu */}
          <div className="hidden md:flex items-center space-x-6">
            <a href="#" className="text-gray-600 hover:text-blue-600 transition-colors">Accueil</a>
            <a href="#" className="text-gray-600 hover:text-blue-600 transition-colors">Produits</a>
            <a href="#" className="text-gray-600 hover:text-blue-600 transition-colors">Services</a>
            <a href="#" className="text-gray-600 hover:text-blue-600 transition-colors">Contact</a>
          </div>

          <div className="flex items-center space-x-4">
            {/* Search bar responsive */}
            <div className="hidden sm:flex items-center bg-gray-100 rounded-lg px-3 py-2">
              <Search className="w-4 h-4 text-gray-500 mr-2" />
              <input 
                type="text" 
                placeholder="Rechercher..." 
                className="bg-transparent outline-none text-sm w-32 md:w-48"
              />
            </div>

            {/* Cart avec animation */}
            <button className="relative p-2 hover:bg-gray-100 rounded-lg transition-colors">
              <ShoppingCart className="w-5 h-5 text-gray-700" />
              {cartCount > 0 && (
                <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center animate-pulse">
                  {cartCount}
                </span>
              )}
            </button>

            {/* Mobile menu toggle */}
            <button 
              className="md:hidden p-2 hover:bg-gray-100 rounded-lg transition-colors"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </nav>

        {/* Mobile menu */}
        {mobileMenuOpen && (
          <div className="md:hidden border-t border-gray-200 p-4 space-y-2 animate-slideDown">
            <a href="#" className="block py-2 text-gray-600 hover:text-blue-600 transition-colors">Accueil</a>
            <a href="#" className="block py-2 text-gray-600 hover:text-blue-600 transition-colors">Produits</a>
            <a href="#" className="block py-2 text-gray-600 hover:text-blue-600 transition-colors">Services</a>
            <a href="#" className="block py-2 text-gray-600 hover:text-blue-600 transition-colors">Contact</a>
          </div>
        )}
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Formulaire stylisé */}
        <section className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-2xl font-bold mb-6 flex items-center">
            <User className="w-6 h-6 mr-2 text-blue-600" />
            Formulaire Stylisé
          </h2>
          
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Nom complet
              </label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  className="input pl-10"
                  placeholder="Jean Dupont"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Email
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  className="input pl-10"
                  placeholder="jean@exemple.com"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Mot de passe
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type={showPassword ? "text" : "password"}
                  name="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  className="input pl-10 pr-10"
                  placeholder="Mot de passe sécurisé"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Message
              </label>
              <textarea
                name="message"
                value={formData.message}
                onChange={handleInputChange}
                className="input resize-none"
                rows="4"
                placeholder="Votre message..."
              />
            </div>

            <div className="flex space-x-3">
              <button type="submit" className="btn btn-primary flex-1">
                Envoyer
              </button>
              <button type="button" className="btn btn-secondary flex-1">
                Annuler
              </button>
            </div>
          </form>
        </section>

        {/* Cartes avec animations */}
        <section className="space-y-6">
          <h2 className="text-2xl font-bold mb-6">Animations & Composants</h2>
          
          {/* Alertes avec animations */}
          <div className="space-y-3">
            <div className="flex items-center p-3 bg-green-50 border border-green-200 rounded-lg animate-fadeIn">
              <CheckCircle className="w-5 h-5 text-green-600 mr-2" />
              <span className="text-green-800">Succès : Opération complétée</span>
            </div>
            
            <div className="flex items-center p-3 bg-yellow-50 border border-yellow-200 rounded-lg animate-fadeIn">
              <AlertCircle className="w-5 h-5 text-yellow-600 mr-2" />
              <span className="text-yellow-800">Attention : Vérifiez les informations</span>
            </div>
            
            <div className="flex items-center p-3 bg-blue-50 border border-blue-200 rounded-lg animate-fadeIn">
              <Info className="w-5 h-5 text-blue-600 mr-2" />
              <span className="text-blue-800">Information : Nouvelle mise à jour</span>
            </div>
          </div>

          {/* Cards avec hover effects */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="card p-4 hover:shadow-lg transition-shadow duration-300 hover:scale-105 transform">
              <div className="flex items-center mb-3">
                <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center mr-3">
                  <Star className="w-5 h-5 text-blue-600" />
                </div>
                <h3 className="font-semibold">Premium</h3>
              </div>
              <p className="text-gray-600 text-sm mb-3">Accès complet à toutes les fonctionnalités</p>
              <button className="btn btn-primary w-full text-sm">
                Choisir
              </button>
            </div>

            <div className="card p-4 hover:shadow-lg transition-shadow duration-300 hover:scale-105 transform">
              <div className="flex items-center mb-3">
                <div className="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center mr-3">
                  <Heart className="w-5 h-5 text-gray-600" />
                </div>
                <h3 className="font-semibold">Gratuit</h3>
              </div>
              <p className="text-gray-600 text-sm mb-3">Fonctionnalités de base</p>
              <button className="btn btn-outline w-full text-sm">
                Commencer
              </button>
            </div>
          </div>

          {/* Boutons avec animations */}
          <div className="card p-4">
            <h3 className="font-semibold mb-3">Variations de boutons</h3>
            <div className="grid grid-cols-2 gap-3">
              <button className="btn btn-primary hover:animate-bounce">
                Primaire
              </button>
              <button className="btn btn-secondary">
                Secondaire
              </button>
              <button className="btn btn-outline">
                Outline
              </button>
              <button className="btn btn-primary opacity-50 cursor-not-allowed" disabled>
                Désactivé
              </button>
            </div>
          </div>
        </section>
      </div>

      {/* Footer responsive */}
      <footer className="mt-12 bg-white rounded-xl shadow-sm p-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <h4 className="font-semibold mb-3">À propos</h4>
            <p className="text-gray-600 text-sm">
              Test des composants TailwindCSS avec Lucide Icons et animations CSS
            </p>
          </div>
          <div>
            <h4 className="font-semibold mb-3">Liens rapides</h4>
            <ul className="space-y-2 text-sm">
              <li><a href="#" className="text-gray-600 hover:text-blue-600 transition-colors">Documentation</a></li>
              <li><a href="#" className="text-gray-600 hover:text-blue-600 transition-colors">GitHub</a></li>
              <li><a href="#" className="text-gray-600 hover:text-blue-600 transition-colors">Support</a></li>
            </ul>
          </div>
          <div>
            <h4 className="font-semibold mb-3">Contact</h4>
            <div className="space-y-2 text-sm text-gray-600">
              <p>email@exemple.com</p>
              <p>+33 1 23 45 67 89</p>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default TestComponents
