import os


class TestRelationFinderRunner():
    def __init__(self):
        pass

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
