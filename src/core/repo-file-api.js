class RepoFileApi {
    constructor(apiUrl) {
        this.apiUrl = apiUrl;
    }

    async getFileContent(filePath) {
        return await fetch(`${this.apiUrl}/files/${filePath}`).then(response => response.text());
    }
}

export default new RepoFileApi('http://localhost:8888/api');