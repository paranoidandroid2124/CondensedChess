"use client";

import Link from "next/link";
import React, { useState } from "react";
import { useAuth } from "@/hooks/useAuth";

export default function Navigation() {
    const { user, logout } = useAuth();
    const isAuthenticated = !!user;
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

    const publicLinks = [
        { href: "/product", label: "Product" },
        { href: "/examples", label: "Examples" },
        { href: "/pricing", label: "Pricing" },
        { href: "/faq", label: "FAQ" },
    ];

    const appLinks = [
        { href: "/app/dashboard", label: "Dashboard" }
    ];

    const navLinks = isAuthenticated ? appLinks : publicLinks;

    return (
        <nav className="glass-card sticky top-0 z-50 border-b border-white/10">
            <div className="mx-auto max-w-7xl px-6 py-4 sm:px-12 lg:px-16">
                <div className="flex items-center justify-between">
                    {/* Logo */}
                    <Link href={isAuthenticated ? "/app/dashboard" : "/"} className="flex items-center gap-2">
                        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-accent-teal to-accent-blue">
                            <span className="text-lg font-bold text-white">♟</span>
                        </div>
                        <span className="font-display text-xl text-white">CondensedChess</span>
                    </Link>

                    {/* Desktop Navigation */}
                    <div className="hidden items-center gap-8 md:flex">
                        {navLinks.map((link) => (
                            <Link
                                key={link.href}
                                href={link.href}
                                className="text-sm font-medium text-white/70 transition hover:text-white"
                            >
                                {link.label}
                            </Link>
                        ))}
                    </div>

                    {/* Desktop CTA */}
                    <div className="hidden items-center gap-3 md:flex">
                        {!isAuthenticated ? (
                            <>
                                <Link
                                    href="/login"
                                    className="text-sm font-medium text-white/70 transition hover:text-white"
                                >
                                    Login
                                </Link>
                                <Link
                                    href="/register"
                                    className="rounded-full bg-accent-teal px-4 py-2 text-sm font-semibold text-ink shadow-glow transition hover:scale-[1.02]"
                                >
                                    Sign up
                                </Link>
                            </>
                        ) : (
                            <div className="flex items-center gap-4">
                                <span className="text-sm text-white/70">Hi, {user.email}</span>
                                <button
                                    onClick={logout}
                                    className="text-sm font-medium text-white/70 transition hover:text-white"
                                >
                                    Logout
                                </button>
                                <Link
                                    href="/app/dashboard"
                                    className="rounded-full bg-accent-teal px-4 py-2 text-sm font-semibold text-ink shadow-glow transition hover:scale-[1.02]"
                                >
                                    Upload Game
                                </Link>
                            </div>
                        )}
                    </div>

                    {/* Mobile Menu Button */}
                    <button
                        onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                        className="flex flex-col gap-1.5 md:hidden"
                        aria-label="Toggle menu"
                    >
                        <span className={`h-0.5 w-6 bg-white transition ${mobileMenuOpen ? "rotate-45 translate-y-2" : ""}`} />
                        <span className={`h-0.5 w-6 bg-white transition ${mobileMenuOpen ? "opacity-0" : ""}`} />
                        <span className={`h-0.5 w-6 bg-white transition ${mobileMenuOpen ? "-rotate-45 -translate-y-2" : ""}`} />
                    </button>
                </div>

                {/* Mobile Menu */}
                {mobileMenuOpen && (
                    <div className="mt-4 flex flex-col gap-3 border-t border-white/10 pt-4 md:hidden">
                        {navLinks.map((link) => (
                            <Link
                                key={link.href}
                                href={link.href}
                                className="text-sm font-medium text-white/70 transition hover:text-white"
                                onClick={() => setMobileMenuOpen(false)}
                            >
                                {link.label}
                            </Link>
                        ))}
                        <div className="mt-2 flex flex-col gap-2 border-t border-white/10 pt-3">
                            {!isAuthenticated ? (
                                <>
                                    <Link
                                        href="/login"
                                        className="text-sm font-medium text-white/70 transition hover:text-white"
                                        onClick={() => setMobileMenuOpen(false)}
                                    >
                                        Login
                                    </Link>
                                    <Link
                                        href="/register"
                                        className="rounded-full bg-accent-teal px-4 py-2 text-center text-sm font-semibold text-ink shadow-glow transition hover:scale-[1.02]"
                                        onClick={() => setMobileMenuOpen(false)}
                                    >
                                        Sign up
                                    </Link>
                                </>
                            ) : (
                                <>
                                    <div className="text-sm text-white/70 mb-2">Hi, {user.email}</div>
                                    <button
                                        onClick={() => { logout(); setMobileMenuOpen(false); }}
                                        className="text-left text-sm font-medium text-white/70 transition hover:text-white"
                                    >
                                        Logout
                                    </button>
                                    <Link
                                        href="/app/dashboard"
                                        className="rounded-full bg-accent-teal px-4 py-2 text-center text-sm font-semibold text-ink shadow-glow transition hover:scale-[1.02]"
                                        onClick={() => setMobileMenuOpen(false)}
                                    >
                                        Upload Game
                                    </Link>
                                </>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </nav>
    );
}
