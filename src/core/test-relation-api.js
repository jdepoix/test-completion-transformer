class TestRelationApi {
    static _ENTITY_NAME = 'test_relations';

    constructor(apiUrl) {
        this.apiUrl = apiUrl;
    }

    async listRepos(page, repoName) {
        let endpointUrl = `${this.apiUrl}/${TestRelationApi._ENTITY_NAME}/?page=${page}`;
        if (repoName) {
            endpointUrl += `&repo_name=%${repoName}%`;
        }
        return await fetch(endpointUrl).then(response => response.json());
    }

    async getRepo(id) {
        return await fetch(`${this.apiUrl}/${TestRelationApi._ENTITY_NAME}/${id}`).then(response => response.json());
    }
}

export default new TestRelationApi('http://localhost:8888/api');
