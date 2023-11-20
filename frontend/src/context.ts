import { createContext } from "react";

type ContextType = {
  signedInUser?: string;
  setSignedInUser?: (signedIn: string) => void;
};

const Context = createContext<ContextType>({});

export default Context;
