-- V5: Add implicit cast from text/varchar to vector
-- Fixes: Hibernate inserts embedding as varchar, but column is of type vector.

-- Create a function that casts text input to vector type
CREATE OR REPLACE FUNCTION public.cast_text_to_vector(text_input text)
RETURNS public.vector
AS $$
BEGIN
    RETURN text_input::public.vector;
END;
$$ LANGUAGE plpgsql IMMUTABLE STRICT;

-- Create implicit cast from varchar to vector
CREATE CAST (varchar AS public.vector)
WITH FUNCTION public.cast_text_to_vector(text)
AS IMPLICIT;

-- Create implicit cast from text to vector
CREATE CAST (text AS public.vector)
WITH FUNCTION public.cast_text_to_vector(text)
AS IMPLICIT;
