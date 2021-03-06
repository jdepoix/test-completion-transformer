import os

from flask import Flask, request, jsonify
from flask_cors import CORS
import requests

import sqlite
import views


app = Flask(__name__)
cors = CORS(app)

sqlite_file = f'{os.environ["RESULT_DIR"]}/test_relations_index.sqlite'


@app.route('/api/test-relations/', methods=('GET',))
def list_test_relations():
    page = request.args.get('page', default=1, type=int)
    search = request.args.get('search', default=None, type=str)
    return jsonify(
        views.TestRelations(
            sqlite.Client(sqlite_file)
        ).list(
            page,
            sqlite.SearchQuery(
                search,
                ('repo_name', 'test_package', 'test_class', 'test_method'),
            ) if search else None
        )
    )


@app.route('/api/test-relations/random', methods=('GET',))
def get_random_test_relation():
    return jsonify(views.TestRelations(sqlite.Client(sqlite_file)).get_random())


@app.route('/api/test-relations/<pk>', methods=('GET',))
def get_test_relation(pk):
    return jsonify(views.TestRelations(sqlite.Client(sqlite_file)).get(pk))


@app.route('/api/test-relations/<pk>/context', methods=('GET',))
def get_test_context(pk):
    return jsonify(views.TestContext(sqlite.Client(sqlite_file)).get_for_test_relation(pk))


@app.route('/api/files/<path:path>', methods=('GET',))
def get_file(path):
    return views.Files(f'{os.environ["RESULT_DIR"]}/repos').get(path)


@app.route('/api/predictions/slm', methods=('POST',))
def predict_slm():
    return jsonify(
        requests.post('https://w0w3uc4a63.execute-api.us-east-1.amazonaws.com/prod/predict', json=request.json).json()
    )
