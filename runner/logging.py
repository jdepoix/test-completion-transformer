import pathlib


class FileLogger():
    def __init__(self, filepath):
        pathlib.Path(filepath).parent.mkdir(parents=True, exist_ok=True)
        self._filepath = filepath

    def log(self, message):
        with open(self._filepath, 'a+') as logfile:
            logfile.write(f'{message}\n')


class Manager():
    def __init__(self, all_logger, failed_logger, successful_logger, touched_logger, error_logger):
        self._all_logger = all_logger
        self._failed_logger = failed_logger
        self._successful_logger = successful_logger
        self._touched_logger = touched_logger
        self._error_logger = error_logger

    def handle_finished_task(self, task):
        filename = task['arguments'][0].split('/')[-1]
        successful = task['process'].returncode == 0

        self._all_logger.log(
            f'{filename}: {"finished" if successful else "FAILED"} '
            f'after {(task["end"] - task["start"]).seconds} seconds.'
        )
        self._touched_logger.log(filename)

        if successful:
            self._successful_logger.log(filename)
        else:
            self._failed_logger.log(filename)
            _, stderr = task['process'].communicate()
            self._error_logger.log(f'{filename}:\n{stderr.decode()}\n')
