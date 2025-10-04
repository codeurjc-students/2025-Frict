package com.tfg.backend.e2e;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.model.Product;
import com.tfg.backend.repository.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;

//SERVER SIDE SYSTEM TESTS
@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class ProductSystemApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private ProductRepository productRepository;

    //Sample products
    Product product1 = new Product("4I4", "Auriculares inalámbricos", null, "Auriculares con cancelación de ruido y Bluetooth 5.0", 120.0);
    Product product2 = new Product("2G2", "Monitor 24\" Full HD", null, "Monitor con panel IPS y colores precisos", 175.0);
    Product product3 = new Product("1F1", "Teclado mecánico", null, "Teclado RGB con switches táctiles y anti-ghosting", 95.0
    );

    private static final String CONTENT_TYPE = "application/json";

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "https://localhost:" + port;
        RestAssured.basePath = "/api/v1/products";
        RestAssured.useRelaxedHTTPSValidation();

        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);
    }

    @AfterEach
    public void tearDown() {
        productRepository.delete(product1);
        productRepository.delete(product2);
        productRepository.delete(product3);
    }

    @Test
    public void createAndRetrieveProductsTest() {
        given()
                .when()
                .get("/all")
                .then()
                .statusCode(200)
                .contentType(CONTENT_TYPE)
                .body("products.referenceCode", hasItems(product1.getReferenceCode(), product2.getReferenceCode(), product3.getReferenceCode()))
                .body("products.name", hasItems(product1.getName(), product2.getName(), product3.getName()))
                .body("products.description", hasItems(product1.getDescription(), product2.getDescription(), product3.getDescription()))
                .body("products.price", hasItems((float) product1.getPrice(), (float) product2.getPrice(), (float) product3.getPrice()));
    }

}