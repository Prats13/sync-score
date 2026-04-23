create table confidential_scan_sessions (
  id uuid primary key,
  agency_id uuid not null references agency_profiles(id) on delete cascade,
  arch_scan_id uuid not null references architecture_scans(id) on delete cascade,

  source_type varchar(50) not null,
  exclusions_json jsonb,
  custom_exclusions text,

  created_at timestamptz not null default now()
);

create index idx_confidential_sessions_agency on confidential_scan_sessions (agency_id, created_at desc);
create index idx_confidential_sessions_scan on confidential_scan_sessions (arch_scan_id);
