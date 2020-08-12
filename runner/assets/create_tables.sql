CREATE TABLE IF NOT EXISTS test_relations (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  repo_name TEXT NOT NULL,
  relation_type TEXT NOT NULL,
  resolution_status TEXT NOT NULL,
  gwt_resolution_status TEXT NOT NULL,
  test_package TEXT NOT NULL,
  test_class TEXT NOT NULL,
  test_method TEXT NOT NULL,
  test_file_path TEXT,
  related_package TEXT,
  related_class TEXT,
  related_method TEXT,
  related_file_path TEXT,
  given_section TEXT,
  when_section TEXT,
  then_section TEXT
);
