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
      }
    }
  },
  plugins: []
};

export default config;
