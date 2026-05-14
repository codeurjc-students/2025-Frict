package com.tfg.backend.api;

import com.tfg.backend.dto.ProductDTO;
import com.tfg.backend.dto.ProductSpecDTO;
import com.tfg.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class ProductApiFunctionalITest extends BaseApiFunctionalITest {

    private static final String BASE_URL_PRODUCTS = "/api/v1/products";

    private Product testProduct;

    private String adminCookie;
    private String userCookie;
    private String managerCookie;

    @BeforeEach
    public void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            Category otrosCategory = new Category("Otros", "icon", "banner", "Desc", "Desc");
            categoryRepository.saveAndFlush(otrosCategory);

            testProduct = new Product("API Test Product", "Desc", 10.0, 5.0);
            testProduct.setReferenceCode("REF-API-123");
            testProduct.setActive(true);
            testProduct = productRepository.saveAndFlush(testProduct);

            Shop testShop = new Shop("API Test Shop", new Address("Shop", "Shop St", "1", "1", "00000", "City", "Country"), 5000.0);
            testShop = shopRepository.saveAndFlush(testShop);

            shopStockRepository.saveAndFlush(new ShopStock(testShop, testProduct, 10));

            User testAdmin = new User("Admin", "admin_api", "admin@api.com", passwordEncoder.encode("pass"), "ADMIN");
            userRepository.saveAndFlush(testAdmin);

            User testUser = new User("User", "user_api", "user@api.com", passwordEncoder.encode("pass"), "USER");
            testUser.setSelectedShop(testShop);
            userRepository.saveAndFlush(testUser);

            User testManager = new User("Manager", "manager_api", "manager@api.com", passwordEncoder.encode("pass"), "MANAGER");
            userRepository.saveAndFlush(testManager);

            testShop.setAssignedManager(testManager);
            shopRepository.saveAndFlush(testShop);
        });

        adminCookie = loginAndGetCookie("admin_api", "pass");
        userCookie = loginAndGetCookie("user_api", "pass");
        managerCookie = loginAndGetCookie("manager_api", "pass");
    }

    @Test
    public void getProduct_StockEnrichment_DependsOnRole() {
        given().spec(getSpec(BASE_URL_PRODUCTS, userCookie)).pathParam("id", testProduct.getId())
                .when().get("/{id}")
                .then().statusCode(200).body("availableUnits", equalTo(10));

        given().spec(getSpec(BASE_URL_PRODUCTS, null)).pathParam("id", testProduct.getId())
                .when().get("/{id}")
                .then().statusCode(200).body("availableUnits", equalTo(0));
    }

    @Test
    public void productVisibilityAspect_HidesInactiveProductsFromUsers() {
        given().spec(getSpec(BASE_URL_PRODUCTS, adminCookie))
                .pathParam("id", testProduct.getId()).queryParam("state", "false")
                .when().put("/active/{id}").then().statusCode(200);

        given().spec(getSpec(BASE_URL_PRODUCTS, null))
                .when().get("/filter")
                .then().statusCode(200).body("items.id", not(hasItem(testProduct.getId().intValue())));

        given().spec(getSpec(BASE_URL_PRODUCTS, adminCookie))
                .when().get("/")
                .then().statusCode(200).body("items.id", hasItem(testProduct.getId().intValue()));
    }

    @Test
    public void updateProductImages_Admin_UploadsMultipartToMinio() {
        given().spec(getSpec(BASE_URL_PRODUCTS, adminCookie))
                .contentType("multipart/form-data")
                .pathParam("id", testProduct.getId())
                .multiPart("existingImages", "[]", "application/json")
                .multiPart("newImages", "foto_test.jpg", "fake_image_content".getBytes(), "image/jpeg")
                .when().put("/{id}/images")
                .then().statusCode(200).body("imagesInfo.size()", greaterThan(0));
    }

    @Test
    public void getAllProducts_Admin_ReturnsPagedProducts() {
        given().spec(getSpec(BASE_URL_PRODUCTS, adminCookie))
                .when().get("/")
                .then().statusCode(200).body("items", notNullValue());
    }

    @Test
    public void createProduct_Admin_CreatesAndReturnsProduct() {
        ProductDTO newProduct = new ProductDTO();
        newProduct.setName("New Admin Product");
        newProduct.setReferenceCode("NEW-REF-001");
        newProduct.setSupplyPrice(15.0);
        newProduct.setCurrentPrice(25.0);
        newProduct.setActive(true);

        given().spec(getSpec(BASE_URL_PRODUCTS, adminCookie))
                .body(newProduct)
                .when().post()
                .then().statusCode(201).body("name", equalTo("New Admin Product"));
    }

    @Test
    public void updateProduct_Admin_UpdatesProduct() {
        ProductDTO updateData = new ProductDTO();
        updateData.setName("Updated Product Name");
        updateData.setReferenceCode("NEW-REF");
        updateData.setActive(true);

        given().spec(getSpec(BASE_URL_PRODUCTS, adminCookie))
                .pathParam("id", testProduct.getId()).body(updateData)
                .when().put("/{id}")
                .then().statusCode(200).body("name", equalTo("Updated Product Name"));
    }

    @Test
    public void deleteProduct_Admin_DeletesProduct() {
        given().spec(getSpec(BASE_URL_PRODUCTS, adminCookie))
                .pathParam("id", testProduct.getId())
                .when().delete("/{id}")
                .then().statusCode(200);
    }

    @Test
    public void getSpecsCatalog_ReturnsMapObject() {
        given().spec(getSpec(BASE_URL_PRODUCTS, null))
                .when().get("/specs")
                .then().statusCode(200).body("$", notNullValue());
    }

    @Test
    public void createProduct_WithSpecs_ReturnsSpecsInResponse() {
        ProductDTO newProduct = new ProductDTO();
        newProduct.setName("Product With Specs");
        newProduct.setSupplyPrice(10.0);
        newProduct.setCurrentPrice(20.0);
        newProduct.setActive(true);
        newProduct.setSpecifications(List.of(new ProductSpecDTO("Color", List.of("Rojo", "Azul"))));

        given().spec(getSpec(BASE_URL_PRODUCTS, adminCookie))
                .body(newProduct)
                .when().post()
                .then().statusCode(201)
                .body("specifications", hasSize(1))
                .body("specifications[0].name", equalTo("Color"))
                .body("specifications[0].values", hasItems("Rojo", "Azul"));
    }

    @Test
    public void getFilteredProducts_BySpecFilter_ReturnsMatchingProduct() {
        ProductDTO specProduct = new ProductDTO();
        specProduct.setName("Spec Filter Test");
        specProduct.setSupplyPrice(10.0);
        specProduct.setCurrentPrice(20.0);
        specProduct.setActive(true);
        specProduct.setSpecifications(List.of(new ProductSpecDTO("Material", List.of("Madera"))));

        given().spec(getSpec(BASE_URL_PRODUCTS, adminCookie))
                .body(specProduct)
                .when().post()
                .then().statusCode(201);

        given().spec(getSpec(BASE_URL_PRODUCTS, null))
                .queryParam("specFilter", "Material:Madera")
                .when().get("/filter")
                .then().statusCode(200)
                .body("items.name", hasItem("Spec Filter Test"));
    }
}