create table architecture_scans (
  id uuid primary key,
  agency_id uuid not null references agency_profiles(id) on delete cascade,
  scan_event_id uuid references scan_events(id) on delete set null,

  status varchar(20) not null,
  confidence varchar(10),
  arch_status varchar(30),
  evidence_source varchar(30) not null default 'PUBLIC_EVIDENCE',

  structural_signals_json jsonb,
  summary_json jsonb,

  ruleset_version varchar(50) not null,
  error_message text,

  created_at timestamptz not null default now(),
  started_at timestamptz,
  finished_at timestamptz
);

create index idx_architecture_scans_agency on architecture_scans (agency_id, created_at desc);
create index idx_architecture_scans_status on architecture_scans (status, created_at desc);


create table arch_scan_structural_signals (
  id uuid primary key,
  architecture_scan_id uuid not null references architecture_scans(id) on delete cascade,

  signal_type varchar(50) not null,
  value_numeric numeric,
  value_label varchar(50),
  confidence_contribution numeric,

  created_at timestamptz not null default now()
);

create index idx_structural_signals_scan on arch_scan_structural_signals (architecture_scan_id);


create table architecture_review_cases (
  id uuid primary key,
  agency_id uuid not null references agency_profiles(id) on delete cascade,
  architecture_scan_id uuid not null references architecture_scans(id) on delete cascade,

  status varchar(20) not null default 'OPEN',
  trigger_reason varchar(100) not null,
  trigger_details_json jsonb,

  resolved_by uuid references users(id) on delete set null,
  resolution_note text,

  created_at timestamptz not null default now(),
  resolved_at timestamptz
);

create index idx_review_cases_status on architecture_review_cases (status, created_at desc);
create index idx_review_cases_agency on architecture_review_cases (agency_id, created_at desc);