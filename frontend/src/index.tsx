import "./index.css";

import React, { useEffect, useState } from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";

import Navbar from "./components/navbar";
import Home from "./pages";
import Login from "./pages/login";
import Register from "./pages/register";
import User from "./pages/user";
import Create from "./pages/create";
import Tournament from "./pages/tournament";
import TournamentParticipants from "./pages/tournament/participants";
import TournamentRanking from "./pages/tournament/ranking";
import TournamentResults from "./pages/tournament/results";
import TournamentRound from "./pages/tournament/round";
import TournamentGame from "./pages/tournament/game";
import NotFound from "./pages/not-found";

import Context from "./context";
import OtherUser from "./pages/other-user";

function App() {
  const [signedInUser, setSignedInUser] = useState("");

  // Check if user is signed in
  useEffect(() => {
    (async () => {
      try {
        const response = await fetch('/api/user');
        if (!response.ok) {
          setSignedInUser("");
          return;
        }

        const user = (await response.json());
        setSignedInUser(user.username);
      } catch (err) {
        setSignedInUser("");
      }
    })();
  }, []);

  return (
    <div className="app">
      <Context.Provider value={{ signedInUser, setSignedInUser }}>
        <Router>
          <Navbar />
          <Routes>
            <Route path="/" Component={Home} />
            <Route path="/login" Component={Login} />
            <Route path="/register" Component={Register} />
            <Route path="/user" Component={User} />
            <Route path="/other-user/:id" Component={OtherUser} />
            <Route path="/create" Component={Create} />
            <Route path="/tournament/:id" Component={Tournament} />
            <Route
              path="/tournament/:id/participants"
              Component={TournamentParticipants}
            />
            <Route
              path="/tournament/:id/ranking"
              Component={TournamentRanking}
            />
            <Route
              path="/tournament/:id/results"
              Component={TournamentResults}
            />
            <Route
              path="/tournament/:tournamentId/round/:round"
              Component={TournamentRound}
            />
            <Route
              path="/tournament/:tournamentId/game/:matchId"
              Component={TournamentGame}
            />
            <Route path="*" Component={NotFound} />
          </Routes>
        </Router>
      </Context.Provider>
    </div>
  );
}

const root = ReactDOM.createRoot(
  document.getElementById("root") as HTMLElement
);

root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
