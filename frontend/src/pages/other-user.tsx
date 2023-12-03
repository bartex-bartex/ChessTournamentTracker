import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styles from './other-user.module.css'; // You will need to create this CSS module.

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
};

export default function OtherUser() {
  const { id } = useParams();
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const response = await fetch('/api/user/account/' + id);
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
      </div>
    </div>
  );
}
