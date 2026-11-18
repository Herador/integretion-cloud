package com.efrei.order;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

/**
 * Tests du contrôleur. catalog-service n'est pas démarré :
 * CatalogClient est remplacé par un faux objet (mock) dont on choisit les réponses.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogClient catalogClient;

    @Test
    void infoRenvoieLeNomDuService() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("order-service"));
    }

    @Test
    void commandeValideCalculeLeTotal() throws Exception {
        when(catalogClient.findProduct(1L))
                .thenReturn(Optional.of(new ProductDto(1L, "Clavier", 50.0, 10)));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName").value("Clavier"))
                .andExpect(jsonPath("$.totalPrice").value(100.0));
    }

    @Test
    void produitInconnuRenvoie404() throws Exception {
        when(catalogClient.findProduct(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":99,\"quantity\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void stockInsuffisantRenvoie409() throws Exception {
        when(catalogClient.findProduct(3L))
                .thenReturn(Optional.of(new ProductDto(3L, "Ecran", 199.0, 1)));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":3,\"quantity\":5}"))
                .andExpect(status().isConflict());
    }

    @Test
    void catalogueInjoignableRenvoie503() throws Exception {
        when(catalogClient.findProduct(1L)).thenThrow(new ResourceAccessException("down"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void quantiteInvalideRenvoie400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }
}
