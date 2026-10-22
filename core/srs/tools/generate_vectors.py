#!/usr/bin/env python3
"""Generate FSRS reference vectors for the Kotlin port (core/srs).

Runs deterministic review sequences through the reference implementation
py-fsrs 6.3.2 (MIT, open-spaced-repetition) with fuzzing disabled and writes
src/test/resources/fsrs_vectors.json, which FsrsVectorsTest replays.

Usage:
    python3 -m venv venv && venv/bin/pip install fsrs==6.3.2
    venv/bin/python core/srs/tools/generate_vectors.py
"""

import json
import random
from datetime import datetime, timedelta, timezone
from pathlib import Path

from importlib.metadata import version

from fsrs import Card, Rating, Scheduler, State

assert version("fsrs") == "6.3.2", f"need fsrs==6.3.2, found {version('fsrs')}"

EPOCH = datetime(1970, 1, 1, tzinfo=timezone.utc)
START = datetime(2024, 1, 1, 8, 0, 0, tzinfo=timezone.utc)
OUT = Path(__file__).resolve().parent.parent / "src/test/resources/fsrs_vectors.json"

CUSTOM_PARAMS = (
    0.4, 0.9, 3.1, 15.7, 7.1, 0.55, 1.5, 0.006, 1.5, 0.12, 1.0,
    1.9, 0.11, 0.3, 2.3, 0.23, 2.9, 0.4, 0.6, 0.15, 0.35,
)

M = timedelta(minutes=1)
CONFIGS = {
    "default": dict(),
    "three_steps": dict(learning_steps=(1 * M, 10 * M, 60 * M), relearning_steps=(10 * M, 60 * M),
                        desired_retention=0.85),
    "no_steps": dict(learning_steps=(), relearning_steps=()),
    "single_step": dict(learning_steps=(10 * M,), relearning_steps=(timedelta(seconds=45),),
                        desired_retention=0.95, maximum_interval=100),
    "custom_params": dict(parameters=CUSTOM_PARAMS, desired_retention=0.8,
                          learning_steps=(timedelta(seconds=30), 7 * M)),
    "low_max": dict(maximum_interval=30, desired_retention=0.7),
}

# Gaps before the next review: fixed durations, or relative to the card's due date.
FIXED_GAPS = [
    timedelta(0), timedelta(seconds=30), M, 5 * M, 10 * M, timedelta(hours=1), timedelta(hours=6),
    timedelta(hours=23, minutes=59), timedelta(days=1), timedelta(days=1, hours=1), timedelta(days=2),
    timedelta(days=5), timedelta(days=12), timedelta(days=30), timedelta(days=75), timedelta(days=200),
    timedelta(days=800), timedelta(milliseconds=86_399_999), timedelta(days=3, milliseconds=123),
]
DUE_OFFSETS = [timedelta(0), timedelta(hours=-2), timedelta(hours=3), timedelta(days=-1), timedelta(days=4)]


def ms(dt):
    return None if dt is None else (dt - EPOCH) // timedelta(milliseconds=1)


def card_json(card):
    return {"state": int(card.state), "step": card.step, "stability": card.stability,
            "difficulty": card.difficulty, "due": ms(card.due), "lastReview": ms(card.last_review)}


def scheduler_json(cfg):
    s = Scheduler(**cfg, enable_fuzzing=False)
    return {"parameters": list(s.parameters), "desiredRetention": s.desired_retention,
            "learningStepsMs": [lst // timedelta(milliseconds=1) for lst in s.learning_steps],
            "relearningStepsMs": [lst // timedelta(milliseconds=1) for lst in s.relearning_steps],
            "maximumInterval": s.maximum_interval}


def start_cards():
    """Unusual starting cards (edge cases in the reference's branch structure)."""
    t = START - timedelta(days=3)
    return [
        Card(card_id=1, due=START),
        Card(card_id=1, state=State.Learning, step=5, stability=2.0, difficulty=6.0, due=START, last_review=t),
        Card(card_id=1, state=State.Relearning, step=3, stability=0.7, difficulty=8.5, due=START, last_review=t),
        Card(card_id=1, state=State.Review, step=None, stability=40.0, difficulty=3.0, due=START, last_review=None),
        Card(card_id=1, state=State.Learning, step=0, stability=1.5, difficulty=5.0, due=START, last_review=None),
        Card(card_id=1, state=State.Review, step=None, stability=0.01, difficulty=9.9, due=START,
             last_review=START - timedelta(days=400)),
    ]


def run_case(scheduler, card, ratings, gaps):
    now = START
    reviews = []
    for rating, gap in zip(ratings, gaps):
        kind, value = gap
        now = now + value if kind == "fixed" else max(now, card.due + value)
        card, _ = scheduler.review_card(card, rating, now)
        probes = sorted({now, now + timedelta(hours=12), now + timedelta(days=1), now + timedelta(days=7),
                         card.due, card.due + timedelta(days=30)})
        reviews.append({
            "rating": int(rating), "at": ms(now), "card": card_json(card),
            "retrievability": [[ms(p), scheduler.get_card_retrievability(card, p)] for p in probes],
        })
    return reviews


def random_gap(rng):
    if rng.random() < 0.45:
        return "due", rng.choice(DUE_OFFSETS)
    return "fixed", rng.choice(FIXED_GAPS)


def main():
    rng = random.Random(20261022)
    cases = []
    for name, cfg in CONFIGS.items():
        scheduler = Scheduler(**cfg, enable_fuzzing=False)
        sequences = [[r] * 6 for r in Rating]  # all-Again, all-Hard, all-Good, all-Easy
        sequences += [[Rating.Good, Rating.Good, Rating.Again, Rating.Good, Rating.Hard, Rating.Easy, Rating.Again,
                       Rating.Again, Rating.Good, Rating.Good]]
        while len(sequences) < 36:
            n = rng.randint(1, 24)
            weights = rng.choice([(1, 1, 1, 1), (1, 2, 6, 1), (4, 2, 3, 1)])
            sequences.append(rng.choices(list(Rating), weights=weights, k=n))
        for i, ratings in enumerate(sequences):
            special = start_cards()[1:]  # the last few sequences start from edge-case cards
            offset = len(sequences) - len(special)
            start = special[i - offset] if i >= offset else start_cards()[0]
            gaps = [random_gap(rng) for _ in ratings]
            if i < 4:  # same-day and long-gap variants for the uniform sequences
                gaps = [("fixed", FIXED_GAPS[(i * 5 + k) % len(FIXED_GAPS)]) for k in range(len(ratings))]
            cases.append({
                "name": f"{name}#{i}", "config": name, "start": card_json(start),
                "reviews": run_case(scheduler, start, ratings, gaps),
            })
        # Every edge-case start card x every first rating, so all branch exits are covered.
        for j, start in enumerate(start_cards()[1:]):
            for first in Rating:
                ratings = [first, Rating.Good, Rating.Again, Rating.Hard, Rating.Good]
                gaps = [("fixed", timedelta(0)), ("due", timedelta(0)), ("fixed", timedelta(days=2)),
                        ("fixed", 5 * M), ("due", timedelta(hours=1))]
                cases.append({
                    "name": f"{name}#edge{j}-{first.name}", "config": name, "start": card_json(start),
                    "reviews": run_case(scheduler, start, ratings, gaps),
                })
    payload = {
        "generator": "py-fsrs 6.3.2, enable_fuzzing=False",
        "configs": {name: scheduler_json(cfg) for name, cfg in CONFIGS.items()},
        "cases": cases,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with OUT.open("w") as f:
        f.write("{\n")
        f.write(f'"generator": {json.dumps(payload["generator"])},\n')
        f.write(f'"configs": {json.dumps(payload["configs"])},\n')
        f.write('"cases": [\n')
        f.write(",\n".join(json.dumps(c, separators=(",", ":")) for c in cases))
        f.write("\n]\n}\n")
    print(f"wrote {len(cases)} cases, {sum(len(c['reviews']) for c in cases)} reviews to {OUT}")


if __name__ == "__main__":
    main()
