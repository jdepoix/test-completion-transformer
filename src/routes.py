import os

from flask import Flask, request, jsonify

import sqlite
import views

app = Flask(__name__)


@app.route('/test-relations/', methods=('GET',))
def list_test_relations():
    page = request.args.get('page', default=1, type=int)
    search = request.args.get('search', default=None, type=str)
    return jsonify(
        views.TestRelations(
            sqlite.Client(f'{os.environ["RESULT_DIR"]}/test_relations_index.sqlite')
        ).list(
            page,
            sqlite.SearchQuery(
                search,
                ('repo_name', 'test_package', 'test_class', 'test_method'),
            ) if search else None
        )
    )


@app.route('/test-relations/random', methods=('GET',))
def get_random_test_relation():
    return jsonify(
        views.TestRelations(
            sqlite.Client(f'{os.environ["RESULT_DIR"]}/test_relations_index.sqlite')
        ).get_random()
    )


@app.route('/test-relations/<pk>', methods=('GET',))
def get_test_relation(pk):
    return jsonify(
        views.TestRelations(sqlite.Client(os.environ['RESULT_DIR'] + 'test_relations_index.sqlite')).get(pk)
    )


@app.route('/test-relations/<pk>/context', methods=('GET',))
def get_test_context(pk):
    return jsonify(
        views.TestContext(
            sqlite.Client(os.environ['RESULT_DIR'] + 'test_relations_index.sqlite')
        ).get_for_test_relation(pk)
    )


@app.route('/files/<path:path>', methods=('GET',))
def get_file(path):
    return views.Files(f'{os.environ["RESULT_DIR"]}/repos').get(path)
