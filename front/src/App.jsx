import { createBrowserRouter, Outlet, RouterProvider } from "react-router-dom";
import './App.css'
import {HomePage} from "./pages/Home.jsx";
import {Navbar} from "./component/Navbar.jsx";
import {Bibliotheque} from "./pages/Bibliotheque.jsx";
import {MaBibliotheque} from "./pages/MaBibliotheque.jsx";
import {Connexion} from "./pages/Connexion.jsx";


function App() {
  const router = createBrowserRouter([
    {
      path: "",
      element: <Root />,
      children: [
        { path: "", element: <HomePage /> },
        { path: "bibliotheque", element: <Bibliotheque /> },
        { path: "ma_bibliotheque", element: <MaBibliotheque /> },
        { path: "connexion", element: <Connexion /> },
      ],
    },
  ]);

  function Root() {
    return (
        <div className="min-h-screen bg-primary-black text-white">
          <Navbar />
          <div className="app-container">
            <Outlet />
          </div>
        </div>
    );
  }

  return <RouterProvider router={router} />;
}

export default App;
