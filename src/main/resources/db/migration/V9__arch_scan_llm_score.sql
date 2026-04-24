ALTER TABLE architecture_scans
    ADD COLUMN llm_score    INTEGER,
    ADD COLUMN llm_reasoning TEXT,
    ADD COLUMN llm_model_id  VARCHAR(100);