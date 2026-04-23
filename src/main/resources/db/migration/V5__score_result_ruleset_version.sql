-- Add ruleset_version to score_results so the public score is self-contained and
-- auditable without requiring a join back to scan_events.
alter table score_results
  add column if not exists ruleset_version varchar(50) not null default 'syncscore-v1-2026-04-14';
