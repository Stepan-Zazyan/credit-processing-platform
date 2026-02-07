CREATE TABLE applications (
    id UUID PRIMARY KEY,
    client_name VARCHAR(255) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
