"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import {
  WORK_ORDER_TYPE_LABELS,
  DIFFICULTY_LABELS,
  INSTALLER_NAME_LABELS,
  OPTION_TYPE_LABELS,
  WORK_ORDER_STATUS_LABELS,
} from "@/lib/constants";

type Client = { id: string; name: string };

type WorkOrderRow = {
  id: string;
  ident: string;
  orderDate: string;
  type: string;
  difficulty: string;
  vehiclePlate: string;
  imei: string;
  status: string;
  client: { name: string };
  installers: { name: string; otherText: string | null }[];
  createdBy: { fullName: string };
};

type Props = {
  canExport: boolean;
};

const emptyFilters = {
  dateFrom: "",
  dateTo: "",
  clientId: "",
  plate: "",
  imei: "",
  type: "",
  difficulty: "",
  installer: "",
  optionType: "",
};

export default function AdminOrdersTable({ canExport }: Props) {
  const [clients, setClients] = useState<Client[]>([]);
  const [filters, setFilters] = useState(emptyFilters);
  const [appliedFilters, setAppliedFilters] = useState(emptyFilters);
  const [sortBy, setSortBy] = useState("orderDate");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("desc");
  const [page, setPage] = useState(1);
  const [items, setItems] = useState<WorkOrderRow[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const pageSize = 25;

  useEffect(() => {
    fetch("/api/clients")
      .then((r) => r.json())
      .then(setClients)
      .catch(() => {});
  }, []);

  const buildParams = useCallback(
    (extra?: Record<string, string>) => {
      const params = new URLSearchParams();
      Object.entries(appliedFilters).forEach(([k, v]) => {
        if (v) params.set(k, v);
      });
      params.set("sortBy", sortBy);
      params.set("sortDir", sortDir);
      params.set("page", String(page));
      params.set("pageSize", String(pageSize));
      if (extra) Object.entries(extra).forEach(([k, v]) => params.set(k, v));
      return params;
    },
    [appliedFilters, sortBy, sortDir, page]
  );

  useEffect(() => {
    setLoading(true);
    fetch(`/api/work-orders?${buildParams().toString()}`)
      .then((r) => r.json())
      .then((data) => {
        setItems(data.items || []);
        setTotal(data.total || 0);
      })
      .finally(() => setLoading(false));
  }, [buildParams]);

  function applyFilters() {
    setPage(1);
    setAppliedFilters(filters);
  }

  function resetFilters() {
    setFilters(emptyFilters);
    setAppliedFilters(emptyFilters);
    setPage(1);
  }

  function toggleSort(field: string) {
    if (sortBy === field) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortBy(field);
      setSortDir("asc");
    }
  }

  function exportUrl() {
    const params = buildParams();
    params.delete("page");
    params.delete("pageSize");
    return `/api/work-orders/export?${params.toString()}`;
  }

  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  return (
    <div className="space-y-4">
      <div className="bg-white border border-gray-200 rounded-lg p-4 grid grid-cols-2 md:grid-cols-4 gap-3">
        <div>
          <label className="block text-xs text-gray-500 mb-1">Datum od</label>
          <input
            type="date"
            className="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-sm"
            value={filters.dateFrom}
            onChange={(e) => setFilters((f) => ({ ...f, dateFrom: e.target.value }))}
          />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">Datum do</label>
          <input
            type="date"
            className="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-sm"
            value={filters.dateTo}
            onChange={(e) => setFilters((f) => ({ ...f, dateTo: e.target.value }))}
          />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">Stranka</label>
          <select
            className="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-sm"
            value={filters.clientId}
            onChange={(e) => setFilters((f) => ({ ...f, clientId: e.target.value }))}
          >
            <option value="">Vse</option>
            {clients.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">Registrska št.</label>
          <input
            className="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-sm"
            value={filters.plate}
            onChange={(e) => setFilters((f) => ({ ...f, plate: e.target.value }))}
          />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">IMEI</label>
          <input
            className="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-sm"
            value={filters.imei}
            onChange={(e) => setFilters((f) => ({ ...f, imei: e.target.value }))}
          />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">Tip naloga</label>
          <select
            className="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-sm"
            value={filters.type}
            onChange={(e) => setFilters((f) => ({ ...f, type: e.target.value }))}
          >
            <option value="">Vsi</option>
            {Object.entries(WORK_ORDER_TYPE_LABELS).map(([k, l]) => (
              <option key={k} value={k}>
                {l}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">Zahtevnost</label>
          <select
            className="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-sm"
            value={filters.difficulty}
            onChange={(e) => setFilters((f) => ({ ...f, difficulty: e.target.value }))}
          >
            <option value="">Vse</option>
            {Object.entries(DIFFICULTY_LABELS).map(([k, l]) => (
              <option key={k} value={k}>
                {l}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">Monter</label>
          <select
            className="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-sm"
            value={filters.installer}
            onChange={(e) => setFilters((f) => ({ ...f, installer: e.target.value }))}
          >
            <option value="">Vsi</option>
            {Object.entries(INSTALLER_NAME_LABELS).map(([k, l]) => (
              <option key={k} value={k}>
                {l}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">Izbira (DIN/ANI/CAN)</label>
          <select
            className="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-sm"
            value={filters.optionType}
            onChange={(e) => setFilters((f) => ({ ...f, optionType: e.target.value }))}
          >
            <option value="">Vse</option>
            {Object.entries(OPTION_TYPE_LABELS).map(([k, l]) => (
              <option key={k} value={k}>
                {l}
              </option>
            ))}
          </select>
        </div>

        <div className="col-span-2 md:col-span-4 flex gap-2 pt-1">
          <button onClick={applyFilters} className="bg-gray-900 text-white rounded-lg px-4 py-1.5 text-sm">
            Filtriraj
          </button>
          <button onClick={resetFilters} className="border border-gray-300 rounded-lg px-4 py-1.5 text-sm">
            Počisti
          </button>
          {canExport && (
            <a href={exportUrl()} className="ml-auto border border-gray-300 rounded-lg px-4 py-1.5 text-sm">
              Izvozi v Excel ({total})
            </a>
          )}
        </div>
      </div>

      <div className="bg-white border border-gray-200 rounded-lg overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-gray-200 text-left">
              {[
                ["ident", "Ident"],
                ["orderDate", "Datum"],
                ["", "Stranka"],
                ["type", "Tip"],
                ["difficulty", "Zahtevnost"],
                ["vehiclePlate", "Registrska"],
                ["imei", "IMEI"],
                ["", "Monterji"],
                ["", "Status"],
                ["", ""],
              ].map(([field, label], i) => (
                <th
                  key={i}
                  className={`px-3 py-2 whitespace-nowrap ${field ? "cursor-pointer select-none" : ""}`}
                  onClick={() => field && toggleSort(field)}
                >
                  {label}
                  {field && sortBy === field ? (sortDir === "asc" ? " ▲" : " ▼") : ""}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {items.map((o) => (
              <tr key={o.id} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="px-3 py-2 font-mono">{o.ident}</td>
                <td className="px-3 py-2 whitespace-nowrap">{new Date(o.orderDate).toLocaleDateString("sl-SI")}</td>
                <td className="px-3 py-2">{o.client.name}</td>
                <td className="px-3 py-2">{WORK_ORDER_TYPE_LABELS[o.type]}</td>
                <td className="px-3 py-2">{DIFFICULTY_LABELS[o.difficulty]}</td>
                <td className="px-3 py-2">{o.vehiclePlate}</td>
                <td className="px-3 py-2 font-mono">{o.imei}</td>
                <td className="px-3 py-2">
                  {o.installers.map((i) => (i.name === "OSTALO" ? i.otherText : INSTALLER_NAME_LABELS[i.name])).join(", ")}
                </td>
                <td className="px-3 py-2">{WORK_ORDER_STATUS_LABELS[o.status]}</td>
                <td className="px-3 py-2">
                  <Link href={`/nalogi/${o.id}`} className="text-blue-600 hover:underline">
                    Odpri
                  </Link>
                </td>
              </tr>
            ))}
            {items.length === 0 && !loading && (
              <tr>
                <td colSpan={10} className="px-3 py-6 text-center text-gray-500">
                  Ni rezultatov.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between text-sm">
        <span className="text-gray-500">
          Stran {page} / {totalPages} ({total} nalogov)
        </span>
        <div className="flex gap-2">
          <button
            disabled={page <= 1}
            onClick={() => setPage((p) => p - 1)}
            className="border border-gray-300 rounded-lg px-3 py-1 disabled:opacity-40"
          >
            Nazaj
          </button>
          <button
            disabled={page >= totalPages}
            onClick={() => setPage((p) => p + 1)}
            className="border border-gray-300 rounded-lg px-3 py-1 disabled:opacity-40"
          >
            Naprej
          </button>
        </div>
      </div>
    </div>
  );
}
