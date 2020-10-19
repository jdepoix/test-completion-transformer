class TransformerPredictionApi {
    static _PREDICTION_ENDPOINT = 'predictions';

    constructor(apiUrl) {
        this.apiUrl = apiUrl;
    }

    async getPrediction(modelName, testFileContent, testClassName, testMethodSignature, relatedFileContent, relatedClassName, relatedMethodSignature, thenSectionStartIndex) {
        return await fetch(`${this.apiUrl}/${TransformerPredictionApi._PREDICTION_ENDPOINT}/${modelName}`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                testFileContent: testFileContent,
                testClassName: testClassName,
                testMethodSignature: testMethodSignature,
                relatedFileContent: relatedFileContent,
                relatedClassName: relatedClassName,
                relatedMethodSignature: relatedMethodSignature,
                thenSectionStartIndex: thenSectionStartIndex,
            }),
        }).then(response => response.json());
    }
}

export default new TransformerPredictionApi('http://localhost:5050/api');
