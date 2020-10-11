class Token():
    @staticmethod
    def is_value(token):
        return not (token.startswith('<[') and token.endswith(']>'))
