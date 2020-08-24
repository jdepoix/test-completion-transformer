CREATE TABLE IF NOT EXISTS test_relations (
    id TEXT PRIMARY KEY,
    repo_name TEXT NOT NULL,
    relation_type TEXT NOT NULL,
    resolution_status TEXT NOT NULL,
    gwt_resolution_status TEXT NOT NULL,
    test_package TEXT NOT NULL,
    test_class TEXT NOT NULL,
    test_method TEXT NOT NULL,
    test_method_signature TEXT NOT NULL,
    test_file_path TEXT,
    related_package TEXT,
    related_class TEXT,
    related_method TEXT,
    related_method_signature TEXT,
    related_file_path TEXT,
    given_section TEXT,
    then_section TEXT
);

CREATE TABLE IF NOT EXISTS test_context (
    test_relation_id TEXT NOT NULL,
    resolution_status TEXT NOT NULL,
    method_call TEXT NOT NULL,
    package TEXT,
    class TEXT,
    method TEXT,
    method_signature TEXT,
    path TEXT,
    FOREIGN KEY(test_relation_id) REFERENCES test_relations(id)
);
