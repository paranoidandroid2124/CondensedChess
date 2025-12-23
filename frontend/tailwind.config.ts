import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx}",
    "./components/**/*.{js,ts,jsx,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        ink: "#0b1021",
        mist: "#e7ecff",
        accent: {
          blue: "#5b8def",
          teal: "#3dd6b7",
          purple: "#8b5cf6"
        },
        surface: {
          base: "#0b1021",
          elevated: "rgba(255, 255, 255, 0.05)",
          floating: "rgba(255, 255, 255, 0.1)",
          highlight: "rgba(255, 255, 255, 0.08)"
        },
        content: {
          primary: "#e7ecff",
          secondary: "rgba(231, 236, 255, 0.7)",
          tertiary: "rgba(231, 236, 255, 0.4)"
        },
        card: {
          base: "rgba(16, 22, 48, 0.85)",
          ring: "rgba(255, 255, 255, 0.08)"
        }
      },
      boxShadow: {
        glow: "0 10px 60px rgba(75, 108, 255, 0.25)"
      },
      backgroundImage: {
        "hero-gradient": "radial-gradient(circle at 20% 20%, rgba(91,141,239,0.2), transparent 35%), radial-gradient(circle at 80% 0%, rgba(61,214,183,0.18), transparent 35%), linear-gradient(120deg, rgba(11,16,33,0.92) 0%, rgba(11,16,33,0.96) 60%)"
      },
      fontFamily: {
        display: ["'Space Grotesk'", "Inter", "system-ui", "sans-serif"],
        body: ["'Inter'", "system-ui", "sans-serif"],
        serif: ["'Merriweather'", "serif"]
      },
      keyframes: {
        "fade-in": {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" }
        },
        "slide-up": {
          "0%": { transform: "translateY(10px)", opacity: "0" },
          "100%": { transform: "translateY(0)", opacity: "1" }
        },
        "scale-in": {
          "0%": { transform: "scale(0.95)", opacity: "0" },
          "100%": { transform: "scale(1)", opacity: "1" }
        },
        shimmer: {
          "0%": { transform: "translateX(-100%)" },
          "100%": { transform: "translateX(100%)" }
        },
        indeterminate: {
          "0%": { left: "-35%", right: "100%" },
          "60%": { left: "100%", right: "-90%" },
          "100%": { left: "100%", right: "-90%" }
        }
      },
      animation: {
        "fade-in": "fade-in 0.3s ease-out",
        "slide-up": "slide-up 0.4s ease-out",
        "scale-in": "scale-in 0.2s ease-out",
        shimmer: "shimmer 2s infinite",
        indeterminate: "indeterminate 1.5s infinite linear"
      }
    }
  },
  plugins: []
};

export default config;
