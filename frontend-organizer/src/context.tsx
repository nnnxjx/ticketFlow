import { createContext, useContext, type PropsWithChildren } from "react";
import { useOrganizerModel } from "./model";

type Model = ReturnType<typeof useOrganizerModel>;
const OrganizerContext = createContext<Model | null>(null);

export function OrganizerProvider({ children }: PropsWithChildren) {
  const model = useOrganizerModel();
  return (
    <OrganizerContext.Provider value={model}>
      {children}
    </OrganizerContext.Provider>
  );
}

export function useOrganizerContext() {
  const model = useContext(OrganizerContext);
  if (!model) throw new Error("OrganizerProvider is missing");
  return model;
}
