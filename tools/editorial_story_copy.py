"""Assemble independently edited bilingual narrative batches.

The split keeps each editorial pass small enough for careful review while this
module gives the deterministic catalogue generator one complete source.
"""

from editorial_daily_copy import (
    CATEGORY_STORIES as DAILY_CATEGORY_STORIES,
    LEVEL_STORIES as DAILY_LEVEL_STORIES,
)
from editorial_life_copy import (
    CATEGORY_STORIES as LIFE_CATEGORY_STORIES,
    LEVEL_STORIES as LIFE_LEVEL_STORIES,
)
from editorial_nature_copy import (
    CATEGORY_STORIES as NATURE_CATEGORY_STORIES,
    LEVEL_STORIES as NATURE_LEVEL_STORIES,
)


def merge_batches(*batches):
    merged = {}
    for batch in batches:
        overlap = set(merged) & set(batch)
        if overlap:
            raise ValueError(f"duplicate editorial categories: {sorted(overlap)}")
        merged.update(batch)
    return merged


CATEGORY_STORIES = merge_batches(
    NATURE_CATEGORY_STORIES,
    LIFE_CATEGORY_STORIES,
    DAILY_CATEGORY_STORIES,
)
LEVEL_STORIES = merge_batches(
    NATURE_LEVEL_STORIES,
    LIFE_LEVEL_STORIES,
    DAILY_LEVEL_STORIES,
)
