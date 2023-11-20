import styles from "./index.module.css";

import { ReactNode } from "react";
import { useParams } from "react-router-dom";

export default function Matches() {
  const { i } = useParams();

  return <div className={styles["matches"]}>matches/i</div>;
}
