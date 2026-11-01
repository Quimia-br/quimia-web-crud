package com.quimia.quimiawebcrud.controller.support;

import com.quimia.quimiawebcrud.exception.BusinessException;
import com.quimia.quimiawebcrud.exception.ResourceNotFoundException;
import com.quimia.quimiawebcrud.model.BaseEntity;
import com.quimia.quimiawebcrud.service.CrudService;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Telas de cadastro: além da lista, têm as rotas de salvar e excluir usadas pelos popups.
 * Erros de regra de negócio viram aviso na própria tela; erros do banco ganham uma mensagem amigável;
 * qualquer outro erro sobe para o tratador global (página de erro).
 */
@Slf4j
public abstract class GenericController<E extends BaseEntity, ID, Req, Res> extends ListController<Res> {

    private static final int MAX_MESSAGES = 3;
    /** Mesmos limites dos campos dos popups "Atualizar por..." e "Apagar por...". */
    private static final int MAX_FILTER_LENGTH = 255;
    private static final int MAX_NEW_VALUE_LENGTH = 1000;

    protected abstract CrudService<E, ID, Req, Res> getService();

    @Override
    protected List<Res> readAll() {
        return getService().readAll();
    }

    @PostMapping("/save")
    public String save(@RequestParam(name = "id", required = false) ID id,
                       @Valid @ModelAttribute Req request,
                       BindingResult validation,
                       RedirectAttributes redirect) {
        if (validation.hasErrors()) {
            redirect.addFlashAttribute("error", describe(validation));
            return redirectToList();
        }
        return attempt(redirect, "salvar",
                "Não foi possível salvar: já existe um registro com esses dados ou algum dado relacionado é inválido.",
                () -> {
                    getService().verifyBeforeSave(request, id);
                    if (id == null) {
                        getService().create(request);
                        return "Registro criado com sucesso.";
                    }
                    getService().update(id, request);
                    return "Registro atualizado com sucesso.";
                });
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") ID id, RedirectAttributes redirect) {
        return attempt(redirect, "excluir",
                "Não foi possível excluir: este registro está em uso por outros dados.",
                () -> {
                    getService().delete(id);
                    return "Registro excluído com sucesso.";
                });
    }

    /**
     * Cada tela de cadastro diz quais colunas oferece nos popups "Atualizar por..." e "Apagar por...".
     */
    @Override
    protected abstract List<ColumnField> getColumnFields();

    /** Popup "Atualizar por...": altera uma coluna em todos os registros que têm o valor filtrado. */
    @PostMapping("/update-by")
    public String updateBy(@RequestParam("filterColumn") String filterColumn,
                           @RequestParam(name = "filterValue", defaultValue = "") String filterValue,
                           @RequestParam("changeColumn") String changeColumn,
                           @RequestParam(name = "newValue", defaultValue = "") String newValue,
                           RedirectAttributes redirect) {
        if (!offers(filterColumn, false) || !offers(changeColumn, true)) {
            redirect.addFlashAttribute("error", "Escolha uma das colunas da lista.");
            return redirectToList();
        }
        if (filterValue.length() > MAX_FILTER_LENGTH || newValue.length() > MAX_NEW_VALUE_LENGTH) {
            redirect.addFlashAttribute("error", "O valor informado é muito longo.");
            return redirectToList();
        }
        return attempt(redirect, "atualizar por coluna",
                "Não foi possível atualizar: o novo valor não é aceito (tamanho, tipo ou registro relacionado inexistente).",
                () -> affectedMessage(getService().updateByColumn(filterColumn, filterValue, Map.of(changeColumn, newValue)),
                        "atualizado"));
    }

    /** Popup "Apagar por...": exclui todos os registros que têm o valor filtrado na coluna escolhida. */
    @PostMapping("/delete-by")
    public String deleteBy(@RequestParam("filterColumn") String filterColumn,
                           @RequestParam(name = "filterValue", defaultValue = "") String filterValue,
                           RedirectAttributes redirect) {
        if (!offers(filterColumn, false)) {
            redirect.addFlashAttribute("error", "Escolha uma das colunas da lista.");
            return redirectToList();
        }
        if (filterValue.length() > MAX_FILTER_LENGTH) {
            redirect.addFlashAttribute("error", "O valor informado é muito longo.");
            return redirectToList();
        }
        return attempt(redirect, "excluir por coluna",
                "Não foi possível excluir: algum dos registros está em uso por outros dados.",
                () -> affectedMessage(getService().deleteByColumn(filterColumn, filterValue), "excluído"));
    }

    /** Só vale uma coluna que a tela oferece (e, para alterar, uma que aceita novo valor): o nome vai para o SQL. */
    private boolean offers(String column, boolean mustBeUpdatable) {
        return getColumnFields().stream()
                .anyMatch(field -> field.getColumn().equals(column) && (!mustBeUpdatable || field.isUpdatable()));
    }

    /**
     * Roda a ação e guarda o resultado como aviso da próxima tela: a mensagem de sucesso que a ação devolve, o texto
     * da regra de negócio que ela quebrou ou, se o banco recusar, a mensagem amigável recebida.
     */
    private String attempt(RedirectAttributes redirect, String what, String integrityMessage, Supplier<String> action) {
        try {
            redirect.addFlashAttribute("message", action.get());
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.warn("Violação de integridade ao {} em {}", what, getBasePath(), e);
            redirect.addFlashAttribute("error", integrityMessage);
        }
        return redirectToList();
    }

    /** "3 registros excluídos com sucesso."; zero registros não é sucesso, vira o aviso de que nada foi encontrado. */
    private static String affectedMessage(int records, String action) {
        if (records == 0) {
            throw new ResourceNotFoundException("Nenhum registro encontrado com esse valor.");
        }
        return records == 1 ? "1 registro " + action + " com sucesso." : records + " registros " + action + "s com sucesso.";
    }

    /**
     * Junta as mensagens de validação (até {@value #MAX_MESSAGES}) em um único aviso para a tela.
     */
    private String describe(BindingResult validation) {
        Set<String> messages = new LinkedHashSet<>();
        for (ObjectError error : validation.getAllErrors()) {
            if (error instanceof FieldError field && "typeMismatch".equals(field.getCode())) {
                messages.add("O valor informado no campo \"" + field.getField() + "\" é inválido.");
            } else {
                messages.add(error.getDefaultMessage());
            }
        }
        return messages.stream().limit(MAX_MESSAGES).collect(Collectors.joining(" "));
    }

    /** Volta para a lista da tela; o aviso de sucesso ou erro segue como flash attribute. */
    protected String redirectToList() {
        return "redirect:" + getBasePath();
    }
}
