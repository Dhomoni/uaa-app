package com.dhomoni.uaa.web.rest.vm;

import javax.validation.constraints.Size;

import com.dhomoni.uaa.service.dto.UserDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * View Model extending the UserDTO, which is meant to be used in the user management UI.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ManagedUserVM extends UserDTO {

    public static final int PASSWORD_MIN_LENGTH = 4;

    public static final int PASSWORD_MAX_LENGTH = 100;

    @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
    private String password;

    public ManagedUserVM() {
        // Empty constructor needed for Jackson.
    }
}
