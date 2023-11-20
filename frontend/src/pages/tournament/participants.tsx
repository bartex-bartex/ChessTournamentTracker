import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import styles from './participants.module.css';
import TournamentNavbar from '../../components/tournament-navbar';

interface TournamentInfo {
  end_date: string;
  organiser: string;
  time_control: string;
  tournament_state: string;
  player_data: PlayerInfo[];
  players: Players[];
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

// Player data but before the tournament has started
interface Players {
  user_id: number;
  first_name: string;
  last_name: string;
  fide: string;
}

export default function TournamentParticipants() {
  const { id } = useParams();
  const [tournamentInfo, setTournamentInfo] = useState<TournamentInfo | null>(null);

  useEffect(() => {
    const fetchTournamentData = async () => {
      try {
        const response = await fetch(`/api/tournament/${id}`);
        if (!response.ok) {
          alert('Failed to fetch tournament details: ' + await response.text());
          return;
        }
        const body: TournamentInfo = await response.json();
        // If there's "players" in body, convert it to "player_data" for consistency
        if (body.players) {
          body.player_data = body.players.map((player) => {
            return {
              player_id: player.user_id,
              first_name: player.first_name,
              last_name: player.last_name,
              start_fide: player.fide,
              change_in_fide: 'N/A',
              score: 'N/A',
            };
          });
        }
        setTournamentInfo(body);
      } catch (err) {
        console.error('Error fetching tournament data:', err);
        setTournamentInfo(null);
      }
    };

    fetchTournamentData();
  }, [id]);

  return (
    <div className={styles['participants']}>
      <TournamentNavbar tournamentInfo={tournamentInfo} />
      <div className={styles['participants-container']}>
        <h1>{tournamentInfo ? tournamentInfo.name : ''}</h1>
        <div className={styles['participants-list']}>
          {tournamentInfo ? <>
            <h4>{tournamentInfo.player_data.length > 0 ? "Participants" : "Ouch! There aren't any participants yet."}</h4>
            {tournamentInfo.player_data.length > 0 &&
              <table className={styles['participants-table']}>
                <thead>
                  <tr>
                    <th>Order</th>
                    <th>Name</th>
                    <th>FIDE Rating</th>
                    {/* <th>Date of Birth</th> */}
                  </tr>
                </thead>
                <tbody>
                  {/* user_id, first_name, last_name, username, fide */}
                  {tournamentInfo.player_data.map((player, i) => (
                    <tr key={i}>
                      <td>{i + 1}</td>
                      <td>{`${player.first_name} ${player.last_name}`}</td>
                      <td>{player.start_fide}</td>
                      {/* <td>{player.date_of_birth}</td> */}
                    </tr>
                  ))}
                </tbody>
              </table>
            }
          </> : (
            ''
          )}
        </div>
      </div>
    </div>
  );
}
