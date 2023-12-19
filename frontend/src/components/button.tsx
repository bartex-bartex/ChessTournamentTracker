import styles from "./button.module.css";

import React from "react";

type Props = {
  text: string;
  onClick?: () => void;
  className?: string;
};

export default function Button({ text, onClick, className }: Props) {
  return (
    <button className={`${styles["button"]} ${className}`} onClick={onClick}>
      {text}
    </button>
  );
}
