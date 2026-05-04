# Chess Model Architecture

This document describes the current pre-render chess model target for this
branch.

## Model Style

The model is an HCE-style deterministic chess scorer. It is not a trained
neural model. Input and output shapes are fixed so decisions can be logged,
audited, and later used for training.

The model replaces legacy selection authority, not renderer authority.

Code names must follow `ChessModelContract.md`: no type or module name may carry
version suffixes or developer-facing abstraction names for the new core model.
Schema numbers and shape sizes live in companion constants.

## Live Authority Chain

The current chess authority path is:

`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`

`BoardMood`, `Story`, `StoryTable`, and `Verdict` shapes, names, family counts,
and deletion rules are fixed by `ChessModelContract.md`.

## BoardMood

`BoardMood` is the shared middle board representation:

- `48` bit slots
- `256` scalar slots
- `3,328` total values

B00..B45 are packed root transport words, B46..B47 are legal-destination
summaries, and S000..S255 are named scalar slots. Missing producer inputs are
zero-filled only through the explicit slot contract; there is no unnamed
expansion region in the live `BoardMood` contract.

## Story

`Story` is the public chess unit. It carries:

- one public `Scene`
- optional `Plan`
- optional `Tactic`
- compact identity: `side`, `target`, `anchor`, `route`, and `rival`
- `Proof` scores using the exact names from `ChessModelContract.md`

`Story.values` is exactly `160` values. It one-hot encodes public families and
stores proof scores in the proof segment.

## StoryTable

`StoryTable` orders stories into at most `8` verdicts. Lead permission depends
on public strength, truth, counterplay risk, quiet/source restrictions, and
owner-aware tactical interaction.

A plan lead is blocked only by an opposing `Tactic` or `Blunder` story at the
public floor. Source stories cannot lead over a non-source board-backed story at
the public floor.

## Verdict

`Verdict` is the language-neutral result handed to outline and renderer work.
It stores story reference, rank, role, lead permission, and strength.

`Verdict.values` is exactly `96` values.
