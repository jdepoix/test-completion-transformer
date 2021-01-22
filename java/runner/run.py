import os


class TestRelationFinder():
    def __init__(self, jar_runner, working_dir, blacklist_manager, logging_manager):
        self._jar_runner = jar_runner
        self._working_dir = working_dir
        self._blacklist_manager = blacklist_manager
        self._logging_manager = logging_manager

    def start(self, repos_root_dir):
        self._jar_runner.run(
            [
                (os.path.join(repos_root_dir, file), self._working_dir) for file in os.listdir(repos_root_dir)
                if not self._blacklist_manager.is_blacklisted(file)
            ],
            self._logging_manager.handle_finished_task
        )


class BlacklistManager():
    def __init__(self, *list_files):
        self._blacklist = self._generate_blacklist(list_files)

    def is_blacklisted(self, repo):
        return repo in self._blacklist

    def _generate_blacklist(self, list_files):
        blacklist = []
        for filepath in list_files:
            if os.path.exists(filepath):
                with open(filepath) as file:
                    blacklist += file.read().split('\n')
        return [list_item for list_item in blacklist if list_item]
