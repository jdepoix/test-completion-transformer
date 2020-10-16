#!/usr/bin/env bash

export DATA_DIR=$1
export MODEL_DIR=$2
export FLASK_ENV=development
export FLASK_APP=src/api.py
venv/bin/flask run --port=$3
