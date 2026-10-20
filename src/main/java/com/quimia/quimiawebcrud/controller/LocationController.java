package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.LocationResponseDTO;
import com.quimia.quimiawebcrud.service.LocationService;
import com.quimia.quimiawebcrud.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Consulta somente leitura das localizações informadas pelos usuários no aplicativo.
 */
@Controller
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController extends ListController<LocationResponseDTO> {

    private final LocationService locationService;
    private final UserService userService;

    @Override
    protected List<LocationResponseDTO> readAll() {
        return locationService.readAll();
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
    protected String getPluralName() {
        return "locations";
    }

    @Override
    protected void addListAttributes(Model model) {
        model.addAttribute("usersById", Lookups.byId(userService.readAll()));
    }

    @Override
    protected Predicate<LocationResponseDTO> filterBy(Map<String, String> params, Model model) {
        String coordinates = params.get("coordenadas");
        return location -> Filters.matchesText(params.get("cep"), location.getZipCode())
                && Filters.sameId(location.getUserId(), params.get("usuario"))
                && (coordinates == null || coordinates.isBlank()
                || ("COM".equals(coordinates) == (location.getLatitude() != null && location.getLongitude() != null)));
    }
}
