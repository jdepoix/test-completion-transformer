import os
import csv
import shutil
import traceback
from argparse import ArgumentParser

import requests


class FileLogger():
    def __init__(self, filepath):
        self._filepath = filepath

    def log(self, message):
        with open(self._filepath, 'a+') as logfile:
            logfile.write(f'{message}\n')


class GitHubClient():
    class NotFound(Exception):
        pass

    def __init__(self, auth_thoken):
        self._auth_token = auth_thoken

    def is_relevant(self, repo):
        response = self.get(f'repos/{repo}')

        if response.status_code == 404:
            raise GitHubClient.NotFound()

        repo_data = response.json()
        if repo_data['fork'] and repo_data['stargazers_count'] < 20:
            return False
        return True

    def get(self, path):
        return requests.get(
            f'https://api.github.com/{path}',
            headers={
                'Authorization': f'token {self._auth_token}'
            }
        )

    def get_tarball(self, repo):
        response = requests.get(f'https://github.com/{repo}/archive/master.tar.gz', stream=True)
        if response.status_code == 404:
            response.close()
            raise GitHubClient.NotFound()
        response.raise_for_status()

        return response


class RepoDownloader():
    class AlreadyDownloaded(Exception):
        pass

    class IsIrrelevant(Exception):
        pass

    class TooBig(Exception):
        pass

    def __init__(self, repo_root, github_client, blacklist_manager):
        self._repo_root = repo_root
        self._github_client = github_client
        self._blacklist_manager = blacklist_manager

    def download(self, repo):
        filepath = self._get_filepath(repo)

        if os.path.exists(filepath) or self._blacklist_manager.is_blacklisted(repo):
            raise RepoDownloader.AlreadyDownloaded()

        try:
            if not self._github_client.is_relevant(repo):
                raise RepoDownloader.IsIrrelevant()

            self._download_tarball(repo, filepath)
            self._check_tarball_size(filepath)
        except Exception as e:
            if os.path.exists(filepath):
                os.remove(filepath)

            raise e

    def _download_tarball(self, repo, filepath):
        with \
            self._github_client.get_tarball(repo) as response, \
            open(filepath, 'wb') as file:
            shutil.copyfileobj(response.raw, file)

    def _check_tarball_size(self, filepath):
        if os.path.getsize(filepath) >= 250000000:
            os.remove(filepath)
            raise RepoDownloader.TooBig()

    def _get_filepath(self, repo):
        splitted_repo_name = repo.split('/')
        return f'{self._repo_root}/{splitted_repo_name[0]}#{splitted_repo_name[1]}.tar.gz'


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


class RepoCrawler():
    def __init__(
        self,
        repo_downloader,
        logger,
        successful_logger,
        irrelevant_logger,
        failed_logger,
        not_found_logger,
        error_logger,
        too_big_logger
    ):
        self._repo_downloader = repo_downloader
        self._logger = logger
        self._successful_logger = successful_logger
        self._irrelevant_logger = irrelevant_logger
        self._failed_logger = failed_logger
        self._not_found_logger = not_found_logger
        self._error_logger = error_logger
        self._too_big_logger = too_big_logger

    def crawl(self, csv_filepath):
        counter = 0
        for repo in self._iterate_csv(csv_filepath):
            counter += 1
            try:
                self._repo_downloader.download(repo)
                self._logger.log(f'#{counter} downloaded repo {repo}')
                self._successful_logger.log(repo)
            except RepoDownloader.AlreadyDownloaded:
                pass
            except RepoDownloader.TooBig:
                self._logger.log(f'#{counter} repo {repo} is TOO BIG')
                self._too_big_logger.log(repo)
            except RepoDownloader.IsIrrelevant:
                self._logger.log(f'#{counter} repo {repo} is IRRELEVANT')
                self._irrelevant_logger.log(repo)
            except GitHubClient.NotFound:
                self._logger.log(f'#{counter} repo {repo} NOT FOUND')
                self._not_found_logger.log(repo)
            except Exception as e:
                self._logger.log(f'#{counter} repo {repo} FAILED')
                self._failed_logger.log(repo)
                self._error_logger.log(f'#{counter} repo {repo} FAILED\n{traceback.format_exc()}\n')
        self._logger.log(f'#{counter} DONE!!!')

    def _iterate_csv(self, filename):
        with open(filename, 'r') as csvfile:
            iterator = csv.reader(csvfile, delimiter=' ', quotechar='|')
            next(iterator)
            for row in iterator:
                yield row[0]


def get_parser():
    parser = ArgumentParser()
    parser.add_argument('--repo_csv_path', type=str, required=True)
    parser.add_argument(
        '--repo_root',
        type=str,
        required=True,
        help='The folder where the compressed repositories will be stored',
    )
    parser.add_argument(
        '--github_auth_token',
        type=str,
        required=True,
        help='The GitHub auth token used to validate using the GitHub API what the star count of a repository is',
    )
    return parser


if __name__ == '__main__':
    args = get_parser().parse_args()

    RepoCrawler(
        RepoDownloader(
            args.repo_root,
            GitHubClient(args.github_auth_token),
            BlacklistManager('logs/irrelevant.log', 'logs/not_found.log', 'logs/too_big.log')
        ),
        FileLogger('logs/all.log'),
        FileLogger('logs/successful.log'),
        FileLogger('logs/irrelevant.log'),
        FileLogger('logs/failed.log'),
        FileLogger('logs/not_found.log'),
        FileLogger('logs/errors.log'),
        FileLogger('logs/too_big.log'),
    ).crawl(args.repo_csv_path)
