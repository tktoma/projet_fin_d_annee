import { createBrowserRouter, Outlet, RouterProvider } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext.jsx';
import { Navbar } from './component/Navbar.jsx';
import { HomePage } from './pages/Home.jsx';
import { Bibliotheque } from './pages/Bibliotheque.jsx';
import { MaBibliotheque } from './pages/MaBibliotheque.jsx';
import { Connexion } from './pages/Connexion.jsx';
import { Inscription } from './pages/Inscription.jsx';

function Root() {
  return (
      <div className="bg-primary-black min-h-screen">
        <Navbar />
        <main>
          <Outlet />
        </main>
      </div>
  );
}

const router = createBrowserRouter([
  {
    path: '',
    element: <Root />,
    children: [
      { path: '', element: <HomePage /> },
      { path: 'bibliotheque', element: <Bibliotheque /> },
      { path: 'ma_bibliotheque', element: <MaBibliotheque /> },
      { path: 'connexion', element: <Connexion /> },
      { path: 'inscription', element: <Inscription /> },
    ],
  },
]);

function App() {
  return (
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
  );
}

export default App;