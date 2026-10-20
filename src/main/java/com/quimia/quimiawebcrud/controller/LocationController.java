package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.LocationRequestDTO;
import com.quimia.quimiawebcrud.dto.LocationResponseDTO;
import com.quimia.quimiawebcrud.model.Location;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.LocationService;
import com.quimia.quimiawebcrud.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController extends GenericController<Location, Integer, LocationResponseDTO, LocationRequestDTO> {

    private final LocationService locationService;
    private final UserService userService;

    @Override
    protected CrudService<Location, Integer, LocationRequestDTO, LocationResponseDTO> getService() {
        return locationService;
    }

    @Override
    protected String getBasePath() {
        return "/locations";
    }

    @Override
    protected String getViewFolder() {
        return "location";
    }

    @Override
    protected String getSingularName() {
        return "location";
    }

    @Override
    protected String getPluralName() {
        return "locations";
    }

    @Override
    protected LocationResponseDTO newResponse() {
        return new LocationResponseDTO();
    }

    @Override
    protected void addFormAttributes(Model model) {
        model.addAttribute("users", userService.readAll());
    }
}
