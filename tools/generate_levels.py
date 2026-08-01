#!/usr/bin/env python3
"""
Generate + validate the Wordscapes-style level dataset.

For each level we are given a set of wheel letters and the target words.
This script lays the words out as a real interlocking crossword and asserts:

  1. Every target word is spellable from the wheel letters (multiset containment).
  2. The wheel letters are exactly the letters of the longest word (Wordscapes rule).
  3. Every word is placed, either crossing an existing word or (for word #1) seeded.
  4. Wherever two words cross, both agree on the letter at that cell.
  5. No two parallel words are laid adjacent in a way that creates unintended
     side-by-side letter runs (which would read as bogus words).
  6. Bonus words are also spellable from the wheel and are not target words.

Placement uses backtracking search over (word, existing-cell, orientation).
Output: assets/levels.json
"""
import json
from collections import Counter
from itertools import permutations

# ---------------------------------------------------------------- level specs
# wheel is derived from the longest word; words must all be sub-anagrams of it.
LEVELS = [
    # (letters, [target words], [bonus words])
    # --- 3 letters: gentle onboarding ---
    ("CAT",   ["CAT", "ACT", "AT"],                                  ["TA"]),
    ("DOG",   ["DOG", "GOD", "GO", "OD"],                            ["DO"]),
    ("EAT",   ["EAT", "ATE", "TEA", "AT"],                           ["ETA", "TAE"]),
    # --- 4 letters ---
    ("STAR",  ["STAR", "RATS", "ARTS", "TAR", "RAT", "ART", "AS"],   ["SAT", "TSAR", "TAS"]),
    ("LAMP",  ["LAMP", "PALM", "MAP", "LAP", "PAL", "AMP"],          ["ALP", "LAM", "APM"]),
    ("BEAR",  ["BEAR", "BARE", "EAR", "ERA", "ARE", "BAR"],          ["BRA", "REB", "ABE"]),
    ("MOON",  ["MOON", "MONO", "MOO", "ON", "NO", "OM"],             ["MON", "NOM"]),
    # --- 5 letters ---
    ("PLANE", ["PLANE", "PANEL", "PLAN", "LANE", "PALE", "LEAP", "PEN", "NAP"],
              ["PEAL", "PENAL", "PLEA", "LEAN", "PANE", "ALE", "APE", "NAE"]),
    ("STONE", ["STONE", "NOTES", "TONES", "ONSET", "NOTE", "TONE", "NEST", "TENS", "SET"],
              ["SENT", "TOES", "NOSE", "ONES", "SON", "TON", "NET", "ONE", "TOE", "EON"]),
    ("BRAIN", ["BRAIN", "BRAN", "RAIN", "BARN", "RIB", "AIR", "BAR", "RAN"],
              ["NAB", "BIN", "BAN", "AIN", "NIB", "RAN", "BRA"]),
    ("CHAIR", ["CHAIR", "CHAI", "HAIR", "RICH", "CHAR", "ARCH", "AIR", "CAR"],
              ["RAH", "CHI", "ICH", "AHI", "RIA"]),
    # --- 6 letters: the meaty ones ---
    ("GARDEN", ["GARDEN", "DANGER", "GANDER", "RANGED", "GRADE", "GRAND", "ANGER", "RANGE", "READ", "DEAR", "RAGE", "GEAR"],
               ["DARN", "RAND", "DANG", "GRAN", "NEAR", "EARN", "DARE", "AGED", "DRAG", "REND", "DEAN"]),
    ("MASTER", ["MASTER", "STREAM", "TAMERS", "MATES", "STEAM", "TEAMS", "MEATS", "SMART", "TERMS", "TAMER", "RATES", "STARE"],
               ["MARES", "REAMS", "TEARS", "MATE", "TEAM", "MEAT", "STAR", "RATE", "TEAR", "ARMS", "RAMS", "MAST"]),
    ("PLANET", ["PLANET", "PLATEN", "PLANT", "PLATE", "PETAL", "LEAPT", "PLEAT", "PANEL", "PLANE", "LATEN", "PLAN", "LEAN"],
               ["PEAL", "PLEA", "LANE", "PALE", "LEAP", "PANT", "NEAT", "ANTE", "TAPE", "PEAT", "LATE", "TALE", "PENAL"]),
    ("CASTLE", ["CASTLE", "CLEATS", "SLATE", "STEAL", "TALES", "LEAST", "CLEAT", "SCALE", "LACES", "CASTE", "STALE", "TEALS"],
               ["CASE", "LACE", "SEAL", "SALE", "EAST", "SEAT", "LAST", "CAST", "CATS", "ACTS", "TALC", "ALES"]),
    ("ORANGE", ["ORANGE", "ONAGER", "GROAN", "ORGAN", "ARGON", "RANGE", "ANGER", "GONER", "AGONE", "GONE", "EARN", "NEAR"],
               ["ROAN", "GEAR", "RAGE", "OGRE", "GORE", "RANG", "NAG", "AGE", "EGO", "OAR", "ERA", "ONE"]),
]


def spellable(word, letters):
    """word can be built from the multiset `letters` (each tile used once)."""
    wc, lc = Counter(word), Counter(letters)
    return all(lc[ch] >= n for ch, n in wc.items())


class Grid:
    def __init__(self):
        self.cells = {}      # (r,c) -> letter
        self.placed = []     # (word, r, c, dir)

    def can_place(self, word, r, c, d):
        dr, dc = (0, 1) if d == "H" else (1, 0)
        # cell immediately before and after the word must be empty
        br, bc = r - dr, c - dc
        ar, ac = r + dr * len(word), c + dc * len(word)
        if (br, bc) in self.cells or (ar, ac) in self.cells:
            return False, 0
        crossings = 0
        for i, ch in enumerate(word):
            rr, cc = r + dr * i, c + dc * i
            cur = self.cells.get((rr, cc))
            if cur is not None:
                if cur != ch:
                    return False, 0
                crossings += 1
            else:
                # a fresh cell must not touch other words on its perpendicular sides,
                # otherwise we create an accidental adjacent letter pair.
                pdr, pdc = (1, 0) if d == "H" else (0, 1)
                if (rr + pdr, cc + pdc) in self.cells or (rr - pdr, cc - pdc) in self.cells:
                    return False, 0
        return True, crossings

    def place(self, word, r, c, d):
        dr, dc = (0, 1) if d == "H" else (1, 0)
        added = []
        for i, ch in enumerate(word):
            k = (r + dr * i, c + dc * i)
            if k not in self.cells:
                self.cells[k] = ch
                added.append(k)
        self.placed.append((word, r, c, d))
        return added

    def unplace(self, added):
        for k in added:
            del self.cells[k]
        self.placed.pop()


def layout(words, max_dim):
    """
    Backtracking crossword layout. Longest word first; every later word must cross
    an already-placed one. Candidate placements are scored so the grid stays compact:
    we prefer more crossings, then the smallest resulting bounding box. `max_dim`
    hard-caps both dimensions so the grid fits a phone screen.
    """
    order = sorted(set(words), key=lambda w: (-len(w), w))
    g = Grid()
    g.place(order[0], 0, 0, "H")

    def bbox_with(word, r, c, d):
        dr, dc = (0, 1) if d == "H" else (1, 0)
        er, ec = r + dr * (len(word) - 1), c + dc * (len(word) - 1)
        rs = [rr for rr, _ in g.cells] + [r, er]
        cs = [cc for _, cc in g.cells] + [c, ec]
        return max(rs) - min(rs) + 1, max(cs) - min(cs) + 1

    def rec(idx):
        if idx == len(order):
            return True
        word = order[idx]
        cands = []
        for i, ch in enumerate(word):
            for (rr, cc), gch in list(g.cells.items()):
                if gch != ch:
                    continue
                for d in ("H", "V"):
                    dr, dc = (0, 1) if d == "H" else (1, 0)
                    r, c = rr - dr * i, cc - dc * i
                    ok, cross = g.can_place(word, r, c, d)
                    if not (ok and cross >= 1):
                        continue
                    h, w = bbox_with(word, r, c, d)
                    if h > max_dim or w > max_dim:
                        continue
                    # compact first: minimise area and squareness, maximise crossings
                    cands.append((-cross, h * w, abs(h - w), r, c, d))
        cands.sort()
        for _, _, _, r, c, d in cands:
            added = g.place(word, r, c, d)
            if rec(idx + 1):
                return True
            g.unplace(added)
        return False

    if not rec(1):
        return None
    return g


def normalise(g):
    """Shift placements so the grid starts at (0,0); return rows, cols, placements."""
    minr = min(r for r, _ in g.cells)
    minc = min(c for _, c in g.cells)
    maxr = max(r for r, _ in g.cells)
    maxc = max(c for _, c in g.cells)
    out = []
    for (word, r, c, d) in g.placed:
        out.append({"word": word, "row": r - minr, "col": c - minc,
                    "direction": "HORIZONTAL" if d == "H" else "VERTICAL"})
    return maxr - minr + 1, maxc - minc + 1, out


def verify(level, rows, cols, placements):
    """Re-derive the grid from the emitted JSON and assert every invariant."""
    letters = level["letters"]
    board = {}
    for p in placements:
        dr, dc = (0, 1) if p["direction"] == "HORIZONTAL" else (1, 0)
        for i, ch in enumerate(p["word"]):
            k = (p["row"] + dr * i, p["col"] + dc * i)
            assert board.get(k, ch) == ch, f"crossing conflict at {k} in level {level['id']}"
            board[k] = ch
            assert 0 <= k[0] < rows and 0 <= k[1] < cols, f"out of bounds {k}"
    placed_words = {p["word"] for p in placements}
    assert placed_words == set(level["words"]), (
        f"level {level['id']}: placed {placed_words} != targets {set(level['words'])}")
    for w in level["words"]:
        assert spellable(w, letters), f"level {level['id']}: '{w}' not spellable from {letters}"
    for w in level["bonusWords"]:
        assert spellable(w, letters), f"level {level['id']}: bonus '{w}' not spellable from {letters}"
        assert w not in placed_words, f"level {level['id']}: bonus '{w}' duplicates a target"
    longest = max(level["words"], key=len)
    assert sorted(longest) == sorted(letters), (
        f"level {level['id']}: wheel {letters} != longest word {longest}")
    return board


def main():
    out = []
    for idx, (letters, words, bonus) in enumerate(LEVELS, start=1):
        words = list(dict.fromkeys(words))     # de-dup, keep order
        bonus = [b for b in dict.fromkeys(bonus) if b not in words]
        # Grow the cap until a layout is found, so each level gets the tightest
        # grid it can actually fit into rather than whatever the search stumbles on.
        g = None
        for cap in range(len(max(words, key=len)), 12):
            g = layout(words, cap)
            if g is not None:
                break
        if g is None:
            raise SystemExit(f"level {idx} ({letters}): could not lay out {words}")
        rows, cols, placements = normalise(g)
        level = {
            "id": idx,
            "letters": letters,
            "words": words,
            "bonusWords": bonus,
            "rows": rows,
            "cols": cols,
            "placements": placements,
        }
        board = verify(level, rows, cols, placements)
        out.append(level)
        # visual sanity dump
        print(f"--- Level {idx}  wheel={letters}  {rows}x{cols}  words={len(words)} bonus={len(bonus)}")
        for r in range(rows):
            print("   " + " ".join(board.get((r, c), ".") for c in range(cols)))

    path = "/Users/ritik/WordPuzzleGame/app/src/main/assets/levels.json"
    with open(path, "w") as f:
        json.dump({"levels": out}, f, indent=2)
    print(f"\nWrote {len(out)} levels to {path}")


if __name__ == "__main__":
    main()
