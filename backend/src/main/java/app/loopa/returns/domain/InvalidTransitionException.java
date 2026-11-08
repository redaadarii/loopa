package app.loopa.returns.domain;

import app.loopa.shared.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidTransitionException extends BusinessException {

    public InvalidTransitionException(String number, ReturnStatus from, ReturnStatus to) {
        super("INVALID_TRANSITION", HttpStatus.CONFLICT,
                "Le retour " + number + " ne peut pas passer de " + from + " à " + to + ".");
    }
}
