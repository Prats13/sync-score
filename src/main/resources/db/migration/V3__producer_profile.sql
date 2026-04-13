create table producer_profiles (
  id uuid primary key,
  user_id uuid not null unique references users(id) on delete cascade,
  linkedin_url varchar(500) not null,
  github_url varchar(500),
  website_url varchar(500) not null,
  live_project_url varchar(500) not null,
  badge2_status varchar(30) not null default 'PENDING',
  linkedin_reachable boolean,
  github_reachable boolean,
  website_reachable boolean,
  live_project_reachable boolean,
  verified_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);