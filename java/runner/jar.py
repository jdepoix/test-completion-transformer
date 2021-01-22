import subprocess
import datetime
import time
from collections import deque


class ParallelRunner():
    def __init__(self, jar_path, max_processes, timeout_in_seconds):
        self._jar_path = jar_path
        self._max_processes = max_processes
        self._timeout_in_seconds = timeout_in_seconds

    def run(self, arguments, callback):
        upcoming_tasks = deque(arguments)
        running_tasks = {}

        try:
            while len(upcoming_tasks) != 0 or len(running_tasks) != 0:
                self._kill_timeouted_tasks(running_tasks)
                self._remove_finished_tasks(running_tasks, callback)
                self._start_tasks_if_possible(running_tasks, upcoming_tasks)
                time.sleep(0.1)
        except BaseException as e:
            self._kill_all_running_tasks(running_tasks)
            raise e

    def _start_tasks_if_possible(self, running_tasks, upcoming_tasks):
        while len(running_tasks) < self._max_processes and len(upcoming_tasks) != 0:
            arguments = upcoming_tasks.popleft()
            running_tasks[arguments] = self._run_task(arguments)

    def _run_task(self, arguments):
        return {
            'process': subprocess.Popen(
                ['java', '-jar', self._jar_path, *arguments],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            ),
            'arguments': arguments,
            'start': datetime.datetime.now(),
            'end': None,
        }

    def _kill_timeouted_tasks(self, running_tasks):
        for timeouted_task in (
            task for task in running_tasks.values()
            if (datetime.datetime.now() - task['start']).seconds > self._timeout_in_seconds
        ):
            self._kill_task(timeouted_task)

    def _remove_finished_tasks(self, running_tasks, callback):
        for finished_task_id in [
            task_id
            for task_id, running_task in running_tasks.items()
            if running_task['process'].poll() is not None
        ]:
            task = running_tasks.pop(finished_task_id)
            task['end'] = datetime.datetime.now()
            callback(task)

    def _kill_all_running_tasks(self, running_tasks):
        for running_task in running_tasks.values():
            self._kill_task(running_task)

    def _kill_task(self, task):
        task['process'].kill()
