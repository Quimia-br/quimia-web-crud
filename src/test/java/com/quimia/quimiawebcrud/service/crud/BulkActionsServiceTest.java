package com.quimia.quimiawebcrud.service.crud;

import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.mapper.ProductMapper;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.model.Product;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import com.quimia.quimiawebcrud.repository.CompanyRepository;
import com.quimia.quimiawebcrud.repository.ProductRepository;
import com.quimia.quimiawebcrud.service.CurrentAdmin;
import com.quimia.quimiawebcrud.service.EditLog;
import com.quimia.quimiawebcrud.service.removal.RecordRemovalService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * "Atualizar por..." e "Apagar por..." nos services de cadastro (testados com o de produtos): cada registro
 * alterado entra no log de edições, e cada registro excluído passa pela exclusão normal da tela.
 */
class BulkActionsServiceTest {

    private final CurrentAdmin currentAdmin = mock(CurrentAdmin.class);
    private final AdminLogRepository logs = mock(AdminLogRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final RecordRemovalService removal = mock(RecordRemovalService.class);
    private final ProductService service = new ProductService(products, new ProductMapper(), mock(CompanyRepository.class),
            removal, new EditLog(currentAdmin, logs));

    private final Product acetone = Product.builder().id(1).name("Acetona").brand("Velha").build();
    private final Product ethanol = Product.builder().id(2).name("Etanol").brand("Velha").build();

    @Test
    void updateChangesTheRowsAndLogsEachRecordWithItsPreviousData() {
        when(currentAdmin.require()).thenReturn(Admin.builder().id(3).status(Status.ATIVO).build());
        when(products.findByColumn("marca", "Velha")).thenReturn(List.of(acetone, ethanol));
        when(products.updateByColumn("marca", "Velha", Map.of("marca", "Nova"))).thenReturn(2);

        int updated = service.updateByColumn("marca", "Velha", Map.of("marca", "Nova"));

        assertEquals(2, updated);
        ArgumentCaptor<AdminLog> saved = ArgumentCaptor.forClass(AdminLog.class);
        verify(logs, times(2)).save(saved.capture());
        assertEquals(List.of(1, 2), saved.getAllValues().stream().map(AdminLog::getRecordId).toList());
        for (AdminLog entry : saved.getAllValues()) {
            assertEquals(AdminAction.UPDATE, entry.getAction());
            assertEquals("produtos", entry.getAffectedTable());
            assertTrue(entry.getPreviousData().contains("Marca: Velha"), "o log guarda a marca de antes");
        }
    }

    @Test
    void updateWithNoMatchingRecordChangesAndLogsNothing() {
        when(products.findByColumn(anyString(), any())).thenReturn(List.of());

        assertEquals(0, service.updateByColumn("marca", "Nenhuma", Map.of("marca", "Nova")));

        verify(products, never()).updateByColumn(anyString(), any(), anyMap());
        verify(logs, never()).save(any());
    }

    @Test
    void deleteGoesThroughTheNormalRemovalOfEachRecord() {
        when(products.findByColumn("marca", "Velha")).thenReturn(List.of(acetone, ethanol));
        when(products.findById(1)).thenReturn(Optional.of(acetone));
        when(products.findById(2)).thenReturn(Optional.of(ethanol));

        assertEquals(2, service.deleteByColumn("marca", "Velha"));

        verify(removal).removeProduct(acetone);
        verify(removal).removeProduct(ethanol);
        verify(products, never()).deleteByColumn(anyString(), any());
    }

    @Test
    void deleteStopsAtTheFirstRecordThatCannotBeRemoved() {
        when(products.findByColumn("marca", "Velha")).thenReturn(List.of(acetone, ethanol));
        when(products.findById(1)).thenReturn(Optional.of(acetone));
        doThrow(new InvalidDataException("não pode")).when(removal).removeProduct(acetone);

        assertThrows(InvalidDataException.class, () -> service.deleteByColumn("marca", "Velha"));

        verify(removal, never()).removeProduct(ethanol);
    }
}
