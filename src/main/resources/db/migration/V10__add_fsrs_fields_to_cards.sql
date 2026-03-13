-- Add FSRS scheduling fields and algorithm selector to cards.
-- stability: how many days until recall probability drops to 90% (FSRS core concept)
-- difficulty: card difficulty 1-10 (FSRS core concept)
-- scheduling_algorithm: SM2 or FSRS, defaults to SM2 for existing cards
ALTER TABLE cards ADD COLUMN stability DOUBLE PRECISION DEFAULT 0;
ALTER TABLE cards ADD COLUMN difficulty DOUBLE PRECISION DEFAULT 0;
ALTER TABLE cards ADD COLUMN scheduling_algorithm VARCHAR(10) NOT NULL DEFAULT 'SM2';
