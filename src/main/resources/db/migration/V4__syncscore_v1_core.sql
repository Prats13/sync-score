-- Sync Score V1 core infra (V1-strict, V2-ready evidence inbox)

create table if not exists agency_profiles (
  id uuid primary key,
  user_id uuid not null unique references users(id) on delete cascade,

  name varchar(200) not null,
  niche varchar(200),
  website_url varchar(500),
  description text,
  booking_url varchar(500),
  github_username varchar(200),

  is_public boolean not null default false,
  -- Cap manual rescans for now (enforced by app logic too)
  rescan_count int not null default 0,
  rescan_limit int not null default 5,
  -- GitHub scanning cap defaults (enforced by app logic; hard max is 15)
  repo_scan_limit int not null default 10,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_agency_profiles_public on agency_profiles (is_public);


-- Evidence inbox: store arbitrary evidence for future V2 analyzers.
-- V1 scoring uses only manifest evidence items (github_username, manifest_text, manifest_file).
create table if not exists evidence_items (
  id uuid primary key,
  agency_id uuid not null references agency_profiles(id) on delete cascade,

  evidence_type varchar(50) not null,
  -- Optional plaintext (e.g., pasted manifest content or user feedback)
  content_text text,
  -- Optional URL (e.g., public profile link); for uploads store a reference in payload_json
  content_url varchar(1000),
  -- V2-ready metadata / structured payload (e.g., uploaded file info)
  payload_json jsonb,

  created_at timestamptz not null default now()
);

create index if not exists idx_evidence_items_agency on evidence_items (agency_id, created_at desc);


create table if not exists verification_submissions (
  id uuid primary key,
  agency_id uuid not null references agency_profiles(id) on delete cascade,

  source_type varchar(20) not null, -- github | paste
  status varchar(30) not null,      -- pending | processed | failed
  created_at timestamptz not null default now()
);

create index if not exists idx_verification_submissions_agency on verification_submissions (agency_id, created_at desc);


create table if not exists scan_events (
  id uuid primary key,
  agency_id uuid not null references agency_profiles(id) on delete cascade,

  trigger_type varchar(20) not null, -- initial | rescan
  status varchar(30) not null,       -- queued | running | succeeded | failed
  error_message text,

  ruleset_version varchar(50) not null,
  verification_label varchar(30) not null, -- GITHUB_VERIFIED | SELF_REPORTED

  -- Snapshot of evidence item ids used for this scan to support iterative refinement.
  evidence_item_ids jsonb,

  started_at timestamptz,
  finished_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists idx_scan_events_agency on scan_events (agency_id, created_at desc);
create index if not exists idx_scan_events_status on scan_events (status, created_at desc);


create table if not exists repository_scans (
  id uuid primary key,
  scan_event_id uuid not null references scan_events(id) on delete cascade,

  repo_full_name varchar(400) not null,
  repo_url varchar(1000) not null,
  default_branch varchar(200),

  status varchar(30) not null, -- scanned | skipped | failed
  error_message text,

  manifests_found jsonb,

  created_at timestamptz not null default now()
);

create index if not exists idx_repository_scans_scan_event on repository_scans (scan_event_id);


create table if not exists detected_packages (
  id uuid primary key,
  scan_event_id uuid not null references scan_events(id) on delete cascade,
  repository_scan_id uuid references repository_scans(id) on delete set null,

  manifest_type varchar(30) not null, -- package_json | requirements_txt
  manifest_path varchar(1000),

  package_name_normalized varchar(200) not null,
  category varchar(50) not null,
  points_awarded int not null,

  created_at timestamptz not null default now()
);

create index if not exists idx_detected_packages_scan_event on detected_packages (scan_event_id);
create index if not exists idx_detected_packages_pkg on detected_packages (package_name_normalized);


create table if not exists score_results (
  id uuid primary key,
  scan_event_id uuid not null unique references scan_events(id) on delete cascade,

  total_score int not null,
  tier varchar(20) not null, -- Wrapper | Builder | Expert

  category_subtotals jsonb,
  created_at timestamptz not null default now()
);

create index if not exists idx_score_results_total on score_results (total_score desc);


create table if not exists public_profiles (
  id uuid primary key,
  agency_id uuid not null unique references agency_profiles(id) on delete cascade,

  slug varchar(250) not null unique,
  published_at timestamptz,
  is_public boolean not null default false,

  -- Snapshot for display (so the public page doesn't require recomputation).
  verification_label varchar(30) not null,
  latest_score_result_id uuid references score_results(id) on delete set null,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_public_profiles_public on public_profiles (is_public);

