/*MIGRATION_DESCRIPTION
--CREATE: storage-Image
New object Image will be created in schema storage
--CREATE: storage-Image-ID
New property ID will be created for Image in storage
--CREATE: storage-Image-body
New property body will be created for Image in storage
--CREATE: storage-Image-width
New property width will be created for Image in storage
--CREATE: storage-Image-height
New property height will be created for Image in storage
MIGRATION_DESCRIPTION*/

DO $$ BEGIN
    IF EXISTS(SELECT * FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = '-NGS-' AND c.relname = 'database_setting') THEN
        IF EXISTS(SELECT * FROM "-NGS-".Database_Setting WHERE Key ILIKE 'mode' AND NOT Value ILIKE 'unsafe') THEN
            RAISE EXCEPTION 'Database upgrade is forbidden. Change database mode to allow upgrade';
        END IF;
    END IF;
END $$ LANGUAGE plpgsql;

DO $$
DECLARE script VARCHAR;
BEGIN
    IF NOT EXISTS(SELECT * FROM pg_namespace WHERE nspname = '-NGS-') THEN
        CREATE SCHEMA "-NGS-";
        COMMENT ON SCHEMA "-NGS-" IS 'NGS generated';
    END IF;
    IF NOT EXISTS(SELECT * FROM pg_namespace WHERE nspname = 'public') THEN
        CREATE SCHEMA public;
        COMMENT ON SCHEMA public IS 'NGS generated';
    END IF;
    SELECT array_to_string(array_agg('DROP VIEW IF EXISTS ' || quote_ident(n.nspname) || '.' || quote_ident(cl.relname) || ' CASCADE;'), '')
    INTO script
    FROM pg_class cl
    INNER JOIN pg_namespace n ON cl.relnamespace = n.oid
    INNER JOIN pg_description d ON d.objoid = cl.oid
    WHERE cl.relkind = 'v' AND d.description LIKE 'NGS volatile%';
    IF length(script) > 0 THEN
        EXECUTE script;
    END IF;
END $$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS "-NGS-".Database_Migration
(
    Ordinal SERIAL PRIMARY KEY,
    Dsls TEXT,
    Implementations BYTEA,
    Version VARCHAR,
    Applied_At TIMESTAMPTZ DEFAULT (CURRENT_TIMESTAMP)
);

CREATE OR REPLACE FUNCTION "-NGS-".Load_Last_Migration()
RETURNS "-NGS-".Database_Migration AS
$$
SELECT m FROM "-NGS-".Database_Migration m
ORDER BY Ordinal DESC
LIMIT 1
$$ LANGUAGE sql SECURITY DEFINER STABLE;

CREATE OR REPLACE FUNCTION "-NGS-".Persist_Concepts(dsls TEXT, implementations BYTEA, version VARCHAR)
  RETURNS void AS
$$
BEGIN
    INSERT INTO "-NGS-".Database_Migration(Dsls, Implementations, Version) VALUES(dsls, implementations, version);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION "-NGS-".Split_Uri(s text) RETURNS TEXT[] AS
$$
DECLARE i int;
DECLARE pos int;
DECLARE len int;
DECLARE res TEXT[];
DECLARE cur TEXT;
DECLARE c CHAR(1);
BEGIN
    pos = 0;
    i = 1;
    cur = '';
    len = length(s);
    LOOP
        pos = pos + 1;
        EXIT WHEN pos > len;
        c = substr(s, pos, 1);
        IF c = '/' THEN
            res[i] = cur;
            i = i + 1;
            cur = '';
        ELSE
            IF c = '\' THEN
                pos = pos + 1;
                c = substr(s, pos, 1);
            END IF;
            cur = cur || c;
        END IF;
    END LOOP;
    res[i] = cur;
    return res;
END
$$ LANGUAGE plpgsql SECURITY DEFINER IMMUTABLE;

CREATE OR REPLACE FUNCTION "-NGS-".Load_Type_Info(
    OUT type_schema character varying,
    OUT type_name character varying,
    OUT column_name character varying,
    OUT column_schema character varying,
    OUT column_type character varying,
    OUT column_index smallint,
    OUT is_not_null boolean,
    OUT is_ngs_generated boolean)
  RETURNS SETOF record AS
$BODY$
SELECT
    ns.nspname::varchar,
    cl.relname::varchar,
    atr.attname::varchar,
    ns_ref.nspname::varchar,
    typ.typname::varchar,
    (SELECT COUNT(*) + 1
    FROM pg_attribute atr_ord
    WHERE
        atr.attrelid = atr_ord.attrelid
        AND atr_ord.attisdropped = false
        AND atr_ord.attnum > 0
        AND atr_ord.attnum < atr.attnum)::smallint,
    atr.attnotnull,
    coalesce(d.description LIKE 'NGS generated%', false)
FROM
    pg_attribute atr
    INNER JOIN pg_class cl ON atr.attrelid = cl.oid
    INNER JOIN pg_namespace ns ON cl.relnamespace = ns.oid
    INNER JOIN pg_type typ ON atr.atttypid = typ.oid
    INNER JOIN pg_namespace ns_ref ON typ.typnamespace = ns_ref.oid
    LEFT JOIN pg_description d ON d.objoid = cl.oid
                                AND d.objsubid = atr.attnum
WHERE
    (cl.relkind = 'r' OR cl.relkind = 'v' OR cl.relkind = 'c')
    AND ns.nspname NOT LIKE 'pg_%'
    AND ns.nspname != 'information_schema'
    AND atr.attnum > 0
    AND atr.attisdropped = FALSE
ORDER BY 1, 2, 6
$BODY$
  LANGUAGE SQL STABLE;

CREATE OR REPLACE FUNCTION "-NGS-".Safe_Notify(target varchar, name varchar, operation varchar, uris varchar[]) RETURNS VOID AS
$$
DECLARE message VARCHAR;
DECLARE array_size INT;
BEGIN
    array_size = array_upper(uris, 1);
    message = name || ':' || operation || ':' || uris::TEXT;
    IF (array_size > 0 and length(message) < 8000) THEN
        PERFORM pg_notify(target, message);
    ELSEIF (array_size > 1) THEN
        PERFORM "-NGS-".Safe_Notify(target, name, operation, (SELECT array_agg(u) FROM (SELECT unnest(uris) u LIMIT (array_size+1)/2) u));
        PERFORM "-NGS-".Safe_Notify(target, name, operation, (SELECT array_agg(u) FROM (SELECT unnest(uris) u OFFSET (array_size+1)/2) u));
    ELSEIF (array_size = 1) THEN
        RAISE EXCEPTION 'uri can''t be longer than 8000 characters';
    END IF;
END
$$ LANGUAGE PLPGSQL SECURITY DEFINER;

CREATE OR REPLACE FUNCTION "-NGS-".cast_int(int[]) RETURNS TEXT AS
$$ SELECT $1::TEXT[]::TEXT $$ LANGUAGE SQL IMMUTABLE COST 1;
CREATE OR REPLACE FUNCTION "-NGS-".cast_bigint(bigint[]) RETURNS TEXT AS
$$ SELECT $1::TEXT[]::TEXT $$ LANGUAGE SQL IMMUTABLE COST 1;

DO $$ BEGIN
    -- unfortunately only superuser can create such casts
    IF EXISTS(SELECT * FROM pg_catalog.pg_user WHERE usename = CURRENT_USER AND usesuper) THEN
        IF NOT EXISTS (SELECT * FROM pg_catalog.pg_cast c JOIN pg_type s ON c.castsource = s.oid JOIN pg_type t ON c.casttarget = t.oid WHERE s.typname = '_int4' AND t.typname = 'text') THEN
            CREATE CAST (int[] AS text) WITH FUNCTION "-NGS-".cast_int(int[]) AS ASSIGNMENT;
        END IF;
        IF NOT EXISTS (SELECT * FROM pg_cast c JOIN pg_type s ON c.castsource = s.oid JOIN pg_type t ON c.casttarget = t.oid WHERE s.typname = '_int8' AND t.typname = 'text') THEN
            CREATE CAST (bigint[] AS text) WITH FUNCTION "-NGS-".cast_bigint(bigint[]) AS ASSIGNMENT;
        END IF;
    END IF;
END $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION "-NGS-".Generate_Uri2(text, text) RETURNS text AS
$$
BEGIN
    RETURN replace(replace($1, '\','\\'), '/', '\/')||'/'||replace(replace($2, '\','\\'), '/', '\/');
END;
$$ LANGUAGE PLPGSQL IMMUTABLE;

CREATE OR REPLACE FUNCTION "-NGS-".Generate_Uri3(text, text, text) RETURNS text AS
$$
BEGIN
    RETURN replace(replace($1, '\','\\'), '/', '\/')||'/'||replace(replace($2, '\','\\'), '/', '\/')||'/'||replace(replace($3, '\','\\'), '/', '\/');
END;
$$ LANGUAGE PLPGSQL IMMUTABLE;

CREATE OR REPLACE FUNCTION "-NGS-".Generate_Uri4(text, text, text, text) RETURNS text AS
$$
BEGIN
    RETURN replace(replace($1, '\','\\'), '/', '\/')||'/'||replace(replace($2, '\','\\'), '/', '\/')||'/'||replace(replace($3, '\','\\'), '/', '\/')||'/'||replace(replace($4, '\','\\'), '/', '\/');
END;
$$ LANGUAGE PLPGSQL IMMUTABLE;

CREATE OR REPLACE FUNCTION "-NGS-".Generate_Uri5(text, text, text, text, text) RETURNS text AS
$$
BEGIN
    RETURN replace(replace($1, '\','\\'), '/', '\/')||'/'||replace(replace($2, '\','\\'), '/', '\/')||'/'||replace(replace($3, '\','\\'), '/', '\/')||'/'||replace(replace($4, '\','\\'), '/', '\/')||'/'||replace(replace($5, '\','\\'), '/', '\/');
END;
$$ LANGUAGE PLPGSQL IMMUTABLE;

CREATE OR REPLACE FUNCTION "-NGS-".Generate_Uri(text[]) RETURNS text AS
$$
BEGIN
    RETURN (SELECT array_to_string(array_agg(replace(replace(u, '\','\\'), '/', '\/')), '/') FROM unnest($1) u);
END;
$$ LANGUAGE PLPGSQL IMMUTABLE;

CREATE TABLE IF NOT EXISTS "-NGS-".Database_Setting
(
    Key VARCHAR PRIMARY KEY,
    Value TEXT NOT NULL
);

CREATE OR REPLACE FUNCTION "-NGS-".Create_Type_Cast(function VARCHAR, schema VARCHAR, from_name VARCHAR, to_name VARCHAR)
RETURNS void
AS
$$
DECLARE header VARCHAR;
DECLARE source VARCHAR;
DECLARE footer VARCHAR;
DECLARE col_name VARCHAR;
DECLARE type VARCHAR = '"' || schema || '"."' || to_name || '"';
BEGIN
    header = 'CREATE OR REPLACE FUNCTION ' || function || '
RETURNS ' || type || '
AS
$BODY$
SELECT ROW(';
    footer = ')::' || type || '
$BODY$ IMMUTABLE LANGUAGE sql;';
    source = '';
    FOR col_name IN
        SELECT
            CASE WHEN
                EXISTS (SELECT * FROM "-NGS-".Load_Type_Info() f
                    WHERE f.type_schema = schema AND f.type_name = from_name AND f.column_name = t.column_name)
                OR EXISTS(SELECT * FROM pg_proc p JOIN pg_type t_in ON p.proargtypes[0] = t_in.oid
                    JOIN pg_namespace n_in ON t_in.typnamespace = n_in.oid JOIN pg_namespace n ON p.pronamespace = n.oid
                    WHERE array_upper(p.proargtypes, 1) = 0 AND n.nspname = 'public' AND t_in.typname = from_name AND p.proname = t.column_name) THEN t.column_name
                ELSE null
            END
        FROM "-NGS-".Load_Type_Info() t
        WHERE
            t.type_schema = schema
            AND t.type_name = to_name
        ORDER BY t.column_index
    LOOP
        IF col_name IS NULL THEN
            source = source || 'null, ';
        ELSE
            source = source || '$1."' || col_name || '", ';
        END IF;
    END LOOP;
    IF (LENGTH(source) > 0) THEN
        source = SUBSTRING(source, 1, LENGTH(source) - 2);
    END IF;
    EXECUTE (header || source || footer);
END
$$ LANGUAGE plpgsql;;

DO $$ BEGIN
    IF NOT EXISTS(SELECT * FROM pg_namespace WHERE nspname = 'storage') THEN
        CREATE SCHEMA "storage";
        COMMENT ON SCHEMA "storage" IS 'NGS generated';
    END IF;
END $$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS(SELECT * FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE n.nspname = 'storage' AND t.typname = '-ngs_Image_type-') THEN
        CREATE TYPE "storage"."-ngs_Image_type-" AS ();
        COMMENT ON TYPE "storage"."-ngs_Image_type-" IS 'NGS generated';
    END IF;
END $$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS(SELECT * FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = 'storage' AND c.relname = 'Image') THEN
        CREATE TABLE "storage"."Image" ();
        COMMENT ON TABLE "storage"."Image" IS 'NGS generated';
    END IF;
END $$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS(SELECT * FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = 'storage' AND c.relname = 'Image_sequence') THEN
        CREATE SEQUENCE "storage"."Image_sequence";
        COMMENT ON SEQUENCE "storage"."Image_sequence" IS 'NGS generated';
    END IF;
END $$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS(SELECT * FROM "-NGS-".Load_Type_Info() WHERE type_schema = 'storage' AND type_name = '-ngs_Image_type-' AND column_name = 'ID') THEN
        ALTER TYPE "storage"."-ngs_Image_type-" ADD ATTRIBUTE "ID" INT;
        COMMENT ON COLUMN "storage"."-ngs_Image_type-"."ID" IS 'NGS generated';
    END IF;
END $$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS(SELECT * FROM "-NGS-".Load_Type_Info() WHERE type_schema = 'storage' AND type_name = 'Image' AND column_name = 'ID') THEN
        ALTER TABLE "storage"."Image" ADD COLUMN "ID" INT;
        COMMENT ON COLUMN "storage"."Image"."ID" IS 'NGS generated';
    END IF;
END $$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS(SELECT * FROM "-NGS-".Load_Type_Info() WHERE type_schema = 'storage' AND type_name = '-ngs_Image_type-' AND column_name = 'body') THEN
        ALTER TYPE "storage"."-ngs_Image_type-" ADD ATTRIBUTE "body" BYTEA;
        COMMENT ON COLUMN "storage"."-ngs_Image_type-"."body" IS 'NGS generated';
    END IF;
END $$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS(SELECT * FROM "-NGS-".Load_Type_Info() WHERE type_schema = 'storage' AND type_name = 'Image' AND column_name = 'body') THEN
        ALTER TABLE "storage"."Image" ADD COLUMN "body" BYTEA;
        COMMENT ON COLUMN "storage"."Image"."body" IS 'NGS generated';
    END IF;
END $$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS(SELECT * FROM "-NGS-".Load_Type_Info() WHERE type_schema = 'storage' AND type_name = '-ngs_Image_type-' AND column_name = 'width') THEN
        ALTER TYPE "storage"."-ngs_Image_type-" ADD ATTRIBUTE "width" INT;
        COMMENT ON COLUMN "storage"."-ngs_Image_type-"."width" IS 'NGS generated';
    END IF;
END $$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS(SELECT * FROM "-NGS-".Load_Type_Info() WHERE type_schema = 'storage' AND type_name = 'Image' AND column_name = 'width') THEN
        ALTER TABLE "storage"."Image" ADD COLUMN "width" INT;
        COMMENT ON COLUMN "storage"."Image"."width" IS 'NGS generated';
    END IF;
END $$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS(SELECT * FROM "-NGS-".Load_Type_Info() WHERE type_schema = 'storage' AND type_name = '-ngs_Image_type-' AND column_name = 'height') THEN
        ALTER TYPE "storage"."-ngs_Image_type-" ADD ATTRIBUTE "height" INT;
        COMMENT ON COLUMN "storage"."-ngs_Image_type-"."height" IS 'NGS generated';
    END IF;
END $$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS(SELECT * FROM "-NGS-".Load_Type_Info() WHERE type_schema = 'storage' AND type_name = 'Image' AND column_name = 'height') THEN
        ALTER TABLE "storage"."Image" ADD COLUMN "height" INT;
        COMMENT ON COLUMN "storage"."Image"."height" IS 'NGS generated';
    END IF;
END $$ LANGUAGE plpgsql;

CREATE OR REPLACE VIEW "storage"."Image_entity" AS
SELECT _entity."ID", _entity."body", _entity."width", _entity."height"
FROM
    "storage"."Image" _entity
    ;
COMMENT ON VIEW "storage"."Image_entity" IS 'NGS volatile';

CREATE OR REPLACE FUNCTION "URI"("storage"."Image_entity") RETURNS TEXT AS $$
SELECT CAST($1."ID" as TEXT)
$$ LANGUAGE SQL IMMUTABLE SECURITY DEFINER;

CREATE OR REPLACE FUNCTION "storage"."cast_Image_to_type"("storage"."-ngs_Image_type-") RETURNS "storage"."Image_entity" AS $$ SELECT $1::text::"storage"."Image_entity" $$ IMMUTABLE LANGUAGE sql;
CREATE OR REPLACE FUNCTION "storage"."cast_Image_to_type"("storage"."Image_entity") RETURNS "storage"."-ngs_Image_type-" AS $$ SELECT $1::text::"storage"."-ngs_Image_type-" $$ IMMUTABLE LANGUAGE sql;

DO $$ BEGIN
    IF NOT EXISTS(SELECT * FROM pg_cast c JOIN pg_type s ON c.castsource = s.oid JOIN pg_type t ON c.casttarget = t.oid JOIN pg_namespace n ON n.oid = s.typnamespace AND n.oid = t.typnamespace
                    WHERE n.nspname = 'storage' AND s.typname = 'Image_entity' AND t.typname = '-ngs_Image_type-') THEN
        CREATE CAST ("storage"."-ngs_Image_type-" AS "storage"."Image_entity") WITH FUNCTION "storage"."cast_Image_to_type"("storage"."-ngs_Image_type-") AS IMPLICIT;
        CREATE CAST ("storage"."Image_entity" AS "storage"."-ngs_Image_type-") WITH FUNCTION "storage"."cast_Image_to_type"("storage"."Image_entity") AS IMPLICIT;
    END IF;
END $$ LANGUAGE plpgsql;

CREATE OR REPLACE VIEW "storage"."Image_unprocessed_events" AS
SELECT _aggregate."ID"
FROM
    "storage"."Image_entity" _aggregate
;
COMMENT ON VIEW "storage"."Image_unprocessed_events" IS 'NGS volatile';

CREATE OR REPLACE FUNCTION "storage"."insert_Image"(IN _inserted "storage"."Image_entity"[]) RETURNS VOID AS
$$
BEGIN
    INSERT INTO "storage"."Image" ("ID", "body", "width", "height") VALUES(_inserted[1]."ID", _inserted[1]."body", _inserted[1]."width", _inserted[1]."height");

    PERFORM pg_notify('aggregate_roots', 'storage.Image:Insert:' || array["URI"(_inserted[1])]::TEXT);
END
$$
LANGUAGE plpgsql SECURITY DEFINER;;

CREATE OR REPLACE FUNCTION "storage"."persist_Image"(
IN _inserted "storage"."Image_entity"[], IN _updated_original "storage"."Image_entity"[], IN _updated_new "storage"."Image_entity"[], IN _deleted "storage"."Image_entity"[])
    RETURNS VARCHAR AS
$$
DECLARE cnt int;
DECLARE uri VARCHAR;
DECLARE tmp record;
DECLARE _update_count int = array_upper(_updated_original, 1);
DECLARE _delete_count int = array_upper(_deleted, 1);

BEGIN

    SET CONSTRAINTS ALL DEFERRED;



    INSERT INTO "storage"."Image" ("ID", "body", "width", "height")
    SELECT _i."ID", _i."body", _i."width", _i."height"
    FROM unnest(_inserted) _i;



    UPDATE "storage"."Image" as _tbl SET "ID" = (_u.changed)."ID", "body" = (_u.changed)."body", "width" = (_u.changed)."width", "height" = (_u.changed)."height"
    FROM (SELECT unnest(_updated_original) as original, unnest(_updated_new) as changed) _u
    WHERE _tbl."ID" = (_u.original)."ID";

    GET DIAGNOSTICS cnt = ROW_COUNT;
    IF cnt != _update_count THEN
        RETURN 'Updated ' || cnt || ' row(s). Expected to update ' || _update_count || ' row(s).';
    END IF;



    DELETE FROM "storage"."Image"
    WHERE ("ID") IN (SELECT _d."ID" FROM unnest(_deleted) _d);

    GET DIAGNOSTICS cnt = ROW_COUNT;
    IF cnt != _delete_count THEN
        RETURN 'Deleted ' || cnt || ' row(s). Expected to delete ' || _delete_count || ' row(s).';
    END IF;


    PERFORM "-NGS-".Safe_Notify('aggregate_roots', 'storage.Image', 'Insert', (SELECT array_agg(_i."URI") FROM unnest(_inserted) _i));
    PERFORM "-NGS-".Safe_Notify('aggregate_roots', 'storage.Image', 'Update', (SELECT array_agg(_u."URI") FROM unnest(_updated_original) _u));
    PERFORM "-NGS-".Safe_Notify('aggregate_roots', 'storage.Image', 'Change', (SELECT array_agg((_u.changed)."URI") FROM (SELECT unnest(_updated_original) as original, unnest(_updated_new) as changed) _u WHERE (_u.changed)."ID" != (_u.original)."ID"));
    PERFORM "-NGS-".Safe_Notify('aggregate_roots', 'storage.Image', 'Delete', (SELECT array_agg(_d."URI") FROM unnest(_deleted) _d));

    SET CONSTRAINTS ALL IMMEDIATE;

    RETURN NULL;
END
$$
LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION "storage"."update_Image"(IN _original "storage"."Image_entity"[], IN _updated "storage"."Image_entity"[]) RETURNS VARCHAR AS
$$
DECLARE cnt int;
BEGIN

    UPDATE "storage"."Image" AS _tab SET "ID" = _updated[1]."ID", "body" = _updated[1]."body", "width" = _updated[1]."width", "height" = _updated[1]."height" WHERE _tab."ID" = _original[1]."ID";
    GET DIAGNOSTICS cnt = ROW_COUNT;

    PERFORM pg_notify('aggregate_roots', 'storage.Image:Update:' || array["URI"(_original[1])]::TEXT);
    IF (_original[1]."ID" != _updated[1]."ID") THEN
        PERFORM pg_notify('aggregate_roots', 'storage.Image:Change:' || array["URI"(_updated[1])]::TEXT);
    END IF;
    RETURN CASE WHEN cnt = 0 THEN 'No rows updated' ELSE NULL END;
END
$$
LANGUAGE plpgsql SECURITY DEFINER;;

SELECT "-NGS-".Create_Type_Cast('"storage"."cast_Image_to_type"("storage"."-ngs_Image_type-")', 'storage', '-ngs_Image_type-', 'Image_entity');
SELECT "-NGS-".Create_Type_Cast('"storage"."cast_Image_to_type"("storage"."Image_entity")', 'storage', 'Image_entity', '-ngs_Image_type-');
UPDATE "storage"."Image" SET "ID" = 0 WHERE "ID" IS NULL;
UPDATE "storage"."Image" SET "body" = '' WHERE "body" IS NULL;
UPDATE "storage"."Image" SET "width" = 0 WHERE "width" IS NULL;
UPDATE "storage"."Image" SET "height" = 0 WHERE "height" IS NULL;

DO $$
DECLARE _pk VARCHAR;
BEGIN
    IF EXISTS(SELECT * FROM pg_index i JOIN pg_class c ON i.indrelid = c.oid JOIN pg_namespace n ON c.relnamespace = n.oid WHERE i.indisprimary AND n.nspname = 'storage' AND c.relname = 'Image') THEN
        SELECT array_to_string(array_agg(sq.attname), ', ') INTO _pk
        FROM
        (
            SELECT atr.attname
            FROM pg_index i
            JOIN pg_class c ON i.indrelid = c.oid
            JOIN pg_attribute atr ON atr.attrelid = c.oid
            WHERE
                c.oid = '"storage"."Image"'::regclass
                AND atr.attnum = any(i.indkey)
                AND indisprimary
            ORDER BY (SELECT i FROM generate_subscripts(i.indkey,1) g(i) WHERE i.indkey[i] = atr.attnum LIMIT 1)
        ) sq;
        IF ('ID' != _pk) THEN
            RAISE EXCEPTION 'Different primary key defined for table storage.Image. Expected primary key: ID. Found: %', _pk;
        END IF;
    ELSE
        ALTER TABLE "storage"."Image" ADD CONSTRAINT "pk_Image" PRIMARY KEY("ID");
        COMMENT ON CONSTRAINT "pk_Image" ON "storage"."Image" IS 'NGS generated';
    END IF;
END $$ LANGUAGE plpgsql;
ALTER TABLE "storage"."Image" ALTER "ID" SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS(SELECT * FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE n.nspname = 'storage' AND c.relname = 'Image_ID_seq' AND c.relkind = 'S') THEN
        CREATE SEQUENCE "storage"."Image_ID_seq";
        ALTER TABLE "storage"."Image"    ALTER COLUMN "ID" SET DEFAULT NEXTVAL('"storage"."Image_ID_seq"');
        PERFORM SETVAL('"storage"."Image_ID_seq"', COALESCE(MAX("ID"), 0) + 1000) FROM "storage"."Image";
    END IF;
END $$ LANGUAGE plpgsql;
ALTER TABLE "storage"."Image" ALTER "body" SET NOT NULL;
ALTER TABLE "storage"."Image" ALTER "width" SET NOT NULL;
ALTER TABLE "storage"."Image" ALTER "height" SET NOT NULL;

SELECT "-NGS-".Persist_Concepts('"dsl/storage.dsl"=>"module storage
{
  aggregate Image {
    Binary  body;
    Int     width;
    Int     height;
  }
}
"', '\x','1.5.5940.19038');
SELECT pg_notify('migration', 'new');
