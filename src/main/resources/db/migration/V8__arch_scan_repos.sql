create table arch_scan_repos (
  id uuid primary key,
  architecture_scan_id uuid not null references architecture_scans(id) on delete cascade,

  repo_full_name varchar(300) not null,
  commits_30d int not null default 0,
  commits_90d int not null default 0,
  contributor_count int not null default 0,
  max_folder_depth int not null default 0,
  service_count int not null default 0,
  source_file_count int not null default 0,
  repo_age_months int not null default 0,

  created_at timestamptz not null default now()
);

create index idx_arch_scan_repos_scan on arch_scan_repos (architecture_scan_id);