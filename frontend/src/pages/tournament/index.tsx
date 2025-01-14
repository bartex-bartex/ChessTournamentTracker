import styles from "./index.module.css";

import React, { useCallback, useContext, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Button from "../../components/button";
import TournamentNavbar from "../../components/tournament-navbar";
import Context from "../../context";
import Input from "../../components/input";

export default function Tournament() {
  const { id } = useParams();
  const [info, setInfo] = useState<any>(null);
  const navigate = useNavigate();
  const context = useContext(Context);
  const newNumberOfRounds = useRef(0);

  const loadTournamentInfo = useCallback(async () => {
    try {
      const response = await fetch(process.env.REACT_APP_BACKEND_URL + '/api/tournament/' + id, { credentials: "include"});

      if (!response.ok) {
        alert("Failed to fetch tournament details: " + await response.text());
      }
      const body = await response.json();
      setInfo(body);
    } catch (err) {
      setInfo(null);
    }
  }, [id]);

  useEffect(() => {
    loadTournamentInfo();
  }, [id]);

  return (
    <div className={styles["tournament"]}>
      <TournamentNavbar tournamentInfo={info!} />
      <div className={styles["tournament-container"]}>
        <h1>{info ? info.name : ""}</h1>
        <p>{info ? info.info : ""}</p>
        <div className={styles["tournament-details"]}>
          <h4>Tournament Details</h4>
          <table className={styles["tournament-details-table"]}>
            <tbody>
              <tr>
                <td>Location</td>
                <td>{info ? info.location : "Loading..."}</td>
              </tr>
              <tr>
                <td>Start Date</td>
                <td>{info ? info.start_date : "Loading..."}</td>
              </tr>
              <tr>
                <td>End Date</td>
                <td>{info ? info.end_date : "Loading..."}</td>
              </tr>
              <tr>
                <td>Organizer</td>
                <td>{info ? info.organiser : "Loading..."}</td>
              </tr>
              <tr>
                <td>Time Control</td>
                <td>{info ? info.time_control : "Loading..."}</td>
              </tr>
              <tr>
                <td>Number of Rounds</td>
                <td>{info ? info.rounds : "Loading..."}</td>
              </tr>
              <tr>
                <td>Number of Players</td>
                <td>{info ? (info.player_data ? info.player_data.length : info.players.length) : "Loading..."}</td>
              </tr>
            </tbody>
          </table>
        </div>
        {/* Only God knows what's below there */}
        {/* Honestly would tidy it up into proper conditional logic, but I am too afraid of introducing new bugs into the codebase. */}
        {info && (
          context.signedInUser ? (info.is_admin === "0" ? (
            (info.players || info.player_data).filter((p: any) => p.username === context.signedInUser).length === 0 ? (
              // user is not participating
              info.tournament_state === "0" ?
                // tournament has not started
                <Button text="Join Tournament" onClick={async () => {
                  // Send post to /api/tournament/join/{tournamentId}
                  // if success, navigate to /tournament/{tournamentId}/participants
                  const response = await fetch(process.env.REACT_APP_BACKEND_URL + '/api/tournament/join/' + id, {
                    method: 'POST',
                    credentials: "include"
                  });

                  if (response.ok) {
                    navigate('/tournament/' + id + '/participants');
                  } else {
                    alert('Failed to join tournament: ' + await response.text());
                  }
                }} /> :
                (info.tournament_state === "1" ? "Tournament has started. You do not participate." : "Tournament has ended. You did not participate.")
            ) : (
              // user is participating
              info.tournament_state === "0" || info.tournament_state === "1" ?
                // tournament has not started or is in progress
                "You participate." :
                // tournament has ended
                "You participated."
            )
          ) : (info.tournament_state !== "0" ? ( // part for ADMIN != 0
            info.tournament_state !== "2" ? <>
              <Button text="End Tournament" onClick={async () => {
                // Send post to /api/tournament/start/{tournamentId}
                // on success reload tournament info
                const response = await fetch(process.env.REACT_APP_BACKEND_URL + '/api/tournament/end/' + id, {
                  method: 'PATCH',
                  credentials: "include"
                });

                if (response.ok) {
                  alert('Tournament ended.');
                  loadTournamentInfo();
                } else {
                  alert('Failed to end the tournament: ' + await response.text());
                }
              }} />
              {info.rounds_generated < info.rounds && <Button className={styles["begin-next-round"]} text="Begin Next Round" onClick={async () => {
                // Send post to /api/tournament/generateRoundPairings
                // on success reload tournament info
                const response = await fetch(process.env.REACT_APP_BACKEND_URL + '/api/tournament/generateRoundPairings?' + new URLSearchParams([
                  ["tournamentId", id!],
                  ["round", info.rounds_generated + 1],
                ]), {
                  method: 'PUT',
                  credentials: "include"
                });

                if (response.ok) {
                  alert('Next round added successfully.');
                  loadTournamentInfo();
                } else {
                  alert('Failed to add new round: ' + await response.text());
                }
              }} />}
            </> : "Tournament has ended."
          ) : (<>
            <div>
              <h4>Change Number of Rounds</h4>
              <div className={styles["change-rounds"]}>
                <Input type="number" defaultValue={info.rounds} onChange={async (e) => {
                  newNumberOfRounds.current = parseInt((e.target as HTMLInputElement).value);
                }} />
                <Button text="Change" onClick={async () => {
                  const response = await fetch(process.env.REACT_APP_BACKEND_URL + '/api/tournament/change-rounds-no?' + new URLSearchParams([
                    ["tournamentId", id!.toString()],
                    ["newRounds", newNumberOfRounds.current.toString()],
                  ]), {
                    method: 'PUT',
                    credentials: "include"
                  });

                  if (response.ok) {
                    alert('Number of rounds changed successfully.');
                    loadTournamentInfo();
                  } else {
                    alert('Failed to change number of rounds: ' + await response.text());
                  }
                }} />
              </div>
            </div>
            {
              info.players.length > 0 ? <Button text="Start Tournament" onClick={async () => {
                // Send post to /api/tournament/start/{tournamentId}
                // on success reload tournament info
                const response = await fetch(process.env.REACT_APP_BACKEND_URL + '/api/tournament/start/' + id, {
                  method: 'PATCH',
                  credentials: "include"
                });

                if (response.ok) {
                  alert('Tournament started.');
                  loadTournamentInfo();
                } else {
                  alert('Failed to start tournament: ' + await response.text());
                }
              }} /> : "Waiting for players to join before the tournament can be started."
            }</>
          ))) : (
            info.tournament_state === "0" ? "Tournament has not yet started. Sign in to join!" : (info.tournament_state === "1" ? "Tournament is in progress." : "Tournament has ended."))
        )}
      </div>
    </div>
  );
}
