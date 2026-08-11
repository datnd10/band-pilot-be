CREATE TABLE srs_record (
    word_id          UUID         NOT NULL,
    interval         INTEGER      NOT NULL DEFAULT 1,
    ease_factor      NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    repetitions      INTEGER      NOT NULL DEFAULT 0,
    next_review_date DATE         NOT NULL,

    CONSTRAINT pk_srs_record       PRIMARY KEY (word_id),
    CONSTRAINT fk_srs_record_word  FOREIGN KEY (word_id)
        REFERENCES word_entry (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_srs_interval    CHECK (interval >= 1),
    CONSTRAINT chk_srs_ease_factor CHECK (ease_factor >= 1.30),
    CONSTRAINT chk_srs_repetitions CHECK (repetitions >= 0)
);

CREATE INDEX idx_srs_record_next_review_date ON srs_record (next_review_date);
