class TestRelationApi {
    static _ENTITY_NAME = 'test-relations';

    constructor(apiUrl) {
        this.apiUrl = apiUrl;
    }

    async listRepos(page, search) {
        let endpointUrl = `${this.apiUrl}/${TestRelationApi._ENTITY_NAME}/?page=${page}`;
        if (search) {
            endpointUrl += `&search=${search}`;
        }
        return await fetch(endpointUrl).then(response => response.json());
    }

    async getRepo(id) {
        return await fetch(`${this.apiUrl}/${TestRelationApi._ENTITY_NAME}/${id}`).then(response => response.json());
    }

    async getContext(id) {
        return await fetch(`${this.apiUrl}/${TestRelationApi._ENTITY_NAME}/${id}/context`).then(response => response.json());
    }
}

export default new TestRelationApi('http://localhost:8888/api');
