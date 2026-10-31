package com.quimia.quimiawebcrud.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;

/**
 * Trata em um só lugar as exceções que chegam aos controllers e mostra a página "error".
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_VIEW = "error";

    /** Registro inexistente: 404 com a mensagem da própria exceção. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** Registro repetido (e-mail, CNPJ, CAS...): 409 com a mensagem da exceção. */
    @ExceptionHandler(DuplicateResourceException.class)
    public ModelAndView handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** Dado que quebra uma regra de negócio: 400 com a mensagem da exceção. */
    @ExceptionHandler(InvalidDataException.class)
    public ModelAndView handleInvalidData(InvalidDataException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Demais regras de negócio (ex.: foto recusada) que chegam aqui sem um tratamento próprio.
     */
    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusiness(BusinessException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Arquivo acima do limite de upload do servidor: o pedido é recusado antes de chegar ao controller.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve ter no máximo 2 MB.", request);
    }

    /** Parâmetro de URL com tipo errado (ex.: id que não é número): 400 com mensagem genérica. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "O valor informado na URL é inválido.", request);
    }

    /** O banco recusou a operação (registro em uso, valor repetido): 409 com mensagem amigável. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violação de integridade em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "A operação não pôde ser concluída: o registro está em uso por outros dados ou já existe.", request);
    }

    /** Os números do dashboard não puderam ser lidos: 503 com o motivo. */
    @ExceptionHandler(DashboardUnavailableException.class)
    public ModelAndView handleDashboardUnavailable(DashboardUnavailableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    /**
     * Banco fora do ar, sem conexão livre no pool, consulta que passou do tempo ou travada por outra: não é culpa do
     * pedido e costuma passar sozinha, então o aviso pede para tentar de novo em vez de mostrar um erro genérico.
     */
    @ExceptionHandler({DataAccessResourceFailureException.class, TransientDataAccessException.class,
            CannotCreateTransactionException.class})
    public ModelAndView handleDatabaseUnavailable(Exception ex, HttpServletRequest request) {
        log.error("Banco indisponível em {}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "O banco de dados está indisponível ou ocupado no momento. Tente novamente em instantes.", request);
    }

    /**
     * Último recurso: erros inesperados são registrados no log e mostram uma mensagem genérica.
     * Erros HTTP do próprio Spring (ex.: página inexistente) mantêm o status original.
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            String message = status == HttpStatus.NOT_FOUND
                    ? "A página solicitada não foi encontrada."
                    : "Não foi possível processar a requisição.";
            return buildResponse(status, message, request);
        }
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente mais tarde.", request);
    }

    private ModelAndView buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        ErrorDetails details = new ErrorDetails(status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), LocalDateTime.now());
        ModelAndView modelAndView = new ModelAndView(ERROR_VIEW);
        modelAndView.setStatus(status);
        modelAndView.addObject("details", details);
        return modelAndView;
    }
}
