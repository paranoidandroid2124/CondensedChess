import { useCallback, useEffect, useRef } from 'react';

export type SoundType = 'move' | 'capture' | 'check';

interface AudioCache {
    move: HTMLAudioElement | null;
    capture: HTMLAudioElement | null;
    check: HTMLAudioElement | null;
}

/**
 * Hook for playing chess move sounds.
 * Sounds are preloaded on mount for instant playback.
 */
export function useChessSound(enabled: boolean = true) {
    const audioCache = useRef<AudioCache>({
        move: null,
        capture: null,
        check: null,
    });

    // Preload audio files on mount
    useEffect(() => {
        if (!enabled || typeof window === 'undefined') return;

        const loadAudio = (path: string): HTMLAudioElement => {
            const audio = new Audio(path);
            audio.preload = 'auto';
            audio.volume = 0.5;
            return audio;
        };

        audioCache.current = {
            move: loadAudio('/sounds/move.mp3'),
            capture: loadAudio('/sounds/capture.mp3'),
            check: loadAudio('/sounds/check.mp3'),
        };

        return () => {
            // Cleanup
            Object.values(audioCache.current).forEach(audio => {
                if (audio) {
                    audio.pause();
                    audio.src = '';
                }
            });
        };
    }, [enabled]);

    const playSound = useCallback((type: SoundType) => {
        if (!enabled) return;

        const audio = audioCache.current[type];
        if (audio) {
            // Reset to start for rapid successive plays
            audio.currentTime = 0;
            audio.play().catch(() => {
                // Ignore autoplay restrictions
            });
        }
    }, [enabled]);

    /**
     * Play appropriate sound based on move characteristics.
     * @param isCapture - Whether the move captured a piece
     * @param isCheck - Whether the move gives check
     */
    const playMoveSound = useCallback((isCapture: boolean, isCheck: boolean) => {
        if (isCheck) {
            playSound('check');
        } else if (isCapture) {
            playSound('capture');
        } else {
            playSound('move');
        }
    }, [playSound]);

    return { playSound, playMoveSound };
}
