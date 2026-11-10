import { createContext, useContext, type PropsWithChildren } from "react";
import { useUserModel } from "./model";

type Model = ReturnType<typeof useUserModel>;
const UserContext = createContext<Model | null>(null);

export function UserProvider({ children }: PropsWithChildren) {
  const model = useUserModel();
  return <UserContext.Provider value={model}>{children}</UserContext.Provider>;
}

export function useUserContext() {
  const model = useContext(UserContext);
  if (!model) throw new Error("UserProvider is missing");
  return model;
}
