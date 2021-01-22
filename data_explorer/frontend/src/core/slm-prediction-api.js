class SlmPredictionApi {
    constructor(apiUrl) {
        this.apiUrl = apiUrl;
    }

    async getPrediction(code) {
        return await fetch(`${this.apiUrl}/predictions/slm`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                code: code
            }),
        }).then(response => response.json());
    }
}

export default new SlmPredictionApi('http://localhost:5000/api');