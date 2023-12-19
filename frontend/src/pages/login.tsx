import styles from "./login.module.css";

import Input from "../components/input";
import Button from "../components/button";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faLock } from "@fortawesome/free-solid-svg-icons";
import { useContext, useState } from "react";
import { useNavigate } from "react-router-dom";
import React from "react";

import Context from "../context";

export default function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const navigate = useNavigate();
  const context = useContext(Context);

  return (
    <div className={styles["login"]}>
      <h1>
        <FontAwesomeIcon icon={faLock} />
        &nbsp; Log into your account:
      </h1>
      <Input className={styles["field"]} label="Username" onChange={e => {
        setUsername(e.target.value);
      }} />
      <Input className={styles["field"]} label="Password" type="password" onChange={e => {
        setPassword(e.target.value);
      }} />
      <Button text="Log In" onClick={async () => {
        // First check if any of the fields are empty
        if (!username || !password) {
          alert("Please fill in all fields.");
          return;
        }

        // Send request to backend
        const response = await fetch('/api/user/login?' + new URLSearchParams([
          ['username', username],
          ['password', password],
        ]).toString(), {
          method: 'POST'
        });

        // Check if request was successful
        if (!response.ok) {
          alert("Login failed: " + await response.text());
          return;
        }

        // Login successful
        // Set signed in user
        context.setSignedInUser!(username);
        // Redirect to home page
        navigate('/');
      }} />
    </div>
  );
}
