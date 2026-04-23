create table if not exists user_email_otps (
  id uuid primary key,
  user_id uuid not null references users(id) on delete cascade,
  purpose varchar(30) not null,
  otp_hash varchar(100) not null,
  expires_at timestamptz not null,
  consumed_at timestamptz null,
  created_at timestamptz not null default now()
);

create index if not exists ix_user_email_otps_user_purpose_created
  on user_email_otps(user_id, purpose, created_at desc);

