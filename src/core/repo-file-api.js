class RepoFileApi {
    constructor(apiUrl) {
        this.apiUrl = apiUrl;
    }

    async getFileContent(filePath) {
        return await fetch(`${this.apiUrl}/${filePath}`).then(response => response.text());
    }
}

export default new RepoFileApi('http://localhost:9999/files');