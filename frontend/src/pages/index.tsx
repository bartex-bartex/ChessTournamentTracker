import styles from "./index.module.css";

import Button from "../components/button";
import React, { useCallback, useContext, useEffect, useState } from "react";

import Context from "../context";
import { useNavigate } from 'react-router-dom';
import Input from "../components/input";
import { Chessboard } from "react-chessboard";
import { Chess } from "chess.js";

type Tournament = {
  id: number;
  date: string;
  name: string;
  timeControl: string;
  place: string;
};

export default function Home() {
  const { signedInUser } = useContext(Context);
  const [tournaments, setTournaments] = useState<Tournament[]>([]);
  const [showingSearchResults, setShowingSearchResults] = useState(false);
  const navigate = useNavigate();
  const [topGameWhiteName, setTopGameWhiteName] = useState('');
  const [topGameBlackName, setTopGameBlackName] = useState('');
  const [topGameFen, setTopGameFen] = useState('');
  const [dailyPuzzleFen, setDailyPuzzleFen] = useState('');
  const [dailyPuzzlePlayer, setDailyPuzzlePlayer] = useState('');

  const fetchRecentTournaments = useCallback(async () => {
    try {
      const response = await fetch('/api/homepage');

      if (!response.ok) {
        alert("Failed to fetch tournaments: " + await response.text());
      }

      const body = await response.json();
      setTournaments(body.tournaments.map((tournament: any) => ({
        id: tournament.tournament_id,
        date: tournament.start_date,
        name: tournament.name,
        timeControl: tournament.time_control,
        place: tournament.location,
      })));
    } catch (err) {
      alert("Failed to fetch tournaments: " + err);
    }
  }, []);

  const fetchDailyPuzzle = useCallback(async () => {
    try {
      const response = await fetch('https://lichess.org/api/puzzle/daily');

      if (!response.ok) {
        alert("Failed to fetch daily puzzle: " + await response.text());
      }

      const body = await response.json();
      const chess = new Chess();
      chess.loadPgn(body.game.pgn);
      if (dailyPuzzleFen === '') {
        setDailyPuzzleFen(chess.fen());
        setDailyPuzzlePlayer(chess.turn() === 'w' ? 'White' : 'Black');
      }
    } catch (err) {
      alert("Failed to fetch daily puzzle: " + err);
    }
  }, [setDailyPuzzleFen]);

  useEffect(() => {
    fetchRecentTournaments();
    setTimeout(() => {
      fetchDailyPuzzle();
    }, 0);

    // function to process incoming JSON objects in the top game stream
    function processJsonData(jsonData: any) {
      // Update the top game chessboard
      if (jsonData.t == 'featured') {
        const game = jsonData.d;
        setTopGameWhiteName(game.players.find((player: any) => player.color === 'white').user.name);
        setTopGameBlackName(game.players.find((player: any) => player.color === 'black').user.name);
      } else if (jsonData.t == 'fen') {
        const game = jsonData.d;
        setTopGameFen(game.fen);
      }
    }

    // Create abort controller (so we can cancel the fetch request)
    const abortController = new AbortController();
    let isMounted = true;

    // Connect to top game stream at GET:https://lichess.org/api/tv/feed
    fetch("https://lichess.org/api/tv/feed", { signal: abortController.signal })
      .then((res) => {
        // Check if the response status is OK (200)
        if (!res.ok) {
          throw new Error(`Request failed with status code ${res.status}`);
        }

        // Create a readable stream from the response body
        const reader = res.body!.getReader();

        // Variable to store incoming data chunks
        let accumulatedData = '';
        let braceCount = 0;

        // Function to process each chunk of data
        const processChunk = ({ done, value }: { done: any; value: any }) => {
          // Check if the component is still mounted
          if (!isMounted) {
            console.log('Component is unmounted. Aborting.');
            abortController.abort();
            return;
          }

          // Check if the stream is done
          if (done) {
            console.log('Stream ended');
            return;
          }

          // Accumulate the data chunk
          for (let i = 0; i < value.length; i++) {
            // If braceCount == 0 and we encounter a non-visual character, skip it
            if (braceCount === 0 && (value[i] === 32 || value[i] === 10)) {
              // space or newline
              continue;
            }

            accumulatedData += String.fromCharCode(value[i]);
            if (value[i] === 123) {
              // {
              braceCount++;
            }
            if (value[i] === 125) {
              // }
              braceCount--;
            }
            if (braceCount === 0) {
              processJsonData(JSON.parse(accumulatedData));
              accumulatedData = '';
            }
          }

          // Check if the component is still mounted before initiating the next read
          if (isMounted) {
            reader.read().then(processChunk as any);
          }
        };

        // Start reading the stream
        reader.read().then(processChunk as any);
      })
      .catch((err) => {
        console.error(`Request error: ${err.message}`);
      });

    return () => {
      // Update the flag when the component is unmounted
      isMounted = false;
    };
  }, []);


  return (
    <div className={styles["home"]}>
      <div className={styles["tournaments-table-container"]}>
        <div className={styles["chessboards"]}>
          {dailyPuzzleFen && <div className={styles["chessboards-item"]}>
            <h4>Daily Puzzle: {dailyPuzzlePlayer} to move</h4>
            <div className={styles["chessboard-container"]}>
              <Chessboard id="daily-puzzle" position={dailyPuzzleFen} />
            </div>
          </div>}
          {topGameFen && <div className={styles["chessboards-item"]}>
            <h4>Top Game: {topGameWhiteName} vs {topGameBlackName}</h4>
            <div className={styles["chessboard-container"]}>
              <Chessboard id="top-game" position={topGameFen} arePiecesDraggable={false} />
            </div>
          </div>}
        </div>
        <div className={styles["tournaments-above-table"]}>
          <h2>{showingSearchResults ? "Search Results" : "Recent Tournaments"}</h2>
          {signedInUser !== '' &&
            <Button
              className="create-button"
              text="Create Tournament"
              onClick={() => {
                navigate("/create");
              }}
            />
          }
        </div>
        <Input type="text" placeholder="Type to search all tournaments..." onChange={async e => {
          const query = e.target.value;

          // If query is empty, show recent tournaments
          if (query === '') {
            await fetchRecentTournaments();
            setShowingSearchResults(false);
            return;
          }

          const response = await fetch(`/api/search/${encodeURIComponent(query)}`);
          const body = await response.json();
          setTournaments(body.map((tournament: any) => ({
            id: tournament.tournament_id,
            date: tournament.start_date,
            name: tournament.name,
            timeControl: tournament.time_control,
            place: tournament.location,
          })));
          setShowingSearchResults(true);
        }} />
        <h4>Click for details.</h4>
        <table className={styles["tournaments-table"]}>
          <thead>
            <tr>
              <th>Date</th>
              <th>Name</th>
              <th>Time Control</th>
              <th>Place</th>
            </tr>
          </thead>
          <tbody>
            {tournaments.map((tournament) => (
              <tr key={tournament.name} onClick={() => navigate(`/tournament/${tournament.id}`)}>
                <td>{tournament.date}</td>
                <td>{tournament.name}</td>
                <td>{tournament.timeControl}</td>
                <td>{tournament.place}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div >
  );
}
