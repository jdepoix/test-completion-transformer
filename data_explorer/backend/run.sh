#!/usr/bin/env bash

export RESULT_DIR=$1
export FLASK_ENV=development
export FLASK_APP=src/routes.py
.venv/bin/flask run
