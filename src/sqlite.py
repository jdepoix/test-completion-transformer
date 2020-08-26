from typing import List

import sqlite3


class SearchQuery():
    def __init__(self, search_term: str, fields: List[str]):
        self.search_term = search_term
        self.fields = fields

    def to_query(self):
        return " OR ".join(
            f'LOWER({field}) LIKE "%{self.search_term.lower()}%"'
            for field in self.fields
        )


class Client():
    class IllegalQuery(Exception):
        pass

    PAGINATION_LIMIT = 50

    def __init__(self, sqlite_file):
        self._sqlite_file = sqlite_file

    def list(self, table_name, page=None, search: SearchQuery = None, where=None, random_order=False):
        with sqlite3.connect(self._sqlite_file) as connection:
            cursor = connection.cursor()
            cursor.execute(
                self._build_query(table_name, page=page, search=search, where=where, random_order=random_order)
            )
            column_names = list(map(lambda x: x[0], cursor.description))
            return [
                {
                    column_names[column_index]: column
                    for column_index, column in enumerate(row)
                }
                for row in cursor.fetchall()
            ]

    def get(self, table_name, pk):
        with sqlite3.connect(self._sqlite_file) as connection:
            cursor = connection.cursor()
            cursor.execute(self._build_query(table_name, pk=pk))
            column_names = list(map(lambda x: x[0], cursor.description))
            return {
                column_names[column_index]: column
                for column_index, column in enumerate(cursor.fetchone())
            }

    def _build_query(self, table_name, page=None, search: SearchQuery = None, pk=None, where=None, random_order=False):
        query = f'SELECT * FROM {table_name}'

        if (search is not None, pk is not None, where is not None).count(True) > 1:
            raise Client.IllegalQuery('Only one of the following can be set: pk, where, search!')

        if search:
            query += f' WHERE {search.to_query()}'

        if pk:
            query += f' WHERE id = "{pk}"'

        if where:
            query += ' WHERE {}'.format(' AND '.join(f'{key} = "{value}"' for key, value in where.items()))

        if random_order:
            query += ' ORDER BY RANDOM()'

        if page:
            query += f' LIMIT {self.PAGINATION_LIMIT} OFFSET {self.PAGINATION_LIMIT * (page - 1)}'

        return query
