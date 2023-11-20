import styles from "./input.module.css";

type Props = {
  label?: string;
  type?: string;
  placeholder?: any;
  defaultValue?: any;
  onChange?: (event: React.ChangeEvent<HTMLInputElement>) => void;
  className?: string;
};

export default function Input({
  label,
  type,
  placeholder,
  defaultValue,
  onChange,
  className,
}: Props) {
  return (
    <div className={styles["input-container"]}>
      {label && <label className={styles["label"]}>{label}</label>}
      <input
        className={`${styles["input"]} ${className}`}
        placeholder={placeholder}
        type={type}
        defaultValue={defaultValue}
        onChange={onChange}
      />
    </div>
  );
}
