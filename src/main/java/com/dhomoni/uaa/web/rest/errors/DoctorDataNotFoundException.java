package com.dhomoni.uaa.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class DoctorDataNotFoundException extends AbstractThrowableProblem {

    private static final long serialVersionUID = 1L;

    public DoctorDataNotFoundException() {
        super(ErrorConstants.ENTITY_NOT_FOUND_TYPE, "Doctor data not found", Status.BAD_REQUEST);
    }
}
