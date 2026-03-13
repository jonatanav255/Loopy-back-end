CREATE TABLE teach_backs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concept_id UUID NOT NULL REFERENCES concepts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_explanation TEXT NOT NULL,
    self_rating INTEGER NOT NULL CHECK (self_rating >= 1 AND self_rating <= 5),
    gaps_found TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_teach_backs_concept_id ON teach_backs(concept_id);
CREATE INDEX idx_teach_backs_user_id ON teach_backs(user_id);
