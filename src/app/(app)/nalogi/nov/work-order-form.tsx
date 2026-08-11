"use client";

import { useCallback, useEffect, useState, useTransition } from "react";
import { createWorkOrder } from "./actions";
import { ImeiScanner } from "./imei-scanner";
import { SignatureModal } from "./signature-modal";
import { ClientCombobox } from "@/components/client-combobox";
import { PlateCombobox } from "@/components/plate-combobox";

type Client = { id: string; name: string };

const TYPES = [
  { value: "MONTAZA", label: "Montaža" },
  { value: "DEMONTAZA", label: "Demontaža" },
  { value: "INTERVENCIJA", label: "Intervencija" },
  { value: "PREMONTAZA", label: "Premontaža" },
  { value: "OSTALO", label: "Ostalo" },
];

const DIFFICULTIES = [
  { value: "OSNOVNA", label: "Osnovna" },
  { value: "ZAHTEVNA", label: "Zahtevna" },
];

const INSTALLERS = ["SIMON", "VITO", "SERGEJ", "GREGOR", "KLEMEN", "OSTALO"];

const OPTION_TYPES = [
  "DIN1",
  "DIN2",
  "DIN3",
  "DIN4",
  "DIN5",
  "ANI1",
  "ANI2",
  "ANI3",
  "ALL_CAN",
  "FMSCAN",
  "TACHO",
  "WIRE_TEMP1",
  "WIRE_TEMP2",
  "WIRE_TEMP3",
  "ID_KEY",
  "RFID_125",
  "RFID_1356",
  "BUZZER",
];

const DEVICE_MODELS = ["FMC130", "FMC150", "FMC650", "FMC880", "TFT100", "OSTALO"];

const OPTION_LABELS: Record<string, string> = {
  DIN1: "DIN1 (IGN)",
  ALL_CAN: "ALL CAN",
  WIRE_TEMP1: "1 Wire Temp (1)",
  WIRE_TEMP2: "1 Wire Temp (2)",
  WIRE_TEMP3: "1 Wire Temp (3)",
  ID_KEY: "ID",
  RFID_125: "RFID 125 kHz",
  RFID_1356: "RFID 13,56 MHz",
  BUZZER: "Brenčač",
};

function fieldClass() {
  return "mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2.5 text-base text-gray-900 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100 sm:py-2";
}

function checkboxClass() {
  return "h-5 w-5 shrink-0 sm:h-4 sm:w-4";
}

export function WorkOrderForm({ clients, vehiclePlates }: { clients: Client[]; vehiclePlates: string[] }) {
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  const [type, setType] = useState("MONTAZA");
  const [difficulty, setDifficulty] = useState("OSNOVNA");
  const [clientId, setClientId] = useState("");

  const [installers, setInstallers] = useState<Record<string, boolean>>({});
  const [installerOtherText, setInstallerOtherText] = useState("");

  const [vehiclePlate, setVehiclePlate] = useState("");
  const [vehicleBrand, setVehicleBrand] = useState("");
  const [vehicleModel, setVehicleModel] = useState("");
  const [vehicleYear, setVehicleYear] = useState("");

  const [imei, setImei] = useState("");
  const [imeiPrev, setImeiPrev] = useState("");
  const [culprit, setCulprit] = useState("");
  const [scanningField, setScanningField] = useState<"imei" | "imeiPrev" | null>(null);

  const handleScan = useCallback(
    (digits: string) => {
      if (scanningField === "imei") setImei(digits);
      if (scanningField === "imeiPrev") setImeiPrev(digits);
      setScanningField(null);
    },
    [scanningField]
  );

  const [optionState, setOptionState] = useState<Record<string, { checked: boolean; comment: string }>>({});

  const [deviceModel, setDeviceModel] = useState("");
  const [deviceModelOtherText, setDeviceModelOtherText] = useState("");

  const [photos, setPhotos] = useState<File[]>([]);
  const [photoUrls, setPhotoUrls] = useState<string[]>([]);
  const [comment, setComment] = useState("");

  const [signatureDataUrl, setSignatureDataUrl] = useState<string | null>(null);
  const [showSignatureModal, setShowSignatureModal] = useState(false);

  useEffect(() => {
    const urls = photos.map((f) => URL.createObjectURL(f));
    setPhotoUrls(urls);
    return () => urls.forEach((u) => URL.revokeObjectURL(u));
  }, [photos]);

  function toggleInstaller(name: string) {
    setInstallers((prev) => ({ ...prev, [name]: !prev[name] }));
  }

  function toggleOption(opt: string) {
    setOptionState((prev) => ({
      ...prev,
      [opt]: { checked: !prev[opt]?.checked, comment: prev[opt]?.comment ?? "" },
    }));
  }

  function setOptionComment(opt: string, value: string) {
    setOptionState((prev) => ({ ...prev, [opt]: { checked: prev[opt]?.checked ?? true, comment: value } }));
  }

  function handlePhotoChange(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    setPhotos((prev) => [...prev, ...files]);
    e.target.value = "";
  }

  function removePhoto(index: number) {
    setPhotos((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!clientId) return setError("Izberi stranko.");
    if (!vehiclePlate || !vehicleBrand || !vehicleModel || !vehicleYear) {
      return setError("Izpolni vse podatke o vozilu.");
    }
    if (!/^\d{10}$/.test(imei)) return setError("IMEI mora vsebovati natanko zadnjih 10 številk.");
    if (type === "INTERVENCIJA" && !/^\d{10}$/.test(imeiPrev)) {
      return setError("Pri intervenciji vnesi tudi veljaven IMEI prej (10 številk).");
    }
    if (type === "INTERVENCIJA" && !culprit) {
      return setError("Pri intervenciji izberi krivca.");
    }

    const selectedInstallers = Object.entries(installers)
      .filter(([, checked]) => checked)
      .map(([name]) => ({ name, otherText: name === "OSTALO" ? installerOtherText : undefined }));

    if (selectedInstallers.length === 0) return setError("Izberi vsaj enega monterja.");
    if (installers.OSTALO && !installerOtherText.trim()) {
      return setError("Vnesi ime monterja pri izbiri 'Ostalo'.");
    }
    if (deviceModel === "OSTALO" && !deviceModelOtherText.trim()) {
      return setError("Vnesi napravo pri izbiri 'Drugo'.");
    }

    const signatureBlob = signatureDataUrl ? await (await fetch(signatureDataUrl)).blob() : null;

    const selectedOptions = Object.entries(optionState)
      .filter(([, v]) => v.checked)
      .map(([optionType, v]) => ({ optionType, comment: v.comment || undefined }));

    const selectedDeviceModels = deviceModel
      ? [{ deviceModel, comment: deviceModel === "OSTALO" ? deviceModelOtherText : undefined }]
      : [];

    const fd = new FormData();
    fd.set("type", type);
    fd.set("difficulty", difficulty);
    fd.set("clientId", clientId);
    fd.set("vehiclePlate", vehiclePlate);
    fd.set("vehicleBrand", vehicleBrand);
    fd.set("vehicleModel", vehicleModel);
    fd.set("vehicleYear", vehicleYear);
    fd.set("imei", imei);
    fd.set("imeiPrev", type === "INTERVENCIJA" ? imeiPrev : "");
    fd.set("culprit", type === "INTERVENCIJA" ? culprit : "");
    fd.set("comment", comment);
    fd.set("installers", JSON.stringify(selectedInstallers));
    fd.set("options", JSON.stringify(selectedOptions));
    fd.set("deviceModels", JSON.stringify(selectedDeviceModels));
    if (signatureBlob) fd.set("signature", signatureBlob, "signature.png");
    photos.forEach((file) => fd.append("photos", file));

    startTransition(async () => {
      try {
        const result = await createWorkOrder(undefined, fd);
        if (result?.error) setError(result.error);
      } catch {
        setError("Shranjevanje ni uspelo (morda prešibka povezava ali prevelike slike). Poskusi znova.");
      }
    });
  }

  return (
    <form onSubmit={handleSubmit} className="mx-auto max-w-xl space-y-6 pb-24">
      <section className="grid grid-cols-2 gap-3">
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          Tip naloga
          <select value={type} onChange={(e) => setType(e.target.value)} className={fieldClass()}>
            {TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
        </label>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          Zahtevnost
          <select value={difficulty} onChange={(e) => setDifficulty(e.target.value)} className={fieldClass()}>
            {DIFFICULTIES.map((d) => (
              <option key={d.value} value={d.value}>
                {d.label}
              </option>
            ))}
          </select>
        </label>
      </section>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Stranka</label>
        <ClientCombobox clients={clients} value={clientId} onChange={setClientId} required />
      </div>

      <fieldset>
        <legend className="text-sm font-medium text-gray-700 dark:text-gray-300">Kdo je montiral</legend>
        <div className="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-3">
          {INSTALLERS.map((name) => (
            <label
              key={name}
              className="flex items-center gap-2.5 rounded-md border border-gray-200 px-3.5 py-3 text-base text-gray-900 dark:border-gray-700 dark:text-gray-100 sm:px-3 sm:py-2 sm:text-sm"
            >
              <input
                type="checkbox"
                checked={!!installers[name]}
                onChange={() => toggleInstaller(name)}
                className={checkboxClass()}
              />
              {name === "OSTALO" ? "Ostalo" : name.charAt(0) + name.slice(1).toLowerCase()}
            </label>
          ))}
        </div>
        {installers.OSTALO && (
          <input
            value={installerOtherText}
            onChange={(e) => setInstallerOtherText(e.target.value)}
            placeholder="Ime monterja"
            className={fieldClass()}
          />
        )}
      </fieldset>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Registrska št. vozila</label>
        <PlateCombobox
          plates={vehiclePlates}
          value={vehiclePlate}
          onChange={setVehiclePlate}
          className={`${fieldClass()} mt-0`}
        />
      </div>

      <section className="grid grid-cols-3 gap-3">
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          Znamka
          <input value={vehicleBrand} onChange={(e) => setVehicleBrand(e.target.value)} className={fieldClass()} />
        </label>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          Model
          <input value={vehicleModel} onChange={(e) => setVehicleModel(e.target.value)} className={fieldClass()} />
        </label>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          Letnik
          <input
            value={vehicleYear}
            onChange={(e) => setVehicleYear(e.target.value)}
            inputMode="numeric"
            className={fieldClass()}
          />
        </label>
      </section>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          Št. IMEI (zadnjih 10 mest)
          <div className="mt-1 flex gap-2">
            <input
              value={imei}
              onChange={(e) => setImei(e.target.value.replace(/\D/g, "").slice(0, 10))}
              inputMode="numeric"
              className={`${fieldClass()} mt-0`}
            />
            <button
              type="button"
              onClick={() => setScanningField("imei")}
              className="mt-0 shrink-0 rounded-md border border-gray-300 bg-white px-3.5 py-2.5 text-base text-gray-700 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-200 sm:px-3 sm:py-2 sm:text-sm"
            >
              📷 Skeniraj
            </button>
          </div>
        </label>
        <a
          href="https://monter.sledenje.com/home"
          target="_blank"
          rel="noopener noreferrer"
          className="mt-2 block w-full rounded-md border border-gray-300 bg-white px-3.5 py-2.5 text-center text-base text-gray-700 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-200 sm:px-3 sm:py-2 sm:text-sm"
        >
          📍 Pozicija
        </a>
      </div>

      {type === "INTERVENCIJA" && (
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
            Št. IMEI prej (zadnjih 10 mest)
            <div className="mt-1 flex gap-2">
              <input
                value={imeiPrev}
                onChange={(e) => setImeiPrev(e.target.value.replace(/\D/g, "").slice(0, 10))}
                inputMode="numeric"
                className={`${fieldClass()} mt-0`}
              />
              <button
                type="button"
                onClick={() => setScanningField("imeiPrev")}
                className="mt-0 shrink-0 rounded-md border border-gray-300 bg-white px-3.5 py-2.5 text-base text-gray-700 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-200 sm:px-3 sm:py-2 sm:text-sm"
              >
                📷 Skeniraj
              </button>
            </div>
          </label>
        </div>
      )}

      {scanningField && <ImeiScanner onScan={handleScan} onClose={() => setScanningField(null)} />}
      {showSignatureModal && (
        <SignatureModal
          onSave={(dataUrl) => setSignatureDataUrl(dataUrl)}
          onClose={() => setShowSignatureModal(false)}
        />
      )}

      <fieldset>
        <legend className="text-sm font-medium text-gray-700 dark:text-gray-300">Model naprave</legend>
        <div className="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-3">
          {DEVICE_MODELS.map((name) => (
            <label
              key={name}
              className="flex items-center gap-2.5 rounded-md border border-gray-200 px-3.5 py-3 text-base text-gray-900 dark:border-gray-700 dark:text-gray-100 sm:px-3 sm:py-2 sm:text-sm"
            >
              <input
                type="radio"
                name="deviceModel"
                checked={deviceModel === name}
                onChange={() => setDeviceModel(name)}
                className={checkboxClass()}
              />
              {name === "OSTALO" ? "Drugo" : name}
            </label>
          ))}
        </div>
        {deviceModel === "OSTALO" && (
          <input
            value={deviceModelOtherText}
            onChange={(e) => setDeviceModelOtherText(e.target.value)}
            placeholder="Naprava"
            className={fieldClass()}
          />
        )}
      </fieldset>

      <fieldset>
        <legend className="text-sm font-medium text-gray-700 dark:text-gray-300">DIN / ANI / CAN</legend>
        <div className="mt-2 space-y-2">
          {OPTION_TYPES.map((opt) => (
            <div key={opt} className="rounded-md border border-gray-200 p-2.5 dark:border-gray-700 sm:p-2">
              <label className="flex items-center gap-2.5 text-base text-gray-900 dark:text-gray-100 sm:text-sm">
                <input
                  type="checkbox"
                  checked={!!optionState[opt]?.checked}
                  onChange={() => toggleOption(opt)}
                  className={checkboxClass()}
                />
                {OPTION_LABELS[opt] ?? opt}
              </label>
              {optionState[opt]?.checked && (
                <input
                  value={optionState[opt]?.comment ?? ""}
                  onChange={(e) => setOptionComment(opt, e.target.value)}
                  placeholder="Komentar (neobvezno)"
                  className="mt-2 w-full rounded-md border border-gray-300 bg-white px-2 py-1 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100"
                />
              )}
            </div>
          ))}
        </div>
      </fieldset>

      <fieldset>
        <legend className="text-sm font-medium text-gray-700 dark:text-gray-300">Slike</legend>
        <div className="mt-2 flex gap-2">
          <label className="flex-1 cursor-pointer rounded-md bg-gray-100 px-4 py-2.5 text-center text-base text-gray-700 dark:bg-gray-700 dark:text-gray-100 sm:py-2 sm:text-sm">
            📷 Fotografiraj
            <input type="file" accept="image/*" capture="environment" multiple onChange={handlePhotoChange} className="hidden" />
          </label>
          <label className="flex-1 cursor-pointer rounded-md bg-gray-100 px-4 py-2.5 text-center text-base text-gray-700 dark:bg-gray-700 dark:text-gray-100 sm:py-2 sm:text-sm">
            🖼️ Iz galerije
            <input type="file" accept="image/*" multiple onChange={handlePhotoChange} className="hidden" />
          </label>
        </div>
        {photoUrls.length > 0 && (
          <div className="mt-2 grid grid-cols-3 gap-2">
            {photoUrls.map((url, i) => (
              // eslint-disable-next-line @next/next/no-img-element
              <div key={url} className="relative">
                <img src={url} alt="" className="h-24 w-full rounded-md object-cover" />
                <button
                  type="button"
                  onClick={() => removePhoto(i)}
                  aria-label="Odstrani sliko"
                  className="absolute right-1 top-1 flex h-7 w-7 items-center justify-center rounded-full bg-black/60 text-base text-white sm:h-6 sm:w-6 sm:text-xs"
                >
                  ×
                </button>
              </div>
            ))}
          </div>
        )}
      </fieldset>

      {type === "INTERVENCIJA" && (
        <fieldset>
          <legend className="text-sm font-medium text-gray-700 dark:text-gray-300">Krivec</legend>
          <div className="mt-2 grid grid-cols-2 gap-2">
            {[
              { value: "SLEDENJE", label: "Sledenje" },
              { value: "STRANKA", label: "Stranka" },
            ].map((c) => (
              <label
                key={c.value}
                className="flex items-center gap-2.5 rounded-md border border-gray-200 px-3.5 py-3 text-base text-gray-900 dark:border-gray-700 dark:text-gray-100 sm:px-3 sm:py-2 sm:text-sm"
              >
                <input
                  type="radio"
                  name="culprit"
                  checked={culprit === c.value}
                  onChange={() => setCulprit(c.value)}
                  className={checkboxClass()}
                />
                {c.label}
              </label>
            ))}
          </div>
        </fieldset>
      )}

      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
        Komentar
        <textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          rows={3}
          className={fieldClass()}
        />
      </label>

      <fieldset>
        <legend className="text-sm font-medium text-gray-700 dark:text-gray-300">
          Podpis stranke (neobvezno)
        </legend>
        <button
          type="button"
          onClick={() => setShowSignatureModal(true)}
          className="mt-2 flex h-24 w-full items-center justify-center rounded-md border border-gray-300 bg-white dark:border-gray-600 dark:bg-gray-800"
        >
          {signatureDataUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={signatureDataUrl} alt="Podpis stranke" className="h-full max-w-full object-contain" />
          ) : (
            <span className="text-base text-gray-500 dark:text-gray-400 sm:text-sm">Tapni za podpis</span>
          )}
        </button>
        {signatureDataUrl && (
          <button
            type="button"
            onClick={() => setSignatureDataUrl(null)}
            className="mt-2 py-1 text-base text-gray-500 underline dark:text-gray-400 sm:text-sm"
          >
            Počisti podpis
          </button>
        )}
      </fieldset>

      {error && <p className="text-sm text-red-600 dark:text-red-400">{error}</p>}

      <div className="fixed inset-x-0 bottom-0 border-t border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800">
        <button
          type="submit"
          disabled={pending}
          className="mx-auto block w-full max-w-xl rounded-md bg-blue-600 px-4 py-4 text-lg font-semibold text-white disabled:opacity-50 sm:py-3 sm:text-base"
        >
          {pending ? "Shranjujem …" : "Shrani"}
        </button>
      </div>
    </form>
  );
}
