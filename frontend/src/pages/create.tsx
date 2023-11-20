import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import styles from "./create.module.css";
import { faPlus } from "@fortawesome/free-solid-svg-icons";
import Input from "../components/input";
import { useState } from "react";
import Button from "../components/button";
import { useNavigate } from "react-router-dom";

// @PostMapping("/api/tournament/create")
// public ResponseEntity<String> create(@CookieValue(value = "auth", defaultValue = "") String auth,
//                                      @RequestParam(value = "tournamentName") String name,
//                                      @RequestParam(value = "location") String location,
//                                      @RequestParam(value = "organizer") String organizer,
//                                      @RequestParam(value = "timeControl") String timeControl,
//                                      @RequestParam(value = "startDate") String startDate,
//                                      @RequestParam(value = "endDate") String endDate,
//                                      @RequestParam(value = "rounds") int rounds,
//                                      @RequestParam(value = "info") String info) {

export default function Create() {
  const [name, setName] = useState<string>("");
  const [location, setLocation] = useState<string>("");
  const [organizer, setOrganizer] = useState<string>("");
  const [timeControl, setTimeControl] = useState<string>("");
  const [startDate, setStartDate] = useState<string>("");
  const [endDate, setEndDate] = useState<string>("");
  const [rounds, setRounds] = useState<string>("1");
  const [info, setInfo] = useState<string>("");
  const navigate = useNavigate();

  return (
    <div className={styles["create"]}>
      <h1>
        <FontAwesomeIcon icon={faPlus} />
        &nbsp; Create a tournament:
      </h1>
      <div className={styles["input-row"]}>
        <Input label="Tournament Name" onChange={e => {
          setName(e.target.value);
        }} />
        <Input label="Location" placeholder="e.g. Warsaw" onChange={e => {
          setLocation(e.target.value);
        }} />
      </div>
      <div className={styles["input-row"]}>
        <Input label="Organizer" onChange={e => {
          setOrganizer(e.target.value);
        }} />
        <Input label="Time Control" placeholder="e.g. 90' + 30''" onChange={e => {
          setTimeControl(e.target.value);
        }} />
      </div>
      <div className={styles["input-row"]}>
        <Input label="Start Date" type="date" onChange={e => {
          setStartDate(e.target.value);
        }} />
        <Input label="End Date" type="date" onChange={e => {
          setEndDate(e.target.value);
        }} />
      </div>
      <div className={styles["input-row"]}>
        <Input label="Rounds" defaultValue="1" type="number" onChange={e => {
          setRounds(e.target.value);
        }} />
        <Input label="Description" onChange={e => {
          setInfo(e.target.value);
        }} />
      </div>
      <Button text="Create" onClick={async () => {
        // First check if any of the fields are empty
        if (!name || !location || !organizer || !timeControl || !startDate || !endDate || !rounds || !info) {
          alert("Please fill in all fields.");
          return;
        }

        // Check if the start date is before the end date
        if (startDate > endDate) {
          alert("Start date must be before the end date.");
          return;
        }

        // Send request to backend
        const response = await fetch('/api/tournament/create?' + new URLSearchParams([
          ['tournamentName', name],
          ['location', location],
          ['organizer', organizer],
          ['timeControl', timeControl],
          ['startDate', startDate],
          ['endDate', endDate],
          ['rounds', rounds],
          ['info', info]
        ]).toString(), {
          method: 'POST'
        });

        // Check if request was successful
        if (!response.ok) {
          alert("Failed to create tournament:\n" + await response.text());
          return;
        }

        const id = (await response.json()).tournamentId;

        // Redirect to the tournament page
        navigate("/tournament/" + id);
      }} />
    </div>
  );
}
