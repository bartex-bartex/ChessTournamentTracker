import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import styles from './results.module.css';
import TournamentNavbar from '../../components/tournament-navbar';

interface TournamentInfo {
  end_date: string;
  organiser: string;
  time_control: string;
  tournament_state: string;
  player_data: PlayerInfo[];
  players: any;
  name: string;
  location: string;
  rounds: string;
  tournament_id: string;
  start_date: string;
  info: string;
}

interface PlayerInfo {
  player_id: number;
  first_name: string;
  last_name: string;
  start_fide: string;
  change_in_fide: string;
  score: string;
}

export default function TournamentResults() {
  const { id } = useParams();
  const [tournamentInfo, setTournamentInfo] = useState<TournamentInfo | null>(null);

  useEffect(() => {
    const fetchTournamentData = async () => {
      try {
        const response = await fetch(process.env.REACT_APP_BACKEND_URL + `/api/tournament/${id}`);
        if (!response.ok) {
          alert('Failed to fetch tournament details: ' + await response.text());
          return;
        }
        const body: TournamentInfo = await response.json();
        setTournamentInfo(body);
      } catch (err) {
        console.error('Error fetching tournament data:', err);
        setTournamentInfo(null);
      }
    };

    fetchTournamentData();
  }, [id]);

  return (
    <div className={styles['results']}>
      <TournamentNavbar tournamentInfo={tournamentInfo} />
      <div className={styles['results-container']}>
        <h1>{tournamentInfo ? 'Tournament Results' : ''}</h1>
        <div className={styles['results-list']}>
          {tournamentInfo ? (
            <>
              <h4>{tournamentInfo.player_data.length > 0 ? "Results" : "No results available."}</h4>
              {tournamentInfo.player_data.length > 0 && (
                <table className={styles['results-table']}>
                  <thead>
                    <tr>
                      <th>Order</th>
                      <th>Name</th>
                      <th>FIDE Rating</th>
                      <th>Points (Pt)</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tournamentInfo.player_data.slice().sort((a, b) => parseFloat(b.score || "0") - parseFloat(a.score || "0")).map((data, i) => (
                      <tr key={data.player_id}>
                        <td>{i + 1}</td>
                        <td>{data.first_name} {data.last_name}</td>
                        <td>{data.start_fide}</td>
                        <td>{parseFloat(data.score || "0").toFixed(1)}</td>
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
