import styles from "./not-found.module.css";

import React from "react";

export default function NotFound() {
    return (
        <div className={styles["not-found"]}>
            <h1>404</h1>
            <h2>Not Found</h2>
        </div>
    );
}
