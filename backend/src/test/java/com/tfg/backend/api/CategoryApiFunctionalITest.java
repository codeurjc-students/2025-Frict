package com.tfg.backend.api;

import com.tfg.backend.dto.CategoryDTO;
import com.tfg.backend.model.Category;
import com.tfg.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CategoryApiFunctionalITest extends BaseApiFunctionalITest {

    private static final String BASE_URL_CATEGORIES = "/api/v1/categories";

    private Category otrosCategory;
    private String adminCookie;

    @BeforeEach
    public void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            otrosCategory = new Category("Otros", "icon", "banner", "Default", "Default");
            otrosCategory = categoryRepository.saveAndFlush(otrosCategory);

            User testAdmin = new User("Admin", "admin_cat", "admin@cat.com", passwordEncoder.encode("pass"), "ADMIN");
            userRepository.saveAndFlush(testAdmin);
        });

        adminCookie = loginAndGetCookie("admin_cat", "pass");
    }

    @Test
    public void getAllCategories_ReturnsPagedAndListFormats() {
        given().spec(getSpec(BASE_URL_CATEGORIES, null))
                .when().get("/")
                .then().statusCode(200).body("items", notNullValue());

        given().spec(getSpec(BASE_URL_CATEGORIES, null))
                .when().get("/list")
                .then().statusCode(200).body("size()", greaterThanOrEqualTo(1)).body("name", hasItem("Otros"));
    }

    @Test
    public void uploadCategoryImage_Admin_UploadsMultipart() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Image Category");
        Long catId = given().spec(getSpec(BASE_URL_CATEGORIES, adminCookie)).body(dto).when().post().jsonPath().getLong("id");

        given().spec(getSpec(BASE_URL_CATEGORIES, adminCookie)).contentType("multipart/form-data")
                .pathParam("id", catId).multiPart("image", "cat.jpg", "fake_image".getBytes(), "image/jpeg")
                .when().post("/{id}/image")
                .then().statusCode(200).body("imageInfo.imageUrl", notNullValue());
    }

    @Test
    public void deleteCategoryImage_Admin_RemovesImage() {
        given().spec(getSpec(BASE_URL_CATEGORIES, adminCookie))
                .pathParam("id", otrosCategory.getId())
                .when().delete("/{id}/image")
                .then().statusCode(200).body("imageInfo", notNullValue());
    }

    @Test
    public void categoryForbiddenOperations_ProtectOtrosCategory() {
        Long realOtrosId = given().spec(getSpec(BASE_URL_CATEGORIES, null))
                .pathParam("name", "Otros")
                .when().get("/name/{name}")
                .then().statusCode(200).extract().jsonPath().getLong("id");

        CategoryDTO updateDto = new CategoryDTO();
        updateDto.setName("Illegal Change");

        given().spec(getSpec(BASE_URL_CATEGORIES, adminCookie))
                .pathParam("id", realOtrosId).body(updateDto)
                .when().put("/{id}").then().statusCode(403);

        given().spec(getSpec(BASE_URL_CATEGORIES, adminCookie))
                .pathParam("id", realOtrosId)
                .when().delete("/{id}").then().statusCode(403);
    }

    @Test
    public void circularReference_ForbiddenTest() {
        CategoryDTO parentDto = new CategoryDTO();
        parentDto.setName("Parent A");
        Long parentId = given().spec(getSpec(BASE_URL_CATEGORIES, adminCookie)).body(parentDto).when().post().jsonPath().getLong("id");

        CategoryDTO childDto = new CategoryDTO();
        childDto.setName("Child B");
        childDto.setParentId(parentId);
        Long childId = given().spec(getSpec(BASE_URL_CATEGORIES, adminCookie)).body(childDto).when().post().jsonPath().getLong("id");

        parentDto.setId(parentId);
        parentDto.setParentId(childId);

        given().spec(getSpec(BASE_URL_CATEGORIES, adminCookie))
                .pathParam("id", parentId).body(parentDto)
                .when().put("/{id}").then().statusCode(403);
    }

    @Test
    public void createSubCategory_LinksCorrectParent() {
        CategoryDTO childDto = new CategoryDTO();
        childDto.setName("Subcategory");
        childDto.setParentId(otrosCategory.getId());

        given().spec(getSpec(BASE_URL_CATEGORIES, adminCookie))
                .body(childDto)
                .when().post()
                .then().statusCode(201).body("parentId", equalTo(otrosCategory.getId().intValue()));
    }
}