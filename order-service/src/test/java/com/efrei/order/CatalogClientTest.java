package com.efrei.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

/**
 * Teste l'appel HTTP vers catalog-service avec un faux serveur
 * (MockRestServiceServer) qui renvoie des réponses préparées.
 */
@RestClientTest(components = CatalogClient.class, properties = "catalog.url=http://catalog")
class CatalogClientTest {

    @Autowired
    private CatalogClient catalogClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void lireUnProduitExistant() {
        server.expect(requestTo("http://catalog/products/1"))
                .andRespond(withSuccess(
                        "{\"id\":1,\"name\":\"Clavier\",\"price\":49.9,\"stock\":10}",
                        MediaType.APPLICATION_JSON));

        Optional<ProductDto> product = catalogClient.findProduct(1L);

        assertThat(product).isPresent();
        assertThat(product.get().name()).isEqualTo("Clavier");
    }

    @Test
    void produitAbsentDonneUnOptionalVide() {
        server.expect(requestTo("http://catalog/products/99"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(catalogClient.findProduct(99L)).isEmpty();
    }
}
