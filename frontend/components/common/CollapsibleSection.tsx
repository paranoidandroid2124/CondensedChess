import React, { useState } from "react";

interface CollapsibleSectionProps {
  title: string;
  defaultOpen?: boolean;
  children: React.ReactNode;
}

export function CollapsibleSection({ title, defaultOpen = true, children }: CollapsibleSectionProps) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="flex w-full items-center justify-between px-4 py-2 text-sm text-white/80 hover:text-white"
      >
        <span className="font-semibold">{title}</span>
        <span className="text-xs uppercase tracking-[0.18em] text-white/60">{open ? "Hide" : "Show"}</span>
      </button>
      {open ? <div className="border-t border-white/10 p-3">{children}</div> : null}
    </div>
  );
}
