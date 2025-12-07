package com.tfg.backend.e2e;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.model.Product;
import com.tfg.backend.repository.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;

//SERVER SIDE SYSTEM TESTS
@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.db.init=false" // Do not run DatabaseInitializer class in order to make finding created products easier with paginated responses
        }
)
public class ProductSystemApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private ProductRepository productRepository;

    //Sample products
    Product product1 = new Product("Auriculares inalámbricos", "Auriculares con cancelación de ruido y Bluetooth 5.0", 120.0);
    Product product2 = new Product("Monitor 24\" Full HD", "Monitor con panel IPS y colores precisos", 175.0);
    Product product3 = new Product("Teclado mecánico", "Teclado RGB con switches táctiles y anti-ghosting", 95.0);

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
                .queryParam("page", 0) //As default page size is 8, 3 created products will always be in page 0
                .when()
                .get("/")
                .then()
                //.log().all()
                .statusCode(200)
                .contentType(CONTENT_TYPE)
                .body("products.referenceCode", hasItems(product1.getReferenceCode(), product2.getReferenceCode(), product3.getReferenceCode()))
                .body("products.name", hasItems(product1.getName(), product2.getName(), product3.getName()))
                .body("products.description", hasItems(product1.getDescription(), product2.getDescription(), product3.getDescription()))
                .body("products.currentPrice", hasItems((float) product1.getCurrentPrice(), (float) product2.getCurrentPrice(), (float) product3.getCurrentPrice()));
    }

}