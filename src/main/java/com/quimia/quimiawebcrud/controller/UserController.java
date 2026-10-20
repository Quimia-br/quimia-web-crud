package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.UserRequestDTO;
import com.quimia.quimiawebcrud.dto.UserResponseDTO;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;
import java.util.function.Predicate;

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
    protected String getPluralName() {
        return "users";
    }

    @Override
    protected Predicate<UserResponseDTO> filterBy(Map<String, String> params, Model model) {
        return user -> Filters.matchesText(params.get("busca"), user.getUserName(), user.getEmail())
                && Filters.sameEnum(user.getStatus(), params.get("status"))
                && Filters.inDateRange(user.getSignUpDate(), params.get("cadastroDe"), params.get("cadastroAte"));
    }
}
