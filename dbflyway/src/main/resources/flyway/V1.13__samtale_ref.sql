ALTER TABLE dialogmelding
    ADD COLUMN samtale_ref UUID;

UPDATE dialogmelding
SET samtale_ref = dialogmelding_uuid
WHERE samtale_ref IS NULL;

ALTER TABLE dialogmelding
    ALTER COLUMN samtale_ref SET NOT NULL;
