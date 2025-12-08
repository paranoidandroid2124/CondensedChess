import React from "react";

interface PaperLayoutProps {
    children: React.ReactNode;
    className?: string;
}

export function PaperLayout({ children, className = "" }: PaperLayoutProps) {
    return (
        <div className={`
            font-serif text-lg leading-relaxed text-neutral-200 
            bg-[#1a1a1a] /* Slightly lighter than pure black for contrast */
            p-8 md:p-12 max-w-3xl mx-auto
            shadow-2xl shadow-black/50
            min-h-screen
            selection:bg-yellow-900/50 selection:text-yellow-100
            ${className}
        `}>
            {children}
        </div>
    );
}
