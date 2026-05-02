import type { PublicCommentaryRender, RenderBlock } from './commentaryBridge';
import type { MoveExplanationReadyState } from './moveExplanation';

type PublicRender = Extract<PublicCommentaryRender, { kind: 'render' }>;
type PublicLine = NonNullable<PublicRender['variationEvidence']>[number];

export type MoveExplanationSurfaceBlock = {
  key: string;
  block: RenderBlock;
  label: string;
  text: string;
  line: string | null;
};

export function visibleMoveExplanationBlocks(state: MoveExplanationReadyState): MoveExplanationSurfaceBlock[] {
  return state.render.blocks.flatMap((block, index) => {
    const text = (block.text.publicText || '').trim();
    const line = publicLineForBlock(state.render, block);
    if (!text && !line) return [];
    return { key: `${index}:${block.role}`, block, label: '', text, line };
  });
}

function publicLineForBlock(render: PublicRender, block: RenderBlock): string | null {
  const ids = block.variationEvidenceIds;
  if (!ids?.length || !render.variationEvidence?.length || !blockAllowsLineCommentary(block, render)) return null;

  const allowedIds = new Set(ids);
  const line = render.variationEvidence.find(item => item.boundClaimId === block.claimId && allowedIds.has(item.proofId) && isPublicLine(item));
  if (!line) return null;

  const san = line.lineSan.map(move => move.trim()).filter(Boolean);
  return san.length ? san.join(' ') : null;
}

function isPublicLine(line: PublicLine): boolean {
  return line.surfaceAllowance === 'public_line' && Array.isArray(line.lineSan) && line.lineSan.length > 0;
}

function blockAllowsLineCommentary(block: RenderBlock, render: PublicRender): boolean {
  const capability = block.phraseCapability;
  if (!capability || !render.wording.allowedPublicText || block.role !== 'primary') return false;
  return (
    capability.allowsLineCommentary &&
    capability.allowedPredicates.includes('line_commentary') &&
    !capability.allowsResultLanguage &&
    !capability.allowsBestForcedLanguage &&
    !capability.allowsEngineLanguage &&
    wordingRank(block.wordingStrength) >= wordingRank('qualified_support') &&
    wordingRank(capability.maxStrength) >= wordingRank(block.wordingStrength) &&
    wordingRank(render.wording.maxStrength) >= wordingRank(block.wordingStrength)
  );
}

function wordingRank(strength: string): number {
  switch (strength) {
    case 'hidden':
      return 0;
    case 'negative_only':
      return 1;
    case 'context_only':
      return 2;
    case 'qualified_support':
      return 3;
    case 'assertive_certified':
      return 4;
    default:
      return -1;
  }
}
