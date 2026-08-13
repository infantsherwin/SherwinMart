Numbered, checked-in migration files per Section 14. Do not hand-edit a live table —
add a new V<n>__description.sql file instead. schema.sql in src/main/resources is the
authoritative "current state" applied automatically by DataSourceListener on startup;
V1 here is that same baseline captured as migration #1.
