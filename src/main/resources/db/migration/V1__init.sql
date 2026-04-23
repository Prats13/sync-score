create table if not exists users (
  id uuid primary key,
  email varchar(320) not null unique,
  username varchar(50) unique,
  password_hash varchar(255),
  status varchar(30) not null,
  created_at timestamptz not null default now()
);

create table if not exists user_roles (
  user_id uuid not null references users(id) on delete cascade,
  role varchar(30) not null,
  primary key (user_id, role)
);

create table if not exists auth_provider_identities (
  id uuid primary key,
  user_id uuid not null references users(id) on delete cascade,
  provider varchar(20) not null,
  provider_subject varchar(200) not null,
  email varchar(320) not null,
  created_at timestamptz not null default now(),
  unique (provider, provider_subject)
);

create table if not exists refresh_tokens (
  id uuid primary key,
  user_id uuid not null references users(id) on delete cascade,
  token_hash varchar(128) not null unique,
  issued_at timestamptz not null default now(),
  expires_at timestamptz not null,
  revoked_at timestamptz null
);

