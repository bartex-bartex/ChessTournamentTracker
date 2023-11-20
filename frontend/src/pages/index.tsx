import styles from "./index.module.css";

import Button from "../components/button";
import { useContext, useEffect, useState } from "react";

import Context from "../context";
import { useNavigate } from 'react-router-dom';

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
  const navigate = useNavigate();

  useEffect(() => {
    (async () => {
      try {
        const response = await fetch('/api/homepage');

        if (!response.ok) {
          alert("Failed to fetch tournaments: " + await response.text());
        }
        // setTournaments(await response.json());
        const body = await response.json();
        // setTournaments([
        //   {
        //     id: 1,
        //     date: "2021-01-01",
        //     name: "Tournament 1",
        //     timeControl: "90' + 30''",
        //     place: "Warsaw",
        //   },
        //   {
        //     id: 2,
        //     date: "2021-01-02",
        //     name: "Tournament 2",
        //     timeControl: "60' + 30''",
        //     place: "Cracow",
        //   },
        //   {
        //     id: 3,
        //     date: "2021-01-03",
        //     name: "Tournament 3",
        //     timeControl: "90' + 30''",
        //     place: "Wroclaw",
        //   },
        // ]);
        setTournaments(body.tournaments.map((tournament: any) => ({
          id: tournament.tournament_id,
          date: tournament.start_date,
          name: tournament.name,
          timeControl: tournament.time_control,
          place: tournament.location,
        })));
      } catch (err) {
        setTournaments([]);
      }
    })();
  }, []);

  return (
    <div className={styles["home"]}>
      <div className={styles["tournaments-table-container"]}>
        <div className={styles["tournaments-above-table"]}>
          <h2>Recent Tournaments</h2>
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
