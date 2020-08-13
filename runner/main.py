import os
import sys

import jar
import db
import logging
import run


if __name__ == '__main__':
    REPOS_ROOT_DIR = sys.argv[1]
    WORKING_DIR = sys.argv[2]
    MAX_WORKERS = sys.argv[3]

    db.SetupManager(WORKING_DIR).setup()

    touched_log_file = os.path.join(WORKING_DIR, 'logs/touched.log')
    logging_manager = logging.Manager(
        logging.FileLogger(os.path.join(WORKING_DIR, 'logs/all.log')),
        logging.FileLogger(os.path.join(WORKING_DIR, 'logs/failed.log')),
        logging.FileLogger(os.path.join(WORKING_DIR, 'logs/successful.log')),
        logging.FileLogger(touched_log_file),
        logging.FileLogger(os.path.join(WORKING_DIR, 'logs/errors.log')),
    )

    run.TestRelationFinder(
        jar.ParallelRunner('assets/findTestRelations.jar', MAX_WORKERS, 5 * 60),
        WORKING_DIR,
        run.BlacklistManager(touched_log_file),
        logging_manager,
    ).start(REPOS_ROOT_DIR)
