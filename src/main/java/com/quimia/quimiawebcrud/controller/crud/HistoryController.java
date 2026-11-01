package com.quimia.quimiawebcrud.controller.crud;

import com.quimia.quimiawebcrud.controller.support.Filters;
import com.quimia.quimiawebcrud.controller.support.ListController;
import com.quimia.quimiawebcrud.controller.support.Lookups;
import com.quimia.quimiawebcrud.dto.response.HistoryResponseDTO;
import com.quimia.quimiawebcrud.dto.response.UserResponseDTO;
import com.quimia.quimiawebcrud.service.crud.HistoryService;
import com.quimia.quimiawebcrud.service.crud.HistoryTypeService;
import com.quimia.quimiawebcrud.service.crud.ShelfService;
import com.quimia.quimiawebcrud.service.crud.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Consulta somente leitura do histórico de uso registrado pelos usuários no aplicativo.
 */
@Controller
@RequestMapping("/histories")
@RequiredArgsConstructor
public class HistoryController extends ListController<HistoryResponseDTO> {

    private final HistoryService historyService;
    private final UserService userService;
    private final ShelfService shelfService;
    private final HistoryTypeService historyTypeService;

    @Override
    protected List<HistoryResponseDTO> readAll() {
        return historyService.readAll();
    }

    @Override
    protected String getBasePath() {
        return "/histories";
    }

    @Override
    protected String getViewFolder() {
        return "history";
    }

    @Override
    protected String getPluralName() {
        return "histories";
    }

    @Override
    protected void addListAttributes(Model model) {
        model.addAttribute("usersById", Lookups.byId(userService.readAll()));
        model.addAttribute("shelvesById", Lookups.byId(shelfService.readAll()));
        model.addAttribute("historyTypesById", Lookups.byId(historyTypeService.readAll()));
    }

    @Override
    protected Predicate<HistoryResponseDTO> filterBy(Map<String, String> params, Model model) {
        Map<Integer, UserResponseDTO> usersById = Lookups.fromModel(model, "usersById");
        return history -> {
            UserResponseDTO user = usersById.get(history.getUserId());
            return Filters.matchesText(params.get("busca"), user == null ? null : user.getUserName())
                    && Filters.sameId(history.getHistoryTypeId(), params.get("tipo"))
                    && Filters.inDateRange(history.getExecutionDate(), params.get("execucaoDe"), params.get("execucaoAte"));
        };
    }
}
