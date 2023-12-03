import { useParams } from "react-router";
import styles from "./game.module.css";
import { useCallback, useEffect, useRef, useState } from "react";
import Input from "../../components/input";
import Button from "../../components/button";
import { Chessboard } from "react-chessboard";
import { Chess } from "chess.js";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faLeftLong, faRightLong } from "@fortawesome/free-solid-svg-icons";

function getFenAtNthMove(pgn: string, n: number) {
  const chess = new Chess();
  const moves = pgn.split(' ');
  for (let i = 0; i < n; i++) {
    chess.move(moves[i]);
  }
  return chess.fen();
}

function getGameLength(pgn: string) {
  const chess = new Chess();
  chess.loadPgn(pgn);
  return chess.history().length;
}

export default function TournamentGame() {
  const { tournamentId, matchId } = useParams();
  const [tournamentInfo, setTournamentInfo] = useState<any>(null);
  const [matchInfo, setMatchInfo] = useState<any>(null);
  const newGameNotation = useRef('');
  const [currentPreviewMoveNumber, setCurrentPreviewMoveNumber] = useState(0);

  const fetchTournamentInfo = useCallback(async () => {
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

  const fetchMatchInfo = useCallback(async () => {
    try {
      const response = await fetch(`/api/tournament/match/${matchId}`);
      if (!response.ok) {
        alert('Failed to fetch match details: ' + await response.text());
        return;
      }
      const body = await response.json();
      setMatchInfo(body);
      newGameNotation.current = body.game_notation;
    } catch (err) {
      console.error('Error fetching match data:', err);
      setMatchInfo(null);
    }
  }, [matchId]);

  useEffect(() => {
    fetchTournamentInfo();
    fetchMatchInfo();
  }, [tournamentId, matchId]);

  const currentFen = matchInfo && matchInfo.game_notation ? getFenAtNthMove(matchInfo.game_notation, currentPreviewMoveNumber) : '';
  const numberOfMoves = matchInfo && matchInfo.game_notation ? getGameLength(matchInfo.game_notation) : 0;

  return (
    <div className={styles['game']}>
      <div className={styles['game-container']}>
        {tournamentInfo && matchInfo && <>
          <h1>{tournamentInfo.name}: Round {matchInfo.round} - Table {matchInfo.table}</h1>
          <h2>{matchInfo.white_first_name} {matchInfo.white_last_name} vs. {matchInfo.black_first_name} {matchInfo.black_last_name}</h2>
          <div className="game-display">
            {matchInfo.game_notation ? <>
              <Chessboard id="game-board" position={currentFen} />
              <div className={styles['game-slider']}>
                <input type="range" min="0" max={numberOfMoves - 1} value={currentPreviewMoveNumber} onChange={(e) => {
                  setCurrentPreviewMoveNumber(parseInt(e.target.value));
                }} />
              </div>
              <div className={styles['game-controls']}>
                <FontAwesomeIcon className={styles['game-controls-icon']} icon={faLeftLong} onClick={() => {
                  setCurrentPreviewMoveNumber(Math.max(currentPreviewMoveNumber - 1, 0));
                }} />
                <div className={styles['game-controls-text']}>
                  <p>Move {currentPreviewMoveNumber + 1} of {numberOfMoves}</p>
                  <p>{currentPreviewMoveNumber % 2 === 0 ? 'White' : 'Black'} to move</p>
                </div>
                <FontAwesomeIcon className={styles['game-controls-icon']} icon={faRightLong} onClick={() => {
                  setCurrentPreviewMoveNumber(Math.min(currentPreviewMoveNumber + 1, numberOfMoves - 1));
                }} />
              </div>
            </> : <>
              <h3 className={styles['no-game-error']}>The recording for this game has not yet been uploaded.</h3>
            </>}
          </div>
          {tournamentInfo.is_admin === '1' && <>
            <h4>Edit The Game</h4>
            <div className={styles['edit-fields']}>
              <Input placeholder="PGN Notation" defaultValue={matchInfo.game_notation} onChange={(e) => {
                newGameNotation.current = e.target.value;
              }} />
              <Button text="Save" onClick={async () => {
                //             /**
                //  * Updates score and game notation of provided match
                //  * @param auth authorisation cookie, to check if user is admin of tournament the match was played in
                //  * @param matchId unique match id
                //  * @param score score (1 - white player win, 0 - tie, -1 - black player win, 2 - deletes match score and associated fide changes, other values - no change, defaults to 2)
                //  * @param gameNotation game notation (if empty no change, defaults to epty)
                //  * @return 200 if match successfully updated
                //  */
                // @PatchMapping("/api/tournament/round/updatematch")
                // public ResponseEntity<String> updateMatch(@CookieValue(value = "auth", defaultValue = "") String auth,
                //                                           @RequestParam(value = "matchId") int matchId,
                //                                           @RequestParam(value = "score", defaultValue = "2") int score,
                //                                           @RequestParam(value = "gameNotation", defaultValue = "") String gameNotation
                // ){
                if (!newGameNotation.current) {
                  alert('Please enter a game notation.');
                  return;
                }
                if (newGameNotation.current === matchInfo.game_notation) {
                  alert('The provided game notation is the same as the current one.');
                  return;
                }

                const response = await fetch("/api/tournament/round/updatematch?" + new URLSearchParams([
                  ['matchId', matchId!],
                  ['score', '-1000'], // do not update score
                  ['gameNotation', newGameNotation.current]
                ]), {
                  method: 'PATCH',
                });

                if (!response.ok) {
                  alert('Failed to update match: ' + await response.text());
                  return;
                }

                alert('Match updated successfully.');
                setCurrentPreviewMoveNumber(0);
                fetchMatchInfo();
              }} />
            </div>
          </>}
        </>}
      </div>
    </div>
  );
}
