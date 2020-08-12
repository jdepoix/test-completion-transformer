import os
import sqlite3


class SetupManager():
    CREATE_TABLES_SCRIPT_PATH = 'assets/create_tables.sql'

    def __init__(self, working_dir):
        self._db_path = os.path.join(working_dir, 'test_relations_index.sqlite')

    def setup(self):
        with sqlite3.connect(self._db_path) as connection:
            with open(SetupManager.CREATE_TABLES_SCRIPT_PATH) as create_table_script:
                connection.cursor().executescript(create_table_script.read())
