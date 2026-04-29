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
    return { key: `${index}:${block.role}`, block, label: blockLabel(block), text, line };
  });
}

function blockLabel(block: RenderBlock): string {
  if (block.role === 'context') return 'Context';
  if (block.role === 'contrast') return 'Reply';
  return 'Move note';
}

function publicLineForBlock(render: PublicRender, block: RenderBlock): string | null {
  const ids = block.variationEvidenceIds;
  if (!ids?.length || !render.variationEvidence?.length) return null;

  const allowedIds = new Set(ids);
  const line = render.variationEvidence.find(item => item.boundClaimId === block.claimId && allowedIds.has(item.proofId) && isPublicLine(item));
  if (!line) return null;

  const san = line.lineSan.map(move => move.trim()).filter(Boolean);
  return san.length ? san.join(' ') : null;
}

function isPublicLine(line: PublicLine): boolean {
  return line.surfaceAllowance === 'public_line' && Array.isArray(line.lineSan) && line.lineSan.length > 0;
}
