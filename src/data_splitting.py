import os
import sqlite3
from argparse import ArgumentParser


def retrieve_repo_test_counts(database_path, when_location):
    with sqlite3.connect(database_path) as connection:
        return connection.cursor().execute(
            'SELECT repo_name, count(*) AS test_count '
            'FROM test_relations '
            f'WHERE when_location=\'{when_location}\' '
            'GROUP BY repo_name '
            'ORDER BY test_count DESC'
        ).fetchall()


def retrieve_ids_for_repos(database_path, repo_names):
    repo_names_query = ", ".join(f'"{repo_name}"' for repo_name in repo_names)
    with sqlite3.connect(database_path) as connection:
        return connection.cursor().execute(
            'SELECT id '
            'FROM test_relations '
            f'WHERE repo_name IN ({repo_names_query}) '
        ).fetchall()


def split_repos(repo_test_counts, split_distribution):
    splits = [{'repos': [], 'size': 0} for _ in range(len(split_distribution))]
    distributed_count = 0

    for repo_name, test_count in repo_test_counts:
        relevant_split_index = next(
            (
                split_index for split_index, split in enumerate(splits)
                if distributed_count != 0 and split['size']/distributed_count < split_distribution[split_index]
            ),
            0
        )
        splits[relevant_split_index]['repos'].append(repo_name)
        splits[relevant_split_index]['size'] += test_count
        distributed_count += test_count

    return splits


def retrieve_id_splits(database_path, splits):
    return [[row[0] for row in retrieve_ids_for_repos(database_path, split['repos'])] for split in splits]


def create_data_split(database_path, output_dir, when_location, split):
    assert len(split) == 3
    assert sum(split) == 1

    repo_test_counts = retrieve_repo_test_counts(database_path, when_location)
    splits = split_repos(repo_test_counts, split)
    id_splits = retrieve_id_splits(database_path, splits)
    save_splits(output_dir, id_splits)


def save_splits(output_dir, id_splits):
    with open(f'{output_dir}/train_ids.txt', 'w+') as file:
        file.write('\n'.join(id_splits[0]))
    with open(f'{output_dir}/validate_ids.txt', 'w+') as file:
        file.write('\n'.join(id_splits[1]))
    with open(f'{output_dir}/test_ids.txt', 'w+') as file:
        file.write('\n'.join(id_splits[2]))


def get_parser():
    parser = ArgumentParser()
    parser.add_argument('--database_path', type=str, required=True, help='path to the raw dataset directory')
    parser.add_argument(
        '--output_dir',
        type=str,
        required=True,
        help='path to where the project-based data splits will be save'
    )
    parser.add_argument(
        '--when_location',
        type=str,
        required=True,
        help='only include datapoints in the split with the given location of the When call'
    )
    return parser


if __name__ == '__main__':
    args = get_parser().parse_args()
    if not os.path.exists(args.output_dir):
        os.makedirs(args.output_dir)

    create_data_split(args.database_path, args.output_dir, args.when_location, (.8, .1, .1))
