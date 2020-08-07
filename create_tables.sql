CREATE TABLE IF NOT EXISTS test_relations (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  repo_name TEXT NOT NULL,
  test_package TEXT NOT NULL,
  test_class TEXT NOT NULL,
  test_method TEXT NOT NULL,
  test_file_path TEXT,
  related_package TEXT,
  related_class TEXT,
  related_method TEXT,
  related_file_path TEXT,
  resolution_status TEXT,
  relation_type TEXT
);
