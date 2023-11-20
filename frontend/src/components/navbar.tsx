import styles from "./navbar.module.css";

import { Link, useNavigate } from "react-router-dom";

import { useContext } from "react";
import Context from "../context";
import { faChessPawn } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";

import Button from "./button";

export default function Navbar() {
  const { signedInUser } = useContext(Context);
  const context = useContext(Context);
  const navigate = useNavigate();

  return (
    <nav className={styles["navbar"]}>
      <Link to="/">
        <FontAwesomeIcon icon={faChessPawn} className={styles["logo"]} />
      </Link>
      {signedInUser !== "" ? (
        <>
          <h1>Hello,&nbsp;
            <Link className={styles["user-link"]} to="/user">
              @{signedInUser}!</Link></h1>

          <div className={styles["session-buttons"]}>
            <Button
              className={styles["logout-button"]}
              text="Log Out"
              onClick={async () => {
                // Send request to backend
                const response = await fetch("/api/user/logout", {
                  method: "POST",
                });

                // Check if request was successful
                if (!response.ok) {
                  alert("Logout failed: " + await response.text());
                  return;
                }

                // Logout successful
                // Set signed in user
                context.setSignedInUser!("");
                // Redirect to home page
                navigate("/");
              }}
            />
          </div>
        </>
      ) : (
        <>
          <h1>Welcome to Chess Tournament Tracker</h1>

          <div className={styles["session-buttons"]}>
            <Link to="/login">
              <Button className={styles["login-button"]} text="Log In" />
            </Link>
            <Link to="/register">
              <Button className={styles["register-button"]} text="Register" />
            </Link>
          </div>
        </>
      )}
    </nav>
  );
}
