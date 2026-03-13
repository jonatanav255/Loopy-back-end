-- Add confidence rating (1 = guessing, 2 = unsure, 3 = confident) to review logs.
-- Nullable for backward compatibility with existing reviews.
ALTER TABLE review_logs ADD COLUMN confidence INTEGER CHECK (confidence >= 1 AND confidence <= 3);
