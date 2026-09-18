"use client";

import { useEffect, useState, useActionState } from "react";
import { useRouter } from "next/navigation";
import { ClientCombobox } from "@/components/client-combobox";
import { DateTimeInput } from "@/components/date-input";
import { createPlanGroup } from "./actions";
import { GroupPopup } from "./group-popup";

const PALETTE = [
  "#2563eb", "#9333ea", "#0d9488", "#db2777", "#d97706",
  "#4338ca", "#0891b2", "#e11d48", "#65a30d", "#c026d3",
];
const DAY_LABELS = ["Pon", "Tor", "Sre", "Čet", "Pet", "Sob", "Ned"];
const PX_PER_HOUR = 40;
const HOURS = Array.from({ length: 24 }, (_, i) => i);

// d.toISOString() vrne UTC datum, d.setDate/getDate pa delata v lokalnem času -- lokalna polnoč
// pade na prejšnji dan v UTC, zato bi toISOString().slice(0,10) tu vrnil napačen dan. Za "lokalni
// koledarski dan" sestavimo niz iz lokalnih komponent, ne iz UTC.
function localDateStr(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

// Vrednost "datetime-local" nima conske -- new Date(...) jo NA STRANI BRSKALNIKA pravilno tolmaci
// kot lokalni cas uporabnika. Vsebnik teče v UTC, zato mora priti do njega ze pretvorjena v pravi
// UTC trenutek (namesto da bi streznik isti niz sam narobe tolmacil kot svoj lokalni UTC cas).
export function localDateTimeToIso(value: string): string {
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? "" : d.toISOString();
}

// Obratna smer od localDateTimeToIso -- za predizpolnitev datetime-local vnosa ob urejanju
// obstoječega dogodka (shranjeni startAt/endAt sta pravi UTC ISO, vnosno polje pa pričakuje
// lokalni "YYYY-MM-DDTHH:mm").
export function isoToLocalDateTimeStr(iso: string): string {
  const d = new Date(iso);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${y}-${m}-${day}T${hh}:${mm}`;
}

function hashToIndex(input: string, mod: number): number {
  let hash = 0;
  for (let i = 0; i < input.length; i++) hash = (hash * 31 + input.charCodeAt(i)) | 0;
  return Math.abs(hash) % mod;
}

function fieldClass() {
  return "mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100";
}

export type PlannedTaskItem = {
  id: string;
  vehiclePlate: string;
  note: string | null;
  expectedInstaller: string | null;
  expectedInstallerOtherText: string | null;
  workOrderId: string | null;
};

export type PlanGroupItem = {
  id: string;
  clientId: string;
  clientName: string;
  startAt: string;
  endAt: string;
  note: string | null;
  tasks: PlannedTaskItem[];
};

type PositionedGroup = PlanGroupItem & { lane: number; laneCount: number };

// Prekrivajoce se dogodke istega dne razporedi v "steze" (kot Outlook/Google Calendar dnevni
// pogled), da se vizualno ne prekrivajo -- pohlepni algoritem: vsak dogodek gre v prvo prosto
// stezo, katere zadnji dogodek se je ze koncal.
function packGroupsForDay(dayGroups: PlanGroupItem[]): PositionedGroup[] {
  const sorted = [...dayGroups].sort((a, b) => a.startAt.localeCompare(b.startAt));
  const laneEndTimes: string[] = [];
  const placed: (PlanGroupItem & { lane: number })[] = [];

  for (const g of sorted) {
    let laneIndex = laneEndTimes.findIndex((endAt) => endAt <= g.startAt);
    if (laneIndex === -1) {
      laneIndex = laneEndTimes.length;
      laneEndTimes.push(g.endAt);
    } else {
      laneEndTimes[laneIndex] = g.endAt;
    }
    placed.push({ ...g, lane: laneIndex });
  }

  return placed.map((g) => {
    const overlapping = placed.filter((other) => other.startAt < g.endAt && other.endAt > g.startAt);
    const laneCount = Math.max(...overlapping.map((o) => o.lane + 1), g.lane + 1);
    return { ...g, laneCount };
  });
}

export function PlanCalendar({
  weekStartIso,
  clients,
  vehiclePlates,
  groups,
  canManagePlan,
}: {
  weekStartIso: string;
  clients: { id: string; name: string }[];
  vehiclePlates: string[];
  groups: PlanGroupItem[];
  canManagePlan: boolean;
}) {
  const router = useRouter();
  const weekStart = new Date(weekStartIso);
  const days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(weekStart);
    d.setDate(d.getDate() + i);
    return d;
  });

  const [modalOpen, setModalOpen] = useState(false);
  const [modalKey, setModalKey] = useState(0);
  const [modalStartDefault, setModalStartDefault] = useState("");
  const [clientId, setClientId] = useState("");
  const [startAtIso, setStartAtIso] = useState("");
  const [endAtIso, setEndAtIso] = useState("");
  const [selectedGroupId, setSelectedGroupId] = useState<string | null>(null);
  const [state, formAction, pending] = useActionState(createPlanGroup, undefined);

  // Nikoli zamrznjena kopija -- izbrana skupina se sveže poišče iz `groups` ob vsakem renderju, da
  // odprt pojavni pano takoj odraža nov nalog po revalidatePath("/plan") (npr. po addPlannedTask).
  const selectedGroup = groups.find((g) => g.id === selectedGroupId) ?? null;

  useEffect(() => {
    if (state?.success) {
      setModalOpen(false);
      // Takoj odpri ravno ustvarjeno skupino, da je dodajanje nalogov mogoče brez dodatnega klika
      // na koledarju.
      if (state.groupId) setSelectedGroupId(state.groupId);
    }
  }, [state]);

  function groupColor(clientId: string) {
    return PALETTE[hashToIndex(clientId, PALETTE.length)];
  }

  // startDefault: "YYYY-MM-DDTHH:mm" v lokalnem času -- prazen niz za ročni vnos (gumb "Nov
  // dogodek"), sicer predizpolnjen iz kliknjenega kvadratka v koledarju.
  function openCreateModal(startDefault: string = "") {
    setClientId("");
    setModalStartDefault(startDefault);
    setStartAtIso(startDefault ? localDateTimeToIso(startDefault) : "");
    setEndAtIso("");
    setModalKey((k) => k + 1);
    setModalOpen(true);
  }

  // Klik na prazen del dneva v koledarju -- izračuna uro/dan iz Y-položaja klika (zaokroženo na 30
  // min) in odpre ustvarjalni obrazec z že izpolnjenim začetkom; konec ostane za ročni vnos.
  function handleDayColumnClick(e: React.MouseEvent<HTMLDivElement>, dayIdx: number) {
    if (!canManagePlan) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const offsetY = e.clientY - rect.top;
    const totalMinutes = Math.max(0, Math.min(23 * 60 + 30, Math.round(((offsetY / PX_PER_HOUR) * 60) / 30) * 30));
    const hour = Math.floor(totalMinutes / 60);
    const minute = totalMinutes % 60;
    const day = days[dayIdx];
    const y = day.getFullYear();
    const m = String(day.getMonth() + 1).padStart(2, "0");
    const d = String(day.getDate()).padStart(2, "0");
    const hh = String(hour).padStart(2, "0");
    const mm = String(minute).padStart(2, "0");
    openCreateModal(`${y}-${m}-${d}T${hh}:${mm}`);
  }

  function goToWeek(offsetDays: number) {
    const d = new Date(weekStart);
    d.setDate(d.getDate() + offsetDays);
    const params = new URLSearchParams(window.location.search);
    params.set("teden", localDateStr(d));
    router.push(`/plan?${params.toString()}`);
  }

  function goToday() {
    const params = new URLSearchParams(window.location.search);
    params.delete("teden");
    router.push(`/plan?${params.toString()}`);
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => goToWeek(-7)}
            className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white"
          >
            ‹ Prejšnji teden
          </button>
          <button
            type="button"
            onClick={goToday}
            className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white"
          >
            Danes
          </button>
          <button
            type="button"
            onClick={() => goToWeek(7)}
            className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white"
          >
            Naslednji teden ›
          </button>
          <span className="ml-1 text-sm font-medium text-gray-700 dark:text-gray-300">
            {days[0].toLocaleDateString("sl-SI", { day: "2-digit", month: "2-digit" })} –{" "}
            {days[6].toLocaleDateString("sl-SI", { day: "2-digit", month: "2-digit", year: "numeric" })}
          </span>
        </div>
        {canManagePlan && (
          <button
            type="button"
            onClick={() => openCreateModal()}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white"
          >
            Nov dogodek
          </button>
        )}
      </div>

      <div className="overflow-hidden rounded-md border border-gray-200 dark:border-gray-700">
        <div className="grid grid-cols-[56px_repeat(7,1fr)] border-b border-gray-200 bg-gray-50 dark:border-gray-700 dark:bg-gray-800">
          <div />
          {days.map((d, i) => (
            <div
              key={i}
              className="border-l border-gray-200 px-2 py-2 text-center text-xs font-medium text-gray-600 dark:border-gray-700 dark:text-gray-300"
            >
              {DAY_LABELS[i]} <span className="text-gray-400 dark:text-gray-500">{d.getDate()}.{d.getMonth() + 1}.</span>
            </div>
          ))}
        </div>
        <div className="max-h-[70vh] overflow-y-auto">
          <div className="relative grid grid-cols-[56px_repeat(7,1fr)]" style={{ height: HOURS.length * PX_PER_HOUR }}>
            <div className="relative">
              {HOURS.map((h) => (
                <div
                  key={h}
                  className="absolute right-1 -translate-y-1/2 text-[10px] text-gray-400 dark:text-gray-500"
                  style={{ top: h * PX_PER_HOUR }}
                >
                  {String(h).padStart(2, "0")}:00
                </div>
              ))}
            </div>
            {days.map((day, dayIdx) => {
              const dayStart = new Date(day);
              dayStart.setHours(0, 0, 0, 0);
              const dayEnd = new Date(dayStart);
              dayEnd.setDate(dayEnd.getDate() + 1);
              const dayGroups = groups.filter(
                (g) => new Date(g.startAt) < dayEnd && new Date(g.endAt) > dayStart
              );
              const positioned = packGroupsForDay(dayGroups);
              return (
                <div
                  key={dayIdx}
                  onClick={(e) => handleDayColumnClick(e, dayIdx)}
                  className={`relative border-l border-gray-100 dark:border-gray-800 ${canManagePlan ? "cursor-pointer" : ""}`}
                >
                  {HOURS.map((h) => (
                    <div
                      key={h}
                      className="absolute left-0 right-0 border-t border-gray-100 dark:border-gray-800"
                      style={{ top: h * PX_PER_HOUR }}
                    />
                  ))}
                  {positioned.map((g) => {
                    const clampedStart = Math.max(new Date(g.startAt).getTime(), dayStart.getTime());
                    const clampedEnd = Math.min(new Date(g.endAt).getTime(), dayEnd.getTime());
                    const topHours = (clampedStart - dayStart.getTime()) / 3_600_000;
                    const durationHours = Math.max((clampedEnd - clampedStart) / 3_600_000, 0.25);
                    const widthPct = 100 / g.laneCount;
                    const doneCount = g.tasks.filter((t) => t.workOrderId).length;
                    return (
                      <button
                        key={g.id}
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedGroupId(g.id);
                        }}
                        className="absolute overflow-hidden rounded px-1 py-0.5 text-left text-[11px] leading-tight text-white shadow-sm"
                        style={{
                          top: topHours * PX_PER_HOUR,
                          height: durationHours * PX_PER_HOUR - 2,
                          left: `${g.lane * widthPct}%`,
                          width: `calc(${widthPct}% - 2px)`,
                          backgroundColor: groupColor(g.clientId),
                        }}
                        title={g.clientName}
                      >
                        <div className="truncate font-semibold">{g.clientName}</div>
                        <div className="truncate">
                          {g.tasks.length > 0 ? `${doneCount}/${g.tasks.length} opravljenih` : "brez nalogov"}
                        </div>
                      </button>
                    );
                  })}
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {selectedGroup && (
        <GroupPopup
          key={selectedGroup.id}
          group={selectedGroup}
          clients={clients}
          vehiclePlates={vehiclePlates}
          canManagePlan={canManagePlan}
          onClose={() => setSelectedGroupId(null)}
        />
      )}

      {modalOpen && (
        <div
          className="fixed inset-0 z-30 flex items-center justify-center bg-black/30"
          onClick={() => setModalOpen(false)}
        >
          <form
            action={formAction}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md space-y-3 rounded-md border border-gray-200 bg-white p-4 shadow-lg dark:border-gray-700 dark:bg-gray-900"
          >
            <h3 className="text-sm font-medium text-gray-900 dark:text-gray-100">Nov dogodek</h3>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Stranka</label>
              <ClientCombobox clients={clients} value={clientId} onChange={setClientId} required />
              <input type="hidden" name="clientId" value={clientId} />
            </div>
            <input type="hidden" name="startAt" value={startAtIso} />
            <input type="hidden" name="endAt" value={endAtIso} />
            <div className="grid grid-cols-2 gap-3">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Od
                <DateTimeInput
                  key={`start-${modalKey}`}
                  withTime
                  required
                  defaultValue={modalStartDefault}
                  onValueChange={(v) => setStartAtIso(localDateTimeToIso(v))}
                />
              </label>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Do
                <DateTimeInput
                  key={`end-${modalKey}`}
                  withTime
                  required
                  onValueChange={(v) => setEndAtIso(localDateTimeToIso(v))}
                />
              </label>
            </div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Opomba (neobvezno)
              <textarea name="note" rows={2} className={fieldClass()} />
            </label>
            {state?.error && <p className="text-sm text-red-600 dark:text-red-400">{state.error}</p>}
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 dark:border-gray-600 dark:text-gray-300"
              >
                Prekliči
              </button>
              <button
                type="submit"
                disabled={pending || !startAtIso || !endAtIso}
                className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
              >
                {pending ? "Shranjujem …" : "Ustvari"}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
