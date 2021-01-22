SELECT DISTINCT(files.repo_name)
FROM `bigquery-public-data.github_repos.files` files
JOIN `bigquery-public-data.github_repos.contents` contents
ON files.id = contents.id
WHERE repo_name IN (
    SELECT repo_name as rname
    FROM `bigquery-public-data.github_repos.languages`, UNNEST(language) as lang
    WHERE lang.name = "Java"
)
AND contents.content is not null
AND contents.content like "%org.junit%";