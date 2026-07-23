export const WORK_ORDER_TYPE_LABELS: Record<string, string> = {
  MONTAZA: "Montaža",
  DEMONTAZA: "Demontaža",
  INTERVENCIJA: "Intervencija",
  PREMONTAZA: "Premontaža",
  OSTALO: "Ostalo",
};

export const DIFFICULTY_LABELS: Record<string, string> = {
  OSNOVNA: "Osnovna",
  ZAHTEVNA: "Zahtevna",
};

export const INSTALLER_NAME_LABELS: Record<string, string> = {
  SIMON: "Simon",
  VITO: "Vito",
  SERGEJ: "Sergej",
  GREGOR: "Gregor",
  KLEMEN: "Klemen",
  OSTALO: "Ostalo",
};

export const OPTION_TYPE_LABELS: Record<string, string> = {
  DIN1: "DIN1",
  DIN2: "DIN2",
  DIN3: "DIN3",
  DIN4: "DIN4",
  DIN5: "DIN5",
  ANI1: "ANI1",
  ANI2: "ANI2",
  ANI3: "ANI3",
  ALL_CAN: "ALL CAN",
  FMSCAN: "FMSCAN",
  TACHO: "TACHO",
};

export const OPTION_TYPE_VALUES = Object.keys(OPTION_TYPE_LABELS);

export const WORK_ORDER_STATUS_LABELS: Record<string, string> = {
  COMPLETED: "Zaključen",
  SENT: "Poslan",
};

export const PERMISSION_DEFINITIONS: { key: PermissionKey; label: string }[] = [
  { key: "canManageUsers", label: "Upravljanje uporabnikov in pravic" },
  { key: "canManageClients", label: "Upravljanje strank" },
  { key: "canViewAllOrders", label: "Pregled vseh delovnih nalogov" },
  { key: "canExportData", label: "Izvoz podatkov (Excel/PDF)" },
  { key: "canSendEmail", label: "Pošiljanje nalogov po e-pošti" },
  { key: "canEditOrders", label: "Urejanje shranjenih nalogov" },
];

export type PermissionKey =
  | "canManageUsers"
  | "canManageClients"
  | "canViewAllOrders"
  | "canExportData"
  | "canSendEmail"
  | "canEditOrders";
