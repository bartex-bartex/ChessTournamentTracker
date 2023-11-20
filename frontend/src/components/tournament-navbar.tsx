import styles from "./tournament-navbar.module.css";

import { Link } from "react-router-dom";

type Props = {
    tournamentId?: string;
    tournamentStarted?: any;
};

const TournamentNavbar = ({ tournamentId, tournamentStarted }: Props) => {
    return (
        <div className={styles["sub-navbar"]}>
            <Link to={`/tournament/${tournamentId}`}><h2>General Information</h2></Link>
            {tournamentId && <>
                <Link className={styles["sub-navbar-link"]} to={`/tournament/${tournamentId}/participants`}>Participants</Link>
                {tournamentStarted && <><Link className={styles["sub-navbar-link"]} to={`/tournament/${tournamentId}/results`}>Results</Link>
                    <Link className={styles["sub-navbar-link"]} to={`/tournament/${tournamentId}/round/1`}>&nbsp;&nbsp;Round 1</Link>
                    <Link className={styles["sub-navbar-link"]} to={`/tournament/${tournamentId}/round/2`}>&nbsp;&nbsp;Round 2</Link>
                    <Link className={styles["sub-navbar-link"]} to={`/tournament/${tournamentId}/ranking`}>Ranking Changes</Link></>}
            </>}
        </div>
    );
};

export default TournamentNavbar;
