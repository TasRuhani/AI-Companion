DO $migration$
BEGIN
    -- Flyway runs before Hibernate. On a fresh database, the current JPA entity
    -- creates companion_identity after Flyway and no relationships column exists.
    IF to_regclass('public.companion_identity') IS NOT NULL
       AND EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'companion_identity'
              AND column_name = 'relationships'
       ) THEN
        ALTER TABLE public.companion_identity
            DROP COLUMN relationships;
    END IF;
END;
$migration$;
