import torch


class ThenSectionPredictor():
    def __init__(self, model, sos_index, eos_index, max_length):
        self._model = model
        self._sos_index = sos_index
        self._eos_index = eos_index
        self._max_length = max_length

    def predict(self, test_declaration_sequence):
        # TODO use final output or construct sentence out of [-1] of each outputs?
        # TODO how to handle sequences which are too long
        test_declaration_tensor = torch.tensor(test_declaration_sequence).to(self._model.device)
        prediction = [self._sos_index]
        while prediction[-1] != self._eos_index and len(prediction) < self._max_length:
            prediction.append(self._forward(test_declaration_tensor, prediction))
        return prediction

    def _forward(self, source, previous_predictions):
        with torch.no_grad():
            target = torch.tensor(previous_predictions).to(source.device)
            output = self._model(source.unsqueeze(1), target.unsqueeze(1))
            return output[-1].argmax().item()
