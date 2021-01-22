# A syntax-aware Transformer Model for Test Completion

This repositiory contains the source code created for my Master Thesis "A syntax-aware Transformer Model for Test Completion", which proposes a Transformer based model predicting assertions for a given unit test.
You can find the code for following tasks in this repository with explanations on how to use them.

* [Scraping data from GitHub to create a dataset from](#scraping-data-from-github-to-create-a-dataset-from)
* [Creating a test completion dataset from the scraped GitHub data](#Creating-a-test-completion-dataset-from-the-scraped-GitHub-data)
* [Preprocessing the dataset](#Preprocessing-the-dataset)
* [The dataset used in the thesis](#The-dataset-used-in-the-thesis)
* [Training the model](#Training-the-model)
* [Hyperparameter tuning](#Hyperparameter-tuning)
* [Creating a project-based data split](#Creating-a-project-based-data-split)
* [Evaluating a trained model](#Evaluating-a-trained-model)
* [Running the Data Explorer](#Running-the-Data-Explorer)

I recommend reading the thesis first ([which can also be found in this repository](thesis.pdf)) to get a better understanding of these tasks.
As some of them heavily rely on [JavaParser](https://github.com/javaparser/javaparser) features, the codebase is split into a [Python repository](python) and [Java repository](java).
The following section will explain how to make them work together for the described tasks.
Also, there are two separate repositories for the Data Explorer [frontend](data_explorer/frontend) and [backend](data_explorer/backend).
All of these repositories are git submodules of this main repository.
The raw dataset, trained model and datasplits used in this thesis [are uploaded here](https://hbx.fhhrz.net/public?folderID=MktmYVpGUll1TlNYUGNCM1lvYVhj).

### Setup

You should install the required dependencies first before continuing with the steps explained in the following sections.
For the Java codebase this is only needed if you wish to modify the code, as the [jars have been uploaded here](https://hbx.fhhrz.net/public?folderID=MktmYVpGUll1TlNYUGNCM1lvYVhj).

#### Python Repository

Create a virtualenv and install the requirements specified in `python/requirements.txt`:
```shell
cd python
python3 -m venv venv
venv/bin/pip install -r requirements.txt 
```

#### Java Repository

Download the [jars which have been uploaded here](https://hbx.fhhrz.net/public?folderID=MktmYVpGUll1TlNYUGNCM1lvYVhj).
The following instructions will assume that you put them into `java/jars`.

To build the Java project yourself [Maven](https://maven.apache.org/) is required.
To install the dependencies run:
```shell
mvn install
```

To compile the project and create the jar files run:
```shell
mvn package
```

#### Data Explorer

To install the requirements for the backend run:
```shell
cd data_explorer/backend
python3 -m venv venv
venv/bin/pip install -r requirements.txt 
```

To install the requirements for the frontend run (requires [yarn](https://yarnpkg.com/)):
```shell
cd data_exporer/frontend
yarn install
```

## Scraping data from GitHub to create a dataset from

If you want to build the dataset from scratch, you'll have to download GitHub repositories including JUnit tests.
In this thesis these repositories were identified using the [GitHub BigQuery dataset released on kaggle](https://www.kaggle.com/github/github-repos) and downloaded using a script presented in the following.

[The sql executed in BigQuery](python/retrieve_repos_with_tests.sql) can be found in the [Python repository](python).
You can execute this script in the [BigQuery cloud console](https://console.cloud.google.com/bigquery) and save the result as a CSV file.
Using this CSV file the scraping script can be executed:
```shell
cd python
venv/bin/python src/crawl_github.py --repo_csv_path="path-to-csv" --repo_root="output-directory" --github_auth_token="github-auth-token"
```

Execute the following to get more information about the parameters used:
```shell
venv/bin/python src/crawl_github.py --help
```

The downloaded data is not included in this repository, as it contains more than 500GB of compressed code.
However, the dataset created from this has been [uploaded here](https://hbx.fhhrz.net/public?folderID=MktmYVpGUll1TlNYUGNCM1lvYVhj).

## Creating a test-completion dataset from the scraped GitHub data

Once GitHub repositories containing JUnit tests have been downloaded, they can be used to create a dataset which maps tests to the methods they are testing.
Also, the test methods are labeled with information about their Given/When/Then sections.
As this process relies on AST analysis with JavaParser, it is implemented in the [Java repository](java).
Since this is a computationally expensive process it is parallelized.
Unfortunately JavaParser leaks memory while doing so, therefore, a small Python script is used (without any dependencies) to parallelize the process in multiple JVMs, thereby preventing memory leaks.

This script requires the `find-test-relations.jar` to be in `java/runner/assets`.
Therefore, move or copy it to there:
```shell
cd java
mv jars/find-test-relations.jar runner/assets/
```

Then you can start the dataset creation run:
```shell
python3 runner/main.py --repos_root_dir="directory-with-downloaded-repos" --working_dir="directory-where-to-store-resulting-dataset" --max_workers="max-jvm-count"
```

The following will provide additional information about the parameters used:
```shell
python3 runner/main.py --help
```

This will create the directory `working_dir` which contains a sqlite database file called `test_relations_index.sqlite` and a directory called `repos`.
Read the [dataset section](#the-dataset-used-in-the-thesis) to find out more about those.

## Preprocessing the dataset

There are two steps to preprocessing the raw dataset [created in the previous section](#creating-a-test-completion-dataset-from-the-scraped-github-data).
As explained in greater detail in the thesis, preprocessing requires resolving the context methods of the tests, which can only be done using JavaParser.
Therefore, the first preprocessing step is done using Java:
```shell
cd java/jars
java -jar create-gwt-dataset.jar path-to-raw-dataset-directory
```
The parameter `path-to-raw-dataset-directory` references the directory resulting from the [dataset creation process explained in the previous section](#creating-a-test-completion-dataset-from-the-scraped-github-data).

Executing this script will add a `datasets` subdirectory to the raw-dataset-directory.
There will be three files in this subdirectory: `gwt.jsonl`, `gwt_when_in_given.jsonl` and `gwt_when_in_both.jsonl`.
These are different versions of the dataset including all datapoint (`gwt.jsonl`), only those with when calls in the given section (`gwt_when_in_given.jsonl`) or only those with when calls in the given or given and then section (`gwt_when_in_both.jsonl`).
Each line in these files represents a single json encoded datapoint, which contains the AST sequence of the source Test Declaration AST and that of the target then section.
It also contains the non-AST-encoded corresponding code.

To complete the preprocessing this data has to be encoded to be compatible with the model.
This can be done by executing:
```shell
cd python
venv/bin/python src/preprocessing.py --input_dataset_path="/path/to/gwt_when_in_given.jsonl" --output_dir="encoded-dataset-directory" --tokenize_input_dataset=True
```

Execute the following to see the extensive list of parameters which can be used to employ predefined datasplits, use tokenize code encoding or resume preprocessing from a given step:
```shell
venv/bin/python src/preprocessing.py --help
```

## The dataset used in the thesis

The compressed raw dataset used in this thesis [has been uploaded here](https://hbx.fhhrz.net/public?folderID=MktmYVpGUll1TlNYUGNCM1lvYVhj).

When unzipped, this directory contains a `test_relations_index.sqlite` sqlite DB and a `repo` directory.
The sqlite DB contains information about all tests in the dataset and maps them to their method under test.
Also, it labels those datapoints with information about where the Given, When and Then are in the test.
However, in this raw dataset the corresponding code is not preprocessed in any way.
Instead, the datapoints contain paths to the test and target methods, which you can use to locate the corresponding files in the `repo` directory.
You can preprocess this dataset as described in the [previous section](#preprocessing-the-dataset) or create your own preprocessing process.

To inspect the schema used in the sqlite DB run:
```shell
sqlite3 test_relations_index.sqlite
>>> .schema test_relations
>>> CREATE TABLE test_relations (
        id TEXT PRIMARY KEY,
        repo_name TEXT NOT NULL,
        relation_type TEXT NOT NULL,
        gwt_resolution_status TEXT NOT NULL,
        test_package TEXT NOT NULL,
        test_class TEXT NOT NULL,
        test_method TEXT NOT NULL,
        test_method_signature TEXT NOT NULL,
        test_method_token_range TEXT NOT NULL,
        test_file_path TEXT,
        related_package TEXT,
        related_class TEXT,
        related_method TEXT,
        related_method_signature TEXT,
        related_method_token_range TEXT,
        related_file_path TEXT,
        given_section TEXT,
        then_section TEXT,
        when_location TEXT,
        then_section_start_index TEXT
    );
```

There also are compressed files containing the [data split in the upload directory](https://hbx.fhhrz.net/public?folderID=MktmYVpGUll1TlNYUGNCM1lvYVhj), which have been used to train the model, in case you want to train a model on the same split for better comparability.
Additionally, [the trained model can be found in the upload directory](https://hbx.fhhrz.net/public?folderID=MktmYVpGUll1TlNYUGNCM1lvYVhj), which also contains the BPE and dataset vocabulary when unzipped. 

## Training the model

To train the model on a preprocessed dataset execute:

```shell
cd python
venv/bin/python src/training.py --dataset_base_path='preprocessed-dataset-base-path' ...
```

There also is a plethora of optional parameters to configure hyperparameters and how the GPU is utilized.
Run the following to get an extensive list of all available parameters:
```shell
venv/bin/python src/training.py --help
```

## Hyperparameter tuning

To optimize the model's hyperparameteres with [Optuna](https://github.com/optuna/optuna) execute:
```shell
cd python
venv/bin/python src/hyperparameter_tuning.py --dataset_base_path='preprocessed-dataset-base-path' ...
```

The parameters are the same as when [training the model](#training-the-model).
To change the value ranges the hyperparameters are optimized on, you'll have to modify the `objective` function in `src/hyperparameter_tuning.py`.

## Creating a project-based data split

To create a new project-based split execute:
```shell
cd python
venv/bin/pytohn src/data_splitting.py --database_path='path-to-raw-dataset' --output_dir='output-path' --when_location='GIVEN'
```

This will generate the files `train_ids.txt`, `test_ids.txt` and `validate_ids.txt` containing the IDs of the respective splits separated by new-lines.

## Evaluating a trained model

As described in the thesis the evaluation process relies on the prediction pipeline.
Therefore, the Java sequentialization API must be running:
```shell
cd java/jars
java -jar sequentialization-api.jar <port> <worker-count>
```

To evaluate a trained model execute:

```shell
cd python
venv/bin/pytohn src/evaluation.py 
  --tensorboard_log_dir='tensorboard-log-dir-resulting-from-training' 
  --evaluation_dataset_path='evaluation-dataset' 
  --vocab_path='path-to-vocabulary' 
  --bpe_model_path='path-to-bpe-model' 
  --prediction_log_dir='path-to-where-to-log-generated-predictions' 
  --log_interval=1000 
  --format='AST' 
  --num_workers=20 
  --sampler_settings GREEDY NUCLEUS?top_p=0.8
  --device='cuda' 
  --max_number_of_checkpoints=1 
  --sequentialization_api_port=5555
  --sequentialization_api_host=localhost
```

You can get the extensive list of the parameters and how to use them by executing:
```shell
venv/bin/pytohn src/evaluation.py --help
```

## Running the Data Explorer

If you just want to use the data explorer start the [Data Explorer backend](data_explorer/backend) backend first:
```shell
cd data_explorer/backend
./run.sh <path-to-raw-dataset>
```

To start the frontend run:
```shell
cd data_explorer/frontend
yarn serve
```

However, if you also want to use the Prediction Explorer, you'll have to make sure that the sequentializetion API is running:
```shell
cd java/jars
java -jar sequentialization-api.jar <port> <worker-count>
```

As well as the prediction API:
```shell
cd python
./run_api.sh /path/to/model_dir /path/to/vocabulary.txt /path/to/bpe.model <port>
```

You can use the pretrained model [which has been uploaded here](https://hbx.fhhrz.net/public?folderID=MktmYVpGUll1TlNYUGNCM1lvYVhj).  

Note that it is not recommended to use `yarn serve` in production.
