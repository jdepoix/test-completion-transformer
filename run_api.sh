#!/usr/bin/env bash

export MODEL_DIR=$1
export VOCAB_PATH=$2
export BPE_PATH=$3
export FLASK_ENV=development
export FLASK_APP=src/api.py
venv/bin/flask run --port=$4
