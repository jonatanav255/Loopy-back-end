-- Add sort_order column to topics and concepts for drag-and-drop reordering

ALTER TABLE topics ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;
ALTER TABLE concepts ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;

-- Initialize sort_order based on current name/title ordering so existing data keeps its order
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY name ASC) AS rn
    FROM topics
)
UPDATE topics SET sort_order = ranked.rn FROM ranked WHERE topics.id = ranked.id;

WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY topic_id ORDER BY title ASC) AS rn
    FROM concepts
)
UPDATE concepts SET sort_order = ranked.rn FROM ranked WHERE concepts.id = ranked.id;
