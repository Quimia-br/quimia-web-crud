package com.quimia.quimiawebcrud.controller.crud;

import com.quimia.quimiawebcrud.dto.response.HistoryResponseDTO;
import com.quimia.quimiawebcrud.service.crud.HistoryService;
import com.quimia.quimiawebcrud.service.crud.HistoryTypeService;
import com.quimia.quimiawebcrud.service.crud.ShelfService;
import com.quimia.quimiawebcrud.service.crud.UserService;
import com.quimia.quimiawebcrud.service.photo.PhotoUrlPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * No banco a estante, a data e a descrição de um histórico são opcionais: a tela não pode quebrar por isso.
 */
@WebMvcTest(HistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PhotoUrlPolicy.class)
class HistoryWithMissingDataTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    HistoryService historyService;
    @MockitoBean
    UserService userService;
    @MockitoBean
    ShelfService shelfService;
    @MockitoBean
    HistoryTypeService historyTypeService;

    @Test
    void historyWithoutShelfDateOrDescriptionStillOpens() throws Exception {
        List<HistoryResponseDTO> histories = new ArrayList<>();
        histories.add(HistoryResponseDTO.builder().id(1).userId(99).historyTypeId(98).build());
        when(historyService.readAll()).thenReturn(histories);
        when(userService.readAll()).thenReturn(new ArrayList<>());
        when(shelfService.readAll()).thenReturn(new ArrayList<>());
        when(historyTypeService.readAll()).thenReturn(new ArrayList<>());

        String html = mvc.perform(get("/histories")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("ver-history-1"), "a linha do histórico aparece");
    }

    @Test
    void historyShowsProductNames() throws Exception {
        List<HistoryResponseDTO> histories = new ArrayList<>();
        histories.add(HistoryResponseDTO.builder().id(1).userId(99).historyTypeId(98)
                .productNames(List.of("Acetona", "Etanol")).build());
        when(historyService.readAll()).thenReturn(histories);
        when(userService.readAll()).thenReturn(new ArrayList<>());
        when(shelfService.readAll()).thenReturn(new ArrayList<>());
        when(historyTypeService.readAll()).thenReturn(new ArrayList<>());

        String html = mvc.perform(get("/histories")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("Acetona, Etanol"), "os nomes dos produtos aparecem");
    }
}
