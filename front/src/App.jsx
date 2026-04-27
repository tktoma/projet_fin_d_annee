import { createBrowserRouter, Outlet, RouterProvider } from "react-router-dom";
import './App.css'
import {HomePage} from "./pages/Home.jsx";
import {Navbar} from "./component/Navbar.jsx";
import {Bibliotheque} from "./pages/Bibliotheque.jsx";
import {MaBibliotheque} from "./pages/MaBibliotheque.jsx";
import {Connexion} from "./pages/Connexion.jsx";
import {Inscription} from "./pages/Inscription.jsx";


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
        { path: "inscription", element: <Inscription />},
      ],
    },
  ]);

  function Root() {
    return (
        <div className="bg-primary-black text-white">
          <Navbar />
          <div className="app-container w-full">
            <Outlet />
          </div>
        </div>
    );
  }

  return <RouterProvider router={router} />;
}

export default App;
