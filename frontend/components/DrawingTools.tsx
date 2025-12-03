import React from "react";

type DrawingColor = "green" | "red" | "blue" | "orange";

type DrawingToolsProps = {
    selectedColor: DrawingColor;
    onSelectColor: (color: DrawingColor) => void;
    onClear: () => void;
};

export function DrawingTools({ selectedColor, onSelectColor, onClear }: DrawingToolsProps) {
    const colors: { id: DrawingColor; hex: string; label: string }[] = [
        { id: "green", hex: "#22c55e", label: "Best Move (Alt+LClick)" },
        { id: "red", hex: "#ef4444", label: "Mistake (Shift+LClick)" },
        { id: "blue", hex: "#3b82f6", label: "Alternative (Alt+Shift+LClick)" },
        { id: "orange", hex: "#f97316", label: "Idea (Ctrl+LClick)" },
    ];

    return (
        <div className="absolute top-2 right-2 z-20 flex flex-col gap-2 rounded-lg border border-white/10 bg-black/60 p-2 backdrop-blur-sm transition-opacity hover:opacity-100 opacity-0 group-hover:opacity-100">
            {colors.map((c) => (
                <button
                    key={c.id}
                    onClick={() => onSelectColor(c.id)}
                    className={`h-6 w-6 rounded-full border-2 transition-transform hover:scale-110 ${selectedColor === c.id ? "border-white scale-110" : "border-transparent"
                        }`}
                    style={{ backgroundColor: c.hex }}
                    title={c.label}
                />
            ))}
            <hr className="border-white/10 my-1" />
            <button
                onClick={onClear}
                className="flex h-6 w-6 items-center justify-center rounded-full bg-white/10 text-white/60 hover:bg-white/20 hover:text-white"
                title="Clear Arrows (Click board)"
            >
                ✕
            </button>
        </div>
    );
}
