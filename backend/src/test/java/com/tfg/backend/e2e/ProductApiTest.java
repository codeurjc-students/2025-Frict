package com.tfg.backend.e2e;

import com.tfg.backend.BackendApplication;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

//SERVER SIDE SYSTEM TESTS
@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class ProductApiTest {

    @LocalServerPort
    int port;

    private static final String CONTENT_TYPE = "application/json";

    // Sample product data
    private String referenceCode = "4A5";
    private String name = "Aspiradora inteligente";
    private String description = "Limpieza sin límites";
    private double price = 130.99;

    @BeforeEach
    public void setUp() {
        referenceCode = "RC-" + System.currentTimeMillis();
        RestAssured.baseURI = "https://localhost:" + port;
        RestAssured.basePath = "/api/products";
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    public void createProductTest() {
        Response postResponse = createProduct();
        postResponse.then().statusCode(201);

        Response getResponse = getProduct(postResponse.jsonPath().getString("id"));
        getResponse.then()
                .statusCode(200)
                .body("referenceCode", equalTo(referenceCode))
                .body("name", equalTo(name))
                .body("description", equalTo(description))
                .body("price", equalTo((float) price));
    }

    @Test
    public void getInexistentProductTest() {
        String wrongId = "-2";
        Response getResponse = getProduct(wrongId);
        getResponse.then().statusCode(404);
    }

    @Test
    public void updateProductTest() {
        //As all test runs under the same DB, previous tests may have already created the product with reference code 4A5, so this data needs to be changed in order to be unique
        referenceCode = "6A7";
        Response postResponse = createProduct(); //Now there will be 2 products with the same name, description and price, but not with the same reference code

        description = "Hasta 30 horas de reproducción de vídeo";
        Response putResponse = updateProduct(postResponse.jsonPath().getString("id"));
        putResponse.then().statusCode(202).body("description", equalTo(description));

        Response getResponse = getProduct(postResponse.jsonPath().getString("id"));
        getResponse.then()
                .statusCode(200)
                .body("referenceCode", equalTo(referenceCode))
                .body("name", equalTo(name))
                .body("description", equalTo(description))
                .body("price", equalTo((float) price));
    }

    @Test
    public void deleteProductTest() {
        referenceCode = "7A8";
        Response postResponse = createProduct();

        Response deleteResponse = deleteProduct(postResponse.jsonPath().getString("id"));
        deleteResponse.then().statusCode(200); //The operation was successful

        Response getResponse = getProduct(postResponse.jsonPath().getString("id"));
        getResponse.then().statusCode(404); //And the product doesnt exist
    }


    private Response createProduct() {
        ProductRequest product = new ProductRequest(referenceCode, name, description, price);
        return given()
                .contentType(CONTENT_TYPE)
                .body(product) //Serialized with Jackson
                .when()
                .post();
    }

    private Response updateProduct(String productId) {
        ProductRequest product = new ProductRequest(referenceCode, name, description, price);
        return given()
                .contentType(CONTENT_TYPE)
                .pathParam("id", productId)
                .body(product)
                .when()
                .put("/{id}");
    }

    private Response getProduct(String productId) {
        return given()
                .pathParam("id", productId)
                .when()
                .get("/{id}");
    }

    private Response deleteProduct(String productId) {
        return given()
                .pathParam("id", productId)
                .when()
                .delete("/{id}");
    }

    //DTO class to make the JSON request
    private static class ProductRequest {
        public String referenceCode;
        public String name;
        public String description;
        public double price;

        public ProductRequest(String referenceCode, String name, String description, double price) {
            this.referenceCode = referenceCode;
            this.name = name;
            this.description = description;
            this.price = price;
        }
    }
}
