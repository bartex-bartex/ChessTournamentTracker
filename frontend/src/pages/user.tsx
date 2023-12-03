import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './user.module.css'; // You will need to create this CSS module.

interface UserInfo {
  mail: string;
  date_of_birth: string;
  sex: string;
  last_name: string;
  fide: string;
  first_name: string;
  username: string;
}

type Tournament = {
  id: number;
  date: string;
  name: string;
  timeControl: string;
  place: string;
  role: string;
  state: string;
};

export default function User() {
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
  const navigate = useNavigate();
  const [tournaments, setTournaments] = useState<Tournament[]>([]);

  const fetchMyTournaments = useCallback(async () => {
    try {
      const response = await fetch('/api/user/my-tournaments');

      if (!response.ok) {
        alert("Failed to fetch tournaments: " + await response.text());
      }

      const body = await response.json();
      setTournaments(body.map((tournament: any) => ({
        id: tournament.tournament_id,
        date: tournament.start_date,
        name: tournament.name,
        timeControl: tournament.time_control,
        place: tournament.location,
        role: tournament.role[0].toUpperCase() + tournament.role.slice(1),
        state: tournament.tournament_state === '0' ? 'Pending' : tournament.tournament_state === '1' ? 'In Progress' : 'Finished'
      })));
    } catch (err) {
      alert("Failed to fetch tournaments: " + err);
    }
  }, []);

  useEffect(() => {
    fetchMyTournaments();
    (async () => {
      try {
        const response = await fetch('/api/user');
        if (!response.ok) {
          alert('Failed to fetch user details: ' + await response.text());
          return;
        }
        const body: UserInfo = await response.json();
        setUserInfo(body);
      } catch (err) {
        console.error('Error fetching user data:', err);
        setUserInfo(null);
      }
    })();
  }, []);

  return (
    <div className={styles['user']}>
      <div className={styles['user-container']}>
        <h1>{userInfo ? `${userInfo.first_name} ${userInfo.last_name}` : 'Loading...'}</h1>
        <div className={styles['user-details']}>
          <h4>User Details</h4>
          <table className={styles['user-details-table']}>
            <tbody>
              <tr>
                <td>Username</td>
                <td>{userInfo ? userInfo.username : 'Loading...'}</td>
              </tr>
              <tr>
                <td>Email</td>
                <td>{userInfo ? userInfo.mail : 'Loading...'}</td>
              </tr>
              <tr>
                <td>Date of Birth</td>
                <td>{userInfo ? userInfo.date_of_birth : 'Loading...'}</td>
              </tr>
              <tr>
                <td>Sex</td>
                <td>{userInfo ? userInfo.sex : 'Loading...'}</td>
              </tr>
              <tr>
                <td>FIDE Rating</td>
                <td>{userInfo ? userInfo.fide : 'Loading...'}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div className={styles['user-details']}>
          <h4>Your Tournaments</h4>
          <table className={styles["tournaments-table"]}>
            <thead>
              <tr>
                <th>Date</th>
                <th>Name</th>
                <th>Time Control</th>
                <th>Place</th>
                <th>Role</th>
                <th>State</th>
              </tr>
            </thead>
            <tbody>
              {tournaments.map((tournament) => (
                <tr key={tournament.name} onClick={() => navigate(`/tournament/${tournament.id}`)}>
                  <td>{tournament.date}</td>
                  <td>{tournament.name}</td>
                  <td>{tournament.timeControl}</td>
                  <td>{tournament.place}</td>
                  <td>{tournament.role}</td>
                  <td>{tournament.state}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}