#!/usr/bin/env bash
rm ../temp/test_relations_index.sqlite
cat create_tables.sql | sqlite3 ../temp/test_relations_index.sqlite