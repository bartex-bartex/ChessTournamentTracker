import { useParams } from "react-router";
import styles from "./round.module.css";
import { useCallback, useEffect, useState } from "react";
import TournamentNavbar from "../../components/tournament-navbar";
import Button from "../../components/button";
import { useNavigate } from "react-router-dom";

// [{"match_id":"1","black_last_name":"Żeliński","white_last_name":"ESf","score":"0","black_player_id":"7","round":"1","white_first_name":"ESf","game_notation":"","white_player_id":"9","black_first_name":"Tomasz","white_fide":"0","black_fide":"100","table":"1"}]
interface TournamentMatch {
  match_id: string;
  black_last_name: string;
  white_last_name: string;
  score: string;
  black_player_id: string;
  round: string;
  white_first_name: string;
  game_notation: string;
  white_player_id: string;
  black_first_name: string;
  white_fide: string;
  black_fide: string;
  table: string;
  white_score: string;
  black_score: string;
}

export default function TournamentRound() {
  const { tournamentId, round } = useParams();
  const [roundInfo, setRoundInfo] = useState<TournamentMatch[]>([]);
  const [tournamentInfo, setTournamentInfo] = useState<any>(null);
  const navigate = useNavigate();

  const fetchRoundData = useCallback(async (reload: boolean) => {
    try {
      const response = await fetch("/api/tournament/round?" + new URLSearchParams([
        ['tournamentId', tournamentId!],
        ['round', round!],
      ]).toString());
      if (!response.ok) {
        alert('Failed to fetch round details: ' + await response.text());
        return;
      }
      const body: TournamentMatch[] = await response.json();

      // Match body order to previous roundInfo order
      // We're guaranteed to have the same number of matches
      // We can use match_id as a key to match two arrays
      if (roundInfo.length !== 0 && !reload) {
        const bodySorted: TournamentMatch[] = [];
        for (const match of roundInfo) {
          for (const newMatch of body) {
            if (match.match_id === newMatch.match_id) {
              bodySorted.push(newMatch);
              break;
            }
          }
        }
        setRoundInfo(bodySorted);
      } else {
        setRoundInfo(body);
      }
    } catch (err) {
      console.error('Error fetching round data:', err);
      setRoundInfo([]);
    }
  }, [tournamentId, round]);

  const fetchTournamentData = useCallback(async () => {
    try {
      const response = await fetch(`/api/tournament/${tournamentId}`);
      if (!response.ok) {
        alert('Failed to fetch tournament details: ' + await response.text());
        return;
      }
      const body = await response.json();
      setTournamentInfo(body);
    } catch (err) {
      console.error('Error fetching tournament data:', err);
      setTournamentInfo(null);
    }
  }, [tournamentId]);

  useEffect(() => {
    fetchRoundData(true);
    fetchTournamentData();
  }, [tournamentId, round]);

  return (
    <div className={styles['round']}>
      <TournamentNavbar tournamentInfo={tournamentInfo} />
      <div className={styles['round-container']}>
        <h1>{roundInfo ? `Round ${round} Pairings` : ''}</h1>
        <div className={styles['round-list']}>
          {roundInfo ? (
            <>
              <h4>{roundInfo.length > 0 ? "Matches in This Round" : "No matches available."}</h4>
              {roundInfo.length > 0 && tournamentInfo && (
                <table className={styles['round-table']}>
                  <thead>
                    <tr>
                      <th>Order</th>
                      <th>White Score</th>
                      <th>White Player</th>
                      <th>Result</th>
                      <th>Black Player</th>
                      <th>Black Score</th>
                      <th>Game</th>
                    </tr>
                  </thead>
                  <tbody>
                    {roundInfo.map((data, i) => (
                      <tr key={data.match_id}>
                        <td>{i + 1}</td>
                        <td>{parseFloat(data.white_score || "0").toFixed(1)}</td>
                        <td>{data.white_first_name} {data.white_last_name}</td>
                        <td>
                          {tournamentInfo.is_admin == "1" && tournamentInfo.tournament_state == "1" ? (
                            <select name="result" id="result" defaultValue={data.score} onChange={async e => {
                              const selection = e.target.value;
                              const result = await fetch("/api/tournament/round/updatematch?" + new URLSearchParams([
                                ['matchId', data.match_id],
                                ['score', selection],
                                ['gameNotation', ''],
                              ]).toString(), {
                                method: "PATCH",
                              });

                              if (!result.ok) {
                                alert("Failed to update match: " + await result.text());
                              }

                              fetchRoundData(false);
                              fetchTournamentData();
                            }}>
                              <option value="1">1-0</option>
                              <option value="0">1/2-1/2</option>
                              <option value="-1">0-1</option>
                              <option value="2">-</option>
                            </select>
                          ) : (
                            data.score === "1" ? "1-0" : data.score === "0" ? "1/2-1/2" : data.score === "-1" ? "0-1" : "-"
                          )}
                        </td>
                        <td>{data.black_first_name} {data.black_last_name}</td>
                        <td>{parseFloat(data.black_score || "0").toFixed(1)}</td>
                        <td>{data.game_notation ? <Button className={styles['view-game-button']} text="View" onClick={() => navigate(`/tournament/${tournamentId}/game/${data.match_id}`)} /> : (
                          tournamentInfo.is_admin == "1" && tournamentInfo.tournament_state == "1" ? (
                            <Button className={styles['view-game-button']} text="Add" onClick={() => navigate(`/tournament/${tournamentId}/game/${data.match_id}`)} />
                          ) : (
                            "N/A"
                          )
                        )}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </>
          ) : (
            ''
          )}
        </div>
      </div>
    </div>
  );
}
