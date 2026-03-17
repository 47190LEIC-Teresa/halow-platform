CREATE OR REPLACE FUNCTION generate_mock_users(n INTEGER)
RETURNS VOID AS
$$
    BEGIN
    INSERT INTO app_user (username, email, first_name, last_name)
    SELECT
        'user_' || gs,
        'user_' || gs || '@example.com',
        'First_' || gs,
        'Last_' || gs
    FROM generate_series(1, n) gs;
    END;
$$ LANGUAGE plpgsql;

select generate_mock_users(5);