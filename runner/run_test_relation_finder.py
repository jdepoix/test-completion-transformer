import os
import subprocess
import datetime
import time
from collections import deque


class ParallelJarRunner():
    def __init__(self, jar_path, max_processes, arguments, callback):
        self._jar_path = jar_path
        self._max_processes = max_processes
        self._upcoming_tasks = deque(arguments)
        self._running_tasks = {}
        self._callback = callback

    def run(self):
        try:
            while len(self._upcoming_tasks) != 0 or len(self._running_tasks) != 0:
                self._remove_finished_tasks()
                self._start_tasks_if_possible()
                time.sleep(0.1)
        except:
            self._kill_all_running_tasks()


    def _start_tasks_if_possible(self):
        while len(self._running_tasks) < self._max_processes and len(self._upcoming_tasks) != 0:
            self._run_task(self._upcoming_tasks.popleft())

    def _run_task(self, argument):
        self._running_tasks[argument] = ({
            'process': subprocess.Popen(['java', '-jar', self._jar_path, argument]),
            'arguments': argument,
            'start': datetime.datetime.now(),
        })

    def _remove_finished_tasks(self):
        for finished_task_id in [
            task_id for task_id, running_task in self._running_tasks.items() if running_task['process'].poll() is not None
        ]:
            task = self._running_tasks.pop(finished_task_id)
            task['end'] = datetime.datetime.now()
            self._callback(task)

    def _kill_all_running_tasks(self):
        for running_task in self._running_tasks.values():
            running_task['process'].kill()


# TODO this is just a test
def callback(finished_task):
    print(finished_task)


# TODO read all files
# TODO logging
# TODO handle error codes

if __name__ == '__main__':
    # paths = [
    #     'assets/tars/spring-projects#spring-boot.tar.gz',
    #     'assets/tars/ReactiveX#RxJava.tar.gz'
    # ]
    base_path = 'assets/tars'
    paths = [os.path.join(base_path, path) for path in os.listdir(base_path)]
    ParallelJarRunner('runArgs.jar', 4, paths, callback).run()
    print("PY DONE")
