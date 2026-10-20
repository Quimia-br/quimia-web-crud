package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.UserRequestDTO;
import com.quimia.quimiawebcrud.dto.UserResponseDTO;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController extends GenericController<User, Integer, UserResponseDTO, UserRequestDTO> {

    private final UserService userService;

    @Override
    protected CrudService<User, Integer, UserRequestDTO, UserResponseDTO> getService() {
        return userService;
    }

    @Override
    protected String getBasePath() {
        return "/users";
    }

    @Override
    protected String getViewFolder() {
        return "user";
    }

    @Override
    protected String getSingularName() {
        return "user";
    }

    @Override
    protected String getPluralName() {
        return "users";
    }

    @Override
    protected UserResponseDTO newResponse() {
        return new UserResponseDTO();
    }
}
