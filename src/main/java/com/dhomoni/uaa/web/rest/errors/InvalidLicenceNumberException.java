package com.dhomoni.uaa.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class InvalidLicenceNumberException extends AbstractThrowableProblem {
    private static final long serialVersionUID = 1L;

    public InvalidLicenceNumberException() {
        super(ErrorConstants.INVALID_LICENCENUMBER_TYPE, "Incorrect licence number", Status.BAD_REQUEST);
    }

}
