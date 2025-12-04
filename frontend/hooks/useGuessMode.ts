import { useState } from "react";

export type GuessState = "waiting" | "correct" | "incorrect" | "giveup";

export function useGuessMode() {
  const [isGuessing, setIsGuessing] = useState(false);
  const [guessState, setGuessState] = useState<GuessState>("waiting");
  const [guessFeedback, setGuessFeedback] = useState<string | undefined>();

  return {
    isGuessing,
    guessState,
    guessFeedback,
    setIsGuessing,
    setGuessState,
    setGuessFeedback
  };
}

