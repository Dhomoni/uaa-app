package com.dhomoni.uaa.web.rest.errors;

public class LicenceNumberAlreadyUsedException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public LicenceNumberAlreadyUsedException() {
        super(ErrorConstants.LICENCENUMBER_ALREADY_USED_TYPE, 
        		"Licence number is already in use!", "userManagement", "licencenumberexists");
    }
}
