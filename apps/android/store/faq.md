# Frequently Asked Questions — CounterLine

## What is CounterLine?

CounterLine is an engine-tested opening repertoire trainer. It teaches you exactly two opening lines — one for White (Vienna Gambit Accepted) and one for Black (Caro-Kann Classical 4…Bf5) — using spaced-repetition drills, annotated model games, and strategic plans.

## Why only two openings?

Depth over breadth. Instead of a database of hundreds of openings you'll never remember, CounterLine focuses on two complete weapons you can actually retain. Every move is explained. Every deviation is covered. You drill until the moves are automatic.

## What do "engine-tested" and "validated" mean?

The two opening lines were tested in engine-vs-engine matches against Stockfish 18 on a published fixed opening suite. CounterLine Combined scored 52.50% against Stockfish 18 (LOS ~93%, +17 Elo est.). The Black specialist scored 53.75% on the Caro-Kann exit. All test results, PGN files, and reproduction commands are published in the open-source repository so you can verify them.

These results represent a positive trend but are not yet statistically proven at the conventional 95% confidence threshold.

## Will this make me beat Stockfish?

No. Engine-vs-engine results at fast time controls do not automatically transfer to human play. CounterLine helps you learn sound, well-tested opening lines. Your results depend on your study, your understanding of the plans, and your middlegame and endgame skills.

## Does the app need an internet connection?

No. CounterLine works completely offline. All repertoire data, drills, and engine analysis run on your device.

## What is the spaced-repetition system?

CounterLine uses an SM-2 variant for drilling. Moves you know well are reviewed less often. Moves you struggle with come back sooner. This is the same approach used in language learning apps like Anki.

## What are "Skill Levels"?

CounterLine has four skill levels that control how much content is visible:
- **Intermediate** — core moves and basic plans
- **Advanced Club** — deeper variations and additional drills
- **Expert/Master** — full deviation coverage and tactical motifs
- **Elite Lab** — engine analysis details and proof matrix data

You can change your level in Settings at any time.

## Can I import my progress from another device?

Currently, progress data is stored locally on your device. Local backup/restore is planned for a future release.

## Is this app free?

Yes. CounterLine is free and open source under GPL-3.0. There are no in-app purchases, no ads, and no subscriptions.

## Where can I find the source code?

The complete source code is on GitHub: https://github.com/official-stockfish/Stockfish (branch: exp/integrated-sf18-killer)

## What openings are included?

- **White**: Vienna Gambit Accepted (ECO C29) — 1.e4 e5 2.Nc3 Nf6 3.f4 exf4 4.e5…
- **Black**: Caro-Kann Classical 4…Bf5 (ECO B18) — 1.e4 c6 2.d4 d5 3.Nc3 dxe4 4.Nxe4 Bf5…

## Will more openings be added?

The repertoire is designed to be swappable. Legacy lines (QGD Exchange for White, Petroff for Black) exist in the repository but are not actively tested against the current Stockfish version. New lines can be added by following the documented process.

## What is the "proof matrix"?

The proof matrix is a set of nine engine-vs-engine matches that validate the repertoire. It includes matches against Stockfish 18 for each line individually and combined, plus a null-wrapper control that confirms any gains come from the specialist opening knowledge rather than the wrapper overhead.
