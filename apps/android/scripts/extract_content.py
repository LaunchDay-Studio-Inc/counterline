#!/usr/bin/env python3
"""
Extract content from repo manifests and book chapters into JSON asset files
for the CounterLine Android app.

Usage:
    python3 apps/android/scripts/extract_content.py

Outputs JSON files to apps/android/app/src/main/assets/content/
"""

import json
import os
import sys

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..'))
ASSET_DIR = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'assets', 'content')


def load_json(path: str) -> dict:
    full = os.path.join(REPO_ROOT, path)
    with open(full, 'r') as f:
        return json.load(f)


def extract_claims() -> dict:
    """Extract claims_manifest.json → claims.json"""
    manifest = load_json('content/claims_manifest.json')
    return {
        'version': manifest['version'],
        'approved_headline': manifest['approved_headline'],
        'approved_subtitle': manifest['approved_subtitle'],
        'approved_badges': manifest['approved_badges'],
        'approved_promises': manifest['approved_promises'],
        'forbidden_phrases': manifest['forbidden_phrases'],
        'proof_status': manifest['proof_status'],
        'scope_statement': manifest['scope_statement'],
        'allowed_performance_statements': manifest['allowed_performance_statements'],
        'required_disclaimers': manifest['required_disclaimers'],
    }


def extract_proof() -> list:
    """Extract proof_manifest.json → proof.json (list of matches)"""
    manifest = load_json('content/proof_manifest.json')
    matches = []
    for m in manifest['matches']:
        matches.append({
            'id': m['id'],
            'label': m['label'],
            'suite': m['suite'],
            'engine1': m['engine1'],
            'engine2': m['engine2'],
            'wins': m['w'],
            'losses': m['l'],
            'draws': m['d'],
            'score': m['score'],
            'pct': m['pct'],
            'asWhite': m['as_white'],
            'asBlack': m['as_black'],
            'finding': m.get('finding'),
            'eloEstimate': m.get('elo_estimate'),
        })
    return matches


def extract_repertoire() -> list:
    """Extract repertoire_manifest.json → repertoire.json"""
    manifest = load_json('content/repertoire_manifest.json')
    lines = []

    for side_key, side_val in [('white', 'WHITE'), ('black', 'BLACK')]:
        entry = manifest['current_repertoire'][side_key]
        seed = entry['seed_line']
        moves = parse_seed_line(seed, side_val)
        lines.append({
            'id': f'{side_val.lower()}_{entry["family"]}',
            'name': entry['name'],
            'family': entry['family'],
            'eco': entry['eco'],
            'side': side_val,
            'seedLine': seed,
            'exitFen': entry['exit_fen'],
            'exitEpd': entry['exit_epd'],
            'exitMoveNumber': entry['exit_move_number'],
            'specialistType': entry['specialist_type'],
            'specialistSize': entry['specialist_size'],
            'screeningRank': entry['screening_rank'],
            'screeningScorePct': entry['screening_score_pct'],
            'evaluationAtExit': entry['evaluation_at_exit'],
            'moves': moves,
            'memoryHook': get_memory_hook(side_val),
            'memoryHookBreakdown': get_memory_hook_breakdown(side_val),
        })

    return lines


def parse_seed_line(seed: str, side: str) -> list:
    """Parse '1.e4 e5 2.Nc3 ...' into move objects."""
    tokens = seed.split()
    moves = []
    move_num = 1
    is_white = True

    for token in tokens:
        if '.' in token:
            # Could be "1.e4" or just "1."
            parts = token.split('.')
            move_num = int(parts[0]) if parts[0].isdigit() else move_num
            san = parts[1] if len(parts) > 1 and parts[1] else None
            if san:
                moves.append({
                    'moveNumber': move_num,
                    'san': san,
                    'purpose': '',
                    'isWhiteMove': True,
                })
                is_white = False
        else:
            moves.append({
                'moveNumber': move_num,
                'san': token,
                'purpose': '',
                'isWhiteMove': is_white,
            })
            if not is_white:
                move_num += 1
            is_white = not is_white

    return moves


def get_memory_hook(side: str) -> str:
    hooks = {
        'WHITE': 'Knight-Fork Freeway: Nc3 opens the f-file, f4 gambit, e5 knight-trap',
        'BLACK': 'Bishop Trip: Bf5-g6-h7-d3 exchange, solid Caro fortress',
    }
    return hooks.get(side, '')


def get_memory_hook_breakdown(side: str) -> list:
    breakdowns = {
        'WHITE': [
            '1.e4 e5 — Open game',
            '2.Nc3 — Vienna: develop knight before f4',
            '3.f4 exf4 — Gambit accepted, open f-file',
            '4.e5 — Push! Trap the knight',
            '5-10 — Clean up: Qe2, Bxf4, exchange to a winning endgame',
        ],
        'BLACK': [
            '1.e4 c6 — Caro-Kann: solid, reliable',
            '2.d4 d5 3.Nc3 dxe4 4.Nxe4 Bf5 — Classical: bishop out before e6',
            '5.Ng3 Bg6 6.h4 h6 — The bishop trip begins',
            '7.Nf3 Nd7 8.h5 Bh7 — Bishop retreats to safety',
            '9.Bd3 Bxd3 10.Qxd3 e6 — Exchange complete, solid position',
        ],
    }
    return breakdowns.get(side, [])


def extract_plans() -> list:
    return [
        {'id': 'w_plan_1', 'side': 'WHITE', 'title': 'Convert material advantage',
         'description': 'After the Vienna Gambit sequence, White often reaches an endgame with an extra pawn or decisive positional advantage. Convert by trading pieces and pushing passed pawns.', 'priority': 1},
        {'id': 'w_plan_2', 'side': 'WHITE', 'title': 'Control the center with d4',
         'description': 'After e5 pushes the knight away, establish d4 and control the central squares. The Bf4 supports this structure.', 'priority': 2},
        {'id': 'w_plan_3', 'side': 'WHITE', 'title': 'Develop rapidly after gambit',
         'description': 'After the pawn sacrifice, complete development quickly with Nf3, Bd3/Be2, and castle. Tempo is the compensation for the pawn.', 'priority': 3},
        {'id': 'b_plan_1', 'side': 'BLACK', 'title': 'Solid fortress structure',
         'description': 'The Caro-Kann pawn structure (c6-d5-e6) creates a solid fortress. Avoid weakening the kingside pawns.', 'priority': 1},
        {'id': 'b_plan_2', 'side': 'BLACK', 'title': 'Develop Ngf6 and Be7',
         'description': 'After the bishop exchange on d3, complete development with Ngf6 and Be7, followed by O-O. The position is solid and equal.', 'priority': 2},
        {'id': 'b_plan_3', 'side': 'BLACK', 'title': 'Counter in the center with c5',
         'description': 'Once fully developed, look for c6-c5 to challenge White\'s d4 pawn and create counterplay.', 'priority': 3},
    ]


def extract_themes() -> list:
    return [
        {'id': 'w_theme_1', 'side': 'WHITE', 'title': 'Knight trap with e5',
         'description': 'The key tactical motif: 4.e5 attacks the Nf6, forcing it to retreat to g8 and losing tempo.', 'occurrenceRate': 'Every game in this line'},
        {'id': 'w_theme_2', 'side': 'WHITE', 'title': 'Queen centralization',
         'description': 'Qe2 followed by Qxe5+ puts the queen on a dominant central square with check.', 'occurrenceRate': 'Main line'},
        {'id': 'b_theme_1', 'side': 'BLACK', 'title': 'Light-square bishop exchange',
         'description': 'The Bf5-g6-h7-d3 maneuver exchanges the potentially bad bishop for White\'s good bishop.', 'occurrenceRate': 'Every game in this line'},
        {'id': 'b_theme_2', 'side': 'BLACK', 'title': 'Pawn structure solidity',
         'description': 'The c6-e6 structure with no weaknesses gives Black a reliable, draw-rich position.', 'occurrenceRate': 'Main line'},
    ]


def extract_deviations() -> list:
    return [
        {'id': 'w_dev_1', 'side': 'WHITE', 'deviationName': '3...d6 instead of 3...exf4',
         'move': '3...d6', 'description': 'Black declines the gambit with a solid move.',
         'response': 'Play 4.Nf3 and transition to a King\'s Indian Attack setup. White keeps a pleasant game.'},
        {'id': 'w_dev_2', 'side': 'WHITE', 'deviationName': '2...Nc6 instead of 2...Nf6',
         'move': '2...Nc6', 'description': 'Black develops the other knight first.',
         'response': 'Continue with 3.f4 as planned. The Vienna Gambit works against both knight moves.'},
        {'id': 'b_dev_1', 'side': 'BLACK', 'deviationName': '4.Nf3 instead of 4.Nxe4',
         'move': '4.Nf3', 'description': 'White plays the Two Knights variation instead of the main classical.',
         'response': 'Continue with 4...Nf6 and develop naturally. The Caro-Kann structure remains solid.'},
        {'id': 'b_dev_2', 'side': 'BLACK', 'deviationName': '5.Nc5 instead of 5.Ng3',
         'move': '5.Nc5', 'description': 'White tries the aggressive knight jump to c5.',
         'response': 'Play 5...e6 6.Nxb7 Qb6, winning the knight back with a tempo on the queen.'},
    ]


def extract_model_games() -> list:
    return [
        {
            'id': 'game_m6_1', 'title': 'CL Black Specialist Win #1 (M6)',
            'side': 'BLACK', 'opening': 'Caro-Kann Classical', 'result': '0-1',
            'moveCount': 67, 'keyTheme': 'Endgame conversion from solid Caro-Kann structure',
            'annotations': [
                {'moveNumber': 10, 'comment': 'Exit position reached — Caro-Kann Classical fortress established', 'evaluation': '+0.3'},
                {'moveNumber': 20, 'comment': 'Black has equalized with the solid c6-e6 structure'},
                {'moveNumber': 40, 'comment': 'Black specialist creates imbalance in the endgame', 'evaluation': '-0.5'},
                {'moveNumber': 60, 'comment': 'Winning technique demonstrated — Black converts', 'evaluation': '-2.0'},
            ],
            'evaluationProgression': '+0.3 → 0.0 → -0.5 → -2.0',
        },
        {
            'id': 'game_m6_2', 'title': 'CL Black Specialist Win #2 (M6)',
            'side': 'BLACK', 'opening': 'Caro-Kann Classical', 'result': '0-1',
            'moveCount': 55, 'keyTheme': 'Central counter-play with c5 break',
            'annotations': [
                {'moveNumber': 10, 'comment': 'Standard Caro-Kann exit', 'evaluation': '+0.3'},
                {'moveNumber': 25, 'comment': 'c5 break creates central tension'},
                {'moveNumber': 35, 'comment': 'Black seizes initiative', 'evaluation': '-0.8'},
                {'moveNumber': 50, 'comment': 'Conversion phase', 'evaluation': '-3.0'},
            ],
            'evaluationProgression': '+0.3 → 0.0 → -0.8 → -3.0',
        },
        {
            'id': 'game_m1_1', 'title': 'Vienna Gambit Crushing Win (M1)',
            'side': 'WHITE', 'opening': 'Vienna Gambit Accepted', 'result': '1-0',
            'moveCount': 30, 'keyTheme': 'Rapid development and material advantage from gambit',
            'annotations': [
                {'moveNumber': 4, 'comment': '4.e5! — the key move, trapping the knight', 'evaluation': '+1.7'},
                {'moveNumber': 10, 'comment': 'All exchanges favor White — material advantage secured', 'evaluation': '+2.0'},
                {'moveNumber': 20, 'comment': 'White converts the advantage in the endgame', 'evaluation': '+4.0'},
            ],
            'evaluationProgression': '+1.7 → +2.0 → +4.0 → 1-0',
        },
    ]


def extract_drills() -> list:
    return [
        # Vienna Gambit drills
        {'id': 'd_w_1', 'type': 'FILL_IN_BLANK', 'title': 'Vienna Gambit Key Move',
         'question': 'In the Vienna Gambit after 1.e4 e5 2.Nc3 Nf6 3.f4 exf4, what is White\'s key fourth move?',
         'options': ['4.e5', '4.Nf3', '4.d4', '4.Bc4'], 'correctAnswer': '4.e5',
         'explanation': '4.e5 attacks the knight on f6, forcing it to retreat to g8. This is the key tactical idea of the Vienna Gambit — White gains tempo and opens the position.',
         'side': 'WHITE', 'fen': None},
        {'id': 'd_w_2', 'type': 'CHOOSE_MOVE', 'title': 'Queen Centralization',
         'question': 'After 7...Bb4 8.Qxe5+ Qe7, what should White play?',
         'options': ['9.Bxf4', '9.O-O-O', '9.Qxe7+', '9.Bg5'], 'correctAnswer': '9.Bxf4',
         'explanation': '9.Bxf4 develops the bishop with tempo, recapturing the gambit pawn and maintaining central control. The queen exchange can wait.',
         'side': 'WHITE', 'fen': 'rnb1k1nr/ppp1qppp/8/4Q3/1b1P1p2/2N2N2/PPP3PP/R1B1KB1R w KQkq - 0 9'},
        {'id': 'd_w_3', 'type': 'PLANS_QUIZ', 'title': 'Post-Opening Plan (White)',
         'question': 'After reaching the Vienna Gambit exit position, what is White\'s primary plan?',
         'options': ['Attack the kingside immediately', 'Convert the material/positional advantage', 'Sacrifice more pawns', 'Trade queens at all costs'],
         'correctAnswer': 'Convert the material/positional advantage',
         'explanation': 'The exit position is nearly +2.0 for White. The plan is to convert this advantage by completing development, trading pieces when favorable, and pushing passed pawns.',
         'side': 'WHITE', 'fen': 'rnb1k1nr/ppp1qppp/8/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq - 0 10'},
        # Caro-Kann drills
        {'id': 'd_b_1', 'type': 'FILL_IN_BLANK', 'title': 'Caro-Kann Bishop Development',
         'question': 'In the Caro-Kann Classical, after 3.Nc3 dxe4 4.Nxe4, where does Black develop the light-squared bishop?',
         'options': ['Bf5', 'Bg4', 'Be6', 'Bd7'], 'correctAnswer': 'Bf5',
         'explanation': '4...Bf5 is the Classical variation. The bishop goes outside the pawn chain before playing e6. This is the key strategic idea — don\'t lock the bishop in!',
         'side': 'BLACK', 'fen': None},
        {'id': 'd_b_2', 'type': 'CHOOSE_MOVE', 'title': 'Bishop Retreat',
         'question': 'After 7.Nf3 Nd7 8.h5, where does the bishop go?',
         'options': ['Bh7', 'Bf5', 'Be4', 'Bxd3'], 'correctAnswer': 'Bh7',
         'explanation': '8...Bh7 completes the bishop trip (Bf5-g6-h7). The bishop retreats to h7 where it is safe, waiting for the Bd3 exchange.',
         'side': 'BLACK', 'fen': 'r2qkbnr/pp1n1pp1/2p1p1bp/7P/3P4/5NN1/PPP2PP1/R1BQKB1R b KQkq - 0 8'},
        {'id': 'd_b_3', 'type': 'FEN_RECOGNITION', 'title': 'Recognize the Exit Position (Black)',
         'question': 'What is the evaluation of the Caro-Kann Classical exit position after 10...e6?',
         'options': ['+0.3 (slight White edge)', 'Equal (0.0)', '-0.5 (slight Black edge)', '+1.0 (clear White advantage)'],
         'correctAnswer': '+0.3 (slight White edge)',
         'explanation': 'The exit position is approximately +0.3, a slight edge for White that is well within drawing range. Black\'s solid structure makes this very playable.',
         'side': 'BLACK', 'fen': 'r2qkbnr/pp1n1pp1/2p1p2p/7P/3P4/3Q1NN1/PPP2PP1/R1B1K2R w KQkq - 0 11'},
        # Mixed / general drills
        {'id': 'd_g_1', 'type': 'FLASHCARD', 'title': 'Memory Hook — White',
         'question': 'What is the memory hook for the White Vienna Gambit line?',
         'options': None, 'correctAnswer': 'Knight-Fork Freeway: Nc3 opens the f-file, f4 gambit, e5 knight-trap',
         'explanation': 'The hook captures the three key ideas: 1) Nc3 before f4, 2) the f4 gambit opens the f-file, 3) e5 traps/displaces the enemy knight.',
         'side': 'WHITE', 'fen': None},
        {'id': 'd_g_2', 'type': 'FLASHCARD', 'title': 'Memory Hook — Black',
         'question': 'What is the memory hook for the Black Caro-Kann Classical line?',
         'options': None, 'correctAnswer': 'Bishop Trip: Bf5-g6-h7-d3 exchange, solid Caro fortress',
         'explanation': 'The hook captures the bishop maneuver (Bf5→g6→h7, then exchanging on d3) and the resulting solid pawn structure.',
         'side': 'BLACK', 'fen': None},
        {'id': 'd_g_3', 'type': 'PLANS_QUIZ', 'title': 'Counter-play Timing (Black)',
         'question': 'When should Black play c6-c5 in the Caro-Kann Classical?',
         'options': ['Immediately after the opening', 'Only after full development and castling', 'Never — keep the pawn on c6', 'Before developing the knight'],
         'correctAnswer': 'Only after full development and castling',
         'explanation': 'c5 is Black\'s main source of counterplay, but it should wait until development is complete (Ngf6, Be7, O-O). Playing it too early weakens the d5 square.',
         'side': 'BLACK', 'fen': None},
    ]


def extract_quick_starts() -> list:
    return [
        {
            'side': 'WHITE', 'lineName': 'Vienna Gambit Accepted (C29)',
            'seedLine': '1.e4 e5 2.Nc3 Nf6 3.f4 exf4 4.e5 Ng8 5.Nf3 d6 6.d4 dxe5 7.Qe2 Bb4 8.Qxe5+ Qe7 9.Bxf4 Bxc3+ 10.bxc3',
            'memoryHook': 'Knight-Fork Freeway: Nc3 opens the f-file, f4 gambit, e5 knight-trap',
            'memoryHookBreakdown': [
                '1.e4 e5 — Open game',
                '2.Nc3 — Vienna: develop knight before f4',
                '3.f4 exf4 — Gambit accepted, open f-file',
                '4.e5 — Push! Trap the knight',
                '5-10 — Clean up: Qe2, Bxf4, exchange to winning endgame',
            ],
            'threeKeyActions': [
                'Play e5 to trap the knight (move 4)',
                'Centralize queen with Qe2 then Qxe5+ (moves 7-8)',
                'Convert the +2.0 advantage in the endgame',
            ],
            'exitFen': 'rnb1k1nr/ppp1qppp/8/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq - 0 10',
            'exitEvaluation': '+1.7 to +2.0',
            'typicalResult': '95% screening score (virtually winning)',
        },
        {
            'side': 'BLACK', 'lineName': 'Caro-Kann Classical 4...Bf5 (B18)',
            'seedLine': '1.e4 c6 2.d4 d5 3.Nc3 dxe4 4.Nxe4 Bf5 5.Ng3 Bg6 6.h4 h6 7.Nf3 Nd7 8.h5 Bh7 9.Bd3 Bxd3 10.Qxd3 e6',
            'memoryHook': 'Bishop Trip: Bf5-g6-h7-d3 exchange, solid Caro fortress',
            'memoryHookBreakdown': [
                '1.e4 c6 — Caro-Kann: solid, reliable',
                '2.d4 d5 3.Nc3 dxe4 4.Nxe4 Bf5 — Classical: bishop out before e6',
                '5.Ng3 Bg6 6.h4 h6 — The bishop trip begins',
                '7.Nf3 Nd7 8.h5 Bh7 — Bishop retreats to safety',
                '9.Bd3 Bxd3 10.Qxd3 e6 — Exchange complete, solid position',
            ],
            'threeKeyActions': [
                'Develop bishop to f5 BEFORE playing e6 (move 4)',
                'Follow the bishop trip: Bf5 → g6 → h7 → exchange on d3',
                'Complete development with Ngf6, Be7, O-O then look for c5',
            ],
            'exitFen': 'r2qkbnr/pp1n1pp1/2p1p2p/7P/3P4/3Q1NN1/PPP2PP1/R1B1K2R w KQkq - 0 11',
            'exitEvaluation': '+0.3',
            'typicalResult': '53.75% as Black specialist (+26 Elo)',
        },
    ]


def main():
    os.makedirs(ASSET_DIR, exist_ok=True)

    assets = {
        'claims.json': extract_claims(),
        'proof.json': extract_proof(),
        'repertoire.json': extract_repertoire(),
        'plans.json': extract_plans(),
        'themes.json': extract_themes(),
        'deviations.json': extract_deviations(),
        'model_games.json': extract_model_games(),
        'drills.json': extract_drills(),
        'quick_starts.json': extract_quick_starts(),
    }

    for name, data in assets.items():
        path = os.path.join(ASSET_DIR, name)
        with open(path, 'w') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        print(f'  wrote {path}')

    print(f'\nDone — {len(assets)} asset files written to {ASSET_DIR}')


if __name__ == '__main__':
    main()
