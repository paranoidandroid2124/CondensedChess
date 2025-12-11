import React from 'react';
import { BookSection } from '@/types/StudyModel';
import { OpeningHero } from './sections/OpeningHero';
import { CrisisView } from './sections/CrisisView';
import { StandardSection } from './sections/StandardSection';
import { AnimatePresence, motion } from 'framer-motion';

interface Props {
    section: BookSection;
    onDiagramClick?: (fen: string) => void;
}

export const ReadingMode: React.FC<Props> = (props) => {
    const { section } = props;

    // Determine the component to render
    const Component = (() => {
        switch (section.sectionType) {
            case 'OpeningReview': return OpeningHero;
            case 'TurningPoints': return CrisisView;
            default: return StandardSection;
        }
    })();

    return (
        <AnimatePresence mode="wait">
            <motion.div
                key={section.title + section.startPly} // Key changes trigger animation
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                transition={{ duration: 0.3 }}
                className="flex-1 flex flex-col overflow-hidden h-full"
            >
                <Component {...props} />
            </motion.div>
        </AnimatePresence>
    );
};

