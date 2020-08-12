import os

import jar
import db
import logging


# TODO read all files
# TODO timeout processes
# TODO skip already seen repos


if __name__ == '__main__':
    # paths = [
    #     'assets/tars/spring-projects#spring-boot.tar.gz',
    #     'assets/tars/ReactiveX#RxJava.tar.gz'
    # ]
    WORKING_DIR = '../../temp'

    db.SetupManager(WORKING_DIR).setup()

    logging_manager = logging.Manager(
        logging.FileLogger(os.path.join(WORKING_DIR, 'logs/all.log')),
        logging.FileLogger(os.path.join(WORKING_DIR, 'logs/failed.log')),
        logging.FileLogger(os.path.join(WORKING_DIR, 'logs/successful.log')),
        logging.FileLogger(os.path.join(WORKING_DIR, 'logs/touched.log')),
        logging.FileLogger(os.path.join(WORKING_DIR, 'logs/errors.log')),
    )

    jar.ParallelRunner('assets/findTestRelations.jar', 1, [('../../testrepos/staticcode/ReactiveX#RxJava-master.tar.gz123', WORKING_DIR)], logging_manager.handle_finished_task).run()


