package io.split.inputValidation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplitNameValidator {
    private static final Logger _log = LoggerFactory.getLogger(SplitNameValidator.class);

    public static InputValidationResult isValid(String name, String method) {
        if (name == null) {
            _log.error(String.format("%s: you passed a null split name, split name must be a non-empty string", method));
            return new InputValidationResult(false);
        }

        if (name.isEmpty()) {
            _log.error(String.format("%s: you passed an empty split name, split name must be a non-empty string", method));
            return new InputValidationResult(false);
        }

        String trimmed = name.trim();
        if (!trimmed.equals(name)) {
            _log.warn(String.format("%s: split name %s has extra whitespace, trimming", method, name));
            name = trimmed;
        }

        return new InputValidationResult(true, name);
    }
}
