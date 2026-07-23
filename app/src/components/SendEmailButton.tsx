"use client";

import { useState } from "react";

export default function SendEmailButton({ workOrderId }: { workOrderId: string }) {
  const [email, setEmail] = useState("");
  const [open, setOpen] = useState(false);
  const [sending, setSending] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  async function handleSend() {
    setSending(true);
    setMessage(null);
    try {
      const res = await fetch(`/api/work-orders/${workOrderId}/send-email`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ to: email }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        setMessage(data.error || "Pošiljanje ni uspelo.");
        return;
      }
      setMessage("Nalog uspešno poslan.");
      setOpen(false);
    } finally {
      setSending(false);
    }
  }

  if (!open) {
    return (
      <button onClick={() => setOpen(true)} className="text-sm border border-gray-300 rounded-lg px-3 py-1.5">
        Pošlji po e-pošti
      </button>
    );
  }

  return (
    <div className="flex flex-col gap-2 border border-gray-300 rounded-lg p-3 bg-white">
      <input
        type="email"
        placeholder="prejemnik@primer.com"
        className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <div className="flex gap-2">
        <button
          onClick={handleSend}
          disabled={sending || !email}
          className="bg-gray-900 text-white rounded-lg px-3 py-1.5 text-sm disabled:opacity-50"
        >
          {sending ? "Pošiljam ..." : "Pošlji"}
        </button>
        <button onClick={() => setOpen(false)} className="text-sm text-gray-500">
          Prekliči
        </button>
      </div>
      {message && <p className="text-sm">{message}</p>}
    </div>
  );
}
