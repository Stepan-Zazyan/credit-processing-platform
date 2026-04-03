ALTER TABLE outbox_event
    ALTER COLUMN payload TYPE TEXT
    USING payload::text;
