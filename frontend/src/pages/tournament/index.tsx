import styles from "./index.module.css";

import { ReactNode, useCallback, useContext, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Button from "../../components/button";
import TournamentNavbar from "../../components/tournament-navbar";
import Context from "../../context";

export default function Tournament() {
  const { id } = useParams();
  const [info, setInfo] = useState<any>(null);
  const navigate = useNavigate();
  const context = useContext(Context);

  const loadTournamentInfo = useCallback(async () => {
    try {
      const response = await fetch('/api/tournament/' + id);

      if (!response.ok) {
        alert("Failed to fetch tournament details: " + await response.text());
      }
      const body = await response.json();
      setInfo(body);
    } catch (err) {
      setInfo("");
    }
  }, [id]);

  useEffect(() => {
    loadTournamentInfo();
  }, [id]);


  return (
    <div className={styles["tournament"]}>
      <TournamentNavbar tournamentId={id} tournamentStarted={info && !info.players} />
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
        {context.signedInUser && (!info || info.is_admin === "0" ? (
          <Button text="Join Tournament" onClick={async () => {
            // Send post to /api/tournament/join/{tournamentId}
            // if success, navigate to /tournament/{tournamentId}/participants
            const response = await fetch('/api/tournament/join/' + id, {
              method: 'POST',
            });

            if (response.ok) {
              navigate('/tournament/' + id + '/participants');
            } else {
              alert('Failed to join tournament: ' + await response.text());
            }
          }} />
        ) : (
          <Button text="Start Tournament" onClick={async () => {
            // Send post to /api/tournament/start/{tournamentId}
            // on success reload tournament info
            const response = await fetch('/api/tournament/start/' + id, {
              method: 'PATCH',
            });

            if (response.ok) {
              alert('Tournament started!');
              loadTournamentInfo();
            } else {
              alert('Failed to start tournament: ' + await response.text());
            }
          }} />
        ))}
      </div>
    </div>
  );
}
