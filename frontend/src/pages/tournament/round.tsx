import { useParams } from "react-router";
import styles from "./round.module.css";
import { useCallback, useEffect, useState } from "react";
import TournamentNavbar from "../../components/tournament-navbar";

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
}

export default function TournamentRound() {
  const { tournamentId, round } = useParams();
  const [roundInfo, setRoundInfo] = useState<TournamentMatch[]>([]);
  const [tournamentInfo, setTournamentInfo] = useState<any>(null);

  const fetchRoundData = useCallback(async () => {
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
      setRoundInfo(body);
    } catch (err) {
      console.error('Error fetching round data:', err);
      setRoundInfo([]);
    }
  }, [tournamentId, round]);

  useEffect(() => {
    fetchRoundData();
  }, [tournamentId, round]);

  useEffect(() => {
    const fetchTournamentData = async () => {
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
    };

    fetchTournamentData();
  }, [tournamentId]);

  return (
    <div className={styles['round']}>
      <TournamentNavbar tournamentInfo={tournamentInfo} />
      <div className={styles['round-container']}>
        <h1>{roundInfo ? `Round ${round} Pairings` : ''}</h1>
        <div className={styles['round-list']}>
          {roundInfo ? (
            <>
              <h4>{roundInfo.length > 0 ? "Matches in This Round" : "No matches available."}</h4>
              {roundInfo.length > 0 && (
                <table className={styles['round-table']}>
                  <thead>
                    <tr>
                      <th>Order</th>
                      <th>White Score</th>
                      <th>White Player</th>
                      <th>Result</th>
                      <th>Black Player</th>
                      <th>Black Score</th>
                    </tr>
                  </thead>
                  <tbody>
                    {roundInfo.map((data, i) => (
                      <tr key={data.match_id}>
                        <td>{i + 1}</td>
                        <td>TODO Backend</td>
                        <td>{data.white_first_name} {data.white_last_name}</td>
                        <td>
                          <select name="result" id="result" defaultValue={data.score} onChange={async e => {
                            // PatchMapping("/api/tournament/round/updatematch")
                            // public ResponseEntity<String> updateMatch(@CookieValue(value = "auth", defaultValue = "") String auth,
                            //                                           @RequestParam(value = "matchId") int matchId,
                            //                                           @RequestParam(value = "score", defaultValue = "2") int score,
                            //                                           @RequestParam(value = "gameNotation", defaultValue = "") String gameNotation
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

                            fetchRoundData();
                          }}>
                            <option value="1">1-0</option>
                            <option value="0">1/2-1/2</option>
                            <option value="-1">0-1</option>
                            <option value="2">-</option>
                          </select>
                        </td>
                        <td>{data.black_first_name} {data.black_last_name}</td>
                        <td>TODO Backend</td>
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
