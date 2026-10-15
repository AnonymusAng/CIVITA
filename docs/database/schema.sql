CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE user_role AS ENUM ('CITIZEN', 'MODERATOR', 'ADMIN');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'SUSPENDED', 'DELETED');
CREATE TYPE report_status AS ENUM ('IN_REVIEW', 'VERIFIED', 'RESOLVED', 'DISCARDED');
CREATE TYPE severity_level AS ENUM ('LOW', 'MEDIUM', 'HIGH');
CREATE TYPE vote_type AS ENUM ('CONFIRM', 'DENY');
CREATE TYPE media_type AS ENUM ('IMAGE', 'VIDEO');
CREATE TYPE moderation_action AS ENUM ('VERIFY', 'DISCARD', 'RESOLVE', 'HIDE_COMMENT', 'RESTORE_COMMENT');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role user_role NOT NULL DEFAULT 'CITIZEN',
    trust_score NUMERIC(5,2) NOT NULL DEFAULT 50 CHECK (trust_score BETWEEN 0 AND 100),
    status user_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    location_enabled BOOLEAN NOT NULL DEFAULT false,
    notification_radius_m INTEGER NOT NULL DEFAULT 2000 CHECK (notification_radius_m BETWEEN 100 AND 50000),
    dark_mode BOOLEAN NOT NULL DEFAULT false,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE categories (
    id SMALLSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    icon_key VARCHAR(80),
    color_hex CHAR(7) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL REFERENCES users(id),
    category_id SMALLINT NOT NULL REFERENCES categories(id),
    title VARCHAR(140) NOT NULL,
    description TEXT NOT NULL,
    severity severity_level NOT NULL,
    status report_status NOT NULL DEFAULT 'IN_REVIEW',
    confidence_score NUMERIC(5,2) NOT NULL DEFAULT 50 CHECK (confidence_score BETWEEN 0 AND 100),
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX reports_location_gix ON reports USING GIST(location);
CREATE INDEX reports_status_category_idx ON reports(status, category_id, created_at DESC);

CREATE TABLE evidences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    media_type media_type NOT NULL,
    storage_url TEXT NOT NULL,
    thumbnail_url TEXT,
    mime_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE validations (
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    vote vote_type NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (report_id, user_id)
);

CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    parent_comment_id UUID REFERENCES comments(id),
    body VARCHAR(1000) NOT NULL,
    hidden BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE report_supports (
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (report_id, user_id)
);

CREATE TABLE report_status_history (
    id BIGSERIAL PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    changed_by UUID NOT NULL REFERENCES users(id),
    previous_status report_status,
    new_status report_status NOT NULL,
    reason VARCHAR(500),
    confidence_after NUMERIC(5,2) CHECK (confidence_after BETWEEN 0 AND 100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE moderation_actions (
    id BIGSERIAL PRIMARY KEY,
    report_id UUID REFERENCES reports(id) ON DELETE CASCADE,
    comment_id UUID REFERENCES comments(id) ON DELETE CASCADE,
    moderator_id UUID NOT NULL REFERENCES users(id),
    action moderation_action NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (report_id IS NOT NULL OR comment_id IS NOT NULL)
);

CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by UUID REFERENCES users(id),
    category_id SMALLINT REFERENCES categories(id),
    title VARCHAR(160) NOT NULL,
    description TEXT,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    CHECK (ends_at > starts_at)
);

CREATE INDEX events_location_gix ON events USING GIST(location);

CREATE TABLE alert_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id SMALLINT REFERENCES categories(id),
    center GEOGRAPHY(POINT, 4326),
    radius_m INTEGER NOT NULL DEFAULT 2000 CHECK (radius_m BETWEEN 100 AND 50000),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    report_id UUID REFERENCES reports(id) ON DELETE SET NULL,
    event_id UUID REFERENCES events(id) ON DELETE SET NULL,
    title VARCHAR(140) NOT NULL,
    body VARCHAR(500) NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (report_id IS NOT NULL OR event_id IS NOT NULL)
);

CREATE TABLE map_layers (
    id SMALLSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    source_name VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    style_json JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE layer_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    layer_id SMALLINT NOT NULL REFERENCES map_layers(id) ON DELETE CASCADE,
    geometry GEOMETRY(GEOMETRY, 4326) NOT NULL,
    properties JSONB NOT NULL DEFAULT '{}'::jsonb,
    observed_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ
);

CREATE INDEX layer_features_geometry_gix ON layer_features USING GIST(geometry);

INSERT INTO categories(code, name, icon_key, color_hex) VALUES
('MOBILITY', 'Movilidad', 'directions_car', '#E8505B'),
('ROADWORKS', 'Obras y cierres', 'construction', '#F4A340'),
('SECURITY', 'Seguridad', 'shield', '#7C5CC4'),
('EVENTS', 'Eventos', 'event', '#2E77D0'),
('ENVIRONMENT', 'Ambiente', 'eco', '#31A66A');
