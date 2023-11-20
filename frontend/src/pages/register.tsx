import styles from "./register.module.css";

import Input from "../components/input";
import Button from "../components/button";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faLock, faUser } from "@fortawesome/free-solid-svg-icons";
import { useContext, useState } from "react";
import { useNavigate } from "react-router-dom";

import Context from "../context";

export default function Register() {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password1, setPassword1] = useState("");
  const [password2, setPassword2] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [sex, setSex] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [fideRating, setFideRating] = useState("");
  const navigate = useNavigate();
  const context = useContext(Context);

  return (
    <div className={styles["register"]}>
      <h1>
        <FontAwesomeIcon icon={faUser} />
        &nbsp; Create an account:
      </h1>
      <div className={styles["input-row"]}>
        <Input label="Username" onChange={e => {
          setUsername(e.target.value);
        }} />
        <Input label="Email" onChange={e => {
          setEmail(e.target.value);
        }} />
      </div>
      <div className={styles["input-row"]}>
        <Input label="Password" type="password" onChange={e => {
          setPassword1(e.target.value);
        }} />
        <Input label="Confirm Password" type="password" onChange={e => {
          setPassword2(e.target.value);
        }} />
      </div>
      <div className={styles["input-row"]}>
        <Input label="First Name" onChange={e => {
          setFirstName(e.target.value);
        }} />
        <Input label="Last Name" onChange={e => {
          setLastName(e.target.value);
        }} />
      </div>
      <div className={styles["input-row"]}>
        <Input label="Sex" onChange={e => {
          setSex(e.target.value);
        }} />
        <Input label="Date of Birth" onChange={e => {
          setDateOfBirth(e.target.value);
        }} />
      </div>
      <div className={styles["input-row"]}>
        <Input label="FIDE Rating" onChange={e => {
          setFideRating(e.target.value);
        }} />
      </div>
      <Button text="Register" onClick={async () => {
        // First check if any of the fields are empty
        if (!username || !email || !password1 || !password2 || !firstName || !lastName || !sex || !dateOfBirth || !fideRating) {
          alert("Please fill in all fields.");
          return;
        }

        // Check if passwords match
        if (password1 !== password2) {
          alert("Passwords do not match.");
          return;
        }

        // Send request to backend
        const response = await fetch('/api/user/register?' + new URLSearchParams([
          ['username', username],
          ['password', password1],
          ['passwordAgain', password2],
          ['mail', email],
          ['first_name', firstName],
          ['last_name', lastName],
          ['sex', sex],
          ['date_of_birth', dateOfBirth],
          ['fide', fideRating]
        ]).toString(), {
          method: 'POST'
        });

        // Check if request was successful
        if (!response.ok) {
          alert("Registration failed: " + await response.text());
          return;
        }

        // Registration successful
        // Set signed in user
        context.setSignedInUser!(username);
        // Redirect to home page
        navigate('/');
      }} />
    </div>
  );
}
