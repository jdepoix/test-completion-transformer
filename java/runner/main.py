import os
import sys
from argparse import ArgumentParser

import jar
import db
import logging
import run


def get_parser():
    parser = ArgumentParser()
    parser.add_argument('--repos_root_dir', type=str, required=True, help='root directory of the compressed scraped GitHub repositories')
    parser.add_argument('--working_dir', type=str, required=True, help='path where to store the resulting dataset')
    parser.add_argument('--max_workers', type=int, required=True, help='amount of worker processes used')
    return parser


if __name__ == '__main__':
    args = get_parser().parse_args()

    db.SetupManager(args.working_dir).setup()

    touched_log_file = os.path.join(args.working_dir, 'logs/touched.log')
    logging_manager = logging.Manager(
        logging.FileLogger(os.path.join(args.working_dir, 'logs/all.log')),
        logging.FileLogger(os.path.join(args.working_dir, 'logs/failed.log')),
        logging.FileLogger(os.path.join(args.working_dir, 'logs/successful.log')),
        logging.FileLogger(touched_log_file),
        logging.FileLogger(os.path.join(args.working_dir, 'logs/errors.log')),
    )

    run.TestRelationFinder(
        jar.ParallelRunner('assets/find-test-relations.jar', args.max_workers, 5 * 60),
        args.working_dir,
        run.BlacklistManager(touched_log_file),
        logging_manager,
    ).start(args.repos_root_dir)
