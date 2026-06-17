ALTER TABLE dialogmelding
    ADD COLUMN conversation_ref UUID;

UPDATE dialogmelding
SET conversation_ref = gen_random_uuid()
WHERE conversation_ref IS NULL;

ALTER TABLE dialogmelding
    ALTER COLUMN conversation_ref SET NOT NULL;
