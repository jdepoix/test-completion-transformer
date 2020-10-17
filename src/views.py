from abc import ABC, abstractmethod

import sqlite


class View(ABC):
    def __init__(self, sqlite_client: sqlite.Client):
        self._sqlite_client = sqlite_client

    def list(self, page, search: sqlite.SearchQuery = None):
        return self._sqlite_client.list(self.table_name, page=page, search=search)

    def get(self, pk):
        return self._sqlite_client.get(self.table_name, pk)

    def get_random(self):
        return self._sqlite_client.list(
            self.table_name,
            page=1,
            random_order=True,
            where={
                'gwt_resolution_status': 'RESOLVED',
                'when_location': 'GIVEN',
            }
        )[0]

    @property
    @abstractmethod
    def table_name(self) -> str:
        pass


class TestRelations(View):
    @property
    def table_name(self) -> str:
        return 'test_relations'


class TestContext(View):
    @property
    def table_name(self) -> str:
        return 'test_context'

    def get_for_test_relation(self, pk):
        return self._sqlite_client.list(self.table_name, where={'test_relation_id': pk})


class Files():
    def __init__(self, base_path):
        self._base_path = base_path

    def get(self, path):
        with open(f'{self._base_path}/{path}') as file:
            return file.read()
