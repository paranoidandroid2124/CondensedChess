"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { apiFetch } from "../lib/api";
import { useAuth } from "../hooks/useAuth";

export default function UploadCard() {
  const router = useRouter();
  const [pgn, setPgn] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    if (!pgn.trim()) {
      setError("Please enter PGN text.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      // Store PGN in localStorage for immediate display
      localStorage.setItem("pending-pgn", pgn);

      const data = await apiFetch<{ jobId: string }>('/api/game-review/chapter', {
        method: "POST",
        body: JSON.stringify({ pgn })
      });

      const jobId = data.jobId;
      if (!jobId) {
        localStorage.removeItem("pending-pgn");
        throw new Error("Missing jobId");
      }
      router.push(`/review/${jobId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Request failed");
    } finally {
      setLoading(false);
    }
  };

  const handleFile = async (file?: File | null) => {
    if (!file) return;
    const text = await file.text();
    setPgn(text);
  };

  const { user } = useAuth();

  return (
    <div className="glass-card rounded-3xl p-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-white/60">Upload</p>
          <h2 className="font-display text-xl text-white">Upload PGN and get a review</h2>
          <p className="text-sm text-white/70">Run full analysis and jump to the review page when ready.</p>
        </div>
        <label className="inline-flex cursor-pointer items-center rounded-full border border-white/20 px-4 py-2 text-xs font-semibold text-white/80 hover:border-white/40">
          Choose file
          <input type="file" accept=".pgn,text/plain" className="hidden" onChange={(e) => handleFile(e.target.files?.[0])} />
        </label>
      </div>
      <div className="mt-4">
        <textarea
          className="w-full rounded-2xl border border-white/10 bg-black/20 p-3 text-sm text-white outline-none focus:border-accent-teal focus:ring-2 focus:ring-accent-teal/40"
          rows={8}
          placeholder="Paste PGN text here."
          value={pgn}
          onChange={(e) => setPgn(e.target.value)}
        />
      </div>
      {error ? <p className="mt-2 text-sm text-rose-300">{error}</p> : null}
      <div className="mt-4 flex flex-wrap items-center gap-3">
        {user ? (
          <button
            onClick={submit}
            disabled={loading}
            className="inline-flex items-center justify-center rounded-full bg-accent-teal px-5 py-2 text-sm font-semibold text-ink shadow-glow transition hover:scale-[1.01] disabled:opacity-60"
          >
            {loading ? "Analyzing..." : "Analyze"}
          </button>
        ) : (
          <button
            onClick={() => router.push("/login?returnUrl=/review")}
            className="inline-flex items-center justify-center rounded-full bg-accent-blue px-5 py-2 text-sm font-semibold text-white shadow-glow transition hover:scale-[1.01]"
          >
            Login to Analyze
          </button>
        )}
        <button
          onClick={() => router.push("/review/sample")}
          className="inline-flex items-center justify-center rounded-full border border-white/20 px-4 py-2 text-xs font-semibold text-white/80 hover:border-white/40"
        >
          View sample
        </button>
        <span className="text-xs text-white/50">Free Tier: 1 game/day</span>
      </div>
    </div>
  );
}
