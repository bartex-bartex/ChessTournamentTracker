import styles from "./tournament-navbar.module.css";

import { Link } from "react-router-dom";

type Props = {
    tournamentInfo?: any;
};

const TournamentNavbar = ({ tournamentInfo }: Props) => {
    return (
        <div className={styles["sub-navbar"]}>
            {tournamentInfo && <>
                <Link to={`/tournament/${tournamentInfo.tournament_id}`}><h2>General Information</h2></Link>
                <Link className={styles["sub-navbar-link"]} to={`/tournament/${tournamentInfo.tournament_id}/participants`}>Participants</Link>
                {tournamentInfo && !tournamentInfo.players && <><Link className={styles["sub-navbar-link"]} to={`/tournament/${tournamentInfo.tournament_id}/results`}>Results</Link>
                    { // Create links for each round (there are tournamentInfo.rounds rounds)
                        [...Array(parseInt(tournamentInfo.rounds_generated))].map((_, i) => (
                            <Link key={i} className={styles["sub-navbar-link"]} to={`/tournament/${tournamentInfo.tournament_id}/round/${i + 1}`}>&nbsp;&nbsp;Round {i + 1}</Link>
                        ))
                    }
                    <Link className={styles["sub-navbar-link"]} to={`/tournament/${tournamentInfo.tournament_id}/ranking`}>Ranking Changes</Link></>}
            </>}
        </div>
    );
};

export default TournamentNavbar;
