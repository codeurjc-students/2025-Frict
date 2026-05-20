package com.tfg.backend.api;

import com.tfg.backend.dto.ReviewDTO;
import com.tfg.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class ReviewApiFunctionalITest extends BaseApiFunctionalITest {

    private static final String BASE_URL_REVIEWS = "/api/v1/reviews";

    private User reviewOwner;
    private User otherUser;
    private Product testProduct;
    private Review testReview;

    private String adminCookie;
    private String ownerCookie;
    private String otherUserCookie;

    @BeforeEach
    public void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            Category otrosCategory = new Category("Otros", "icon", "banner", "Desc", "Desc");
            categoryRepository.saveAndFlush(otrosCategory);

            testProduct = new Product("API Review Product", "Desc", 10.0, 5.0);
            testProduct.setReferenceCode("REF-REV-123");
            testProduct.setActive(true);
            testProduct = productRepository.saveAndFlush(testProduct);

            Shop testShop = new Shop();
            testShop.setName("Functional Shop");
            testShop.setReferenceCode("SH-FUNC");
            testShop = shopRepository.saveAndFlush(testShop);

            User testAdmin = new User("Admin", "admin_rev", "admin@rev.com", passwordEncoder.encode("pass"), "ADMIN");
            testAdmin.setSelectedShop(testShop);
            userRepository.saveAndFlush(testAdmin);

            reviewOwner = new User("Owner", "owner_rev", "owner@rev.com", passwordEncoder.encode("pass"), "USER");
            reviewOwner.setSelectedShop(testShop);
            userRepository.saveAndFlush(reviewOwner);

            otherUser = new User("Other", "other_rev", "other@rev.com", passwordEncoder.encode("pass"), "USER");
            otherUser.setSelectedShop(testShop);
            userRepository.saveAndFlush(otherUser);

            testReview = new Review(reviewOwner, testProduct, 5, "Excellent product!", true);
            reviewRepository.saveAndFlush(testReview);
        });

        adminCookie = loginAndGetCookie("admin_rev", "pass");
        ownerCookie = loginAndGetCookie("owner_rev", "pass");
        otherUserCookie = loginAndGetCookie("other_rev", "pass");
    }

    @Test
    public void getAllReviewsByProductId_ReturnsList() {
        given().spec(getSpec(BASE_URL_REVIEWS, otherUserCookie))
                .queryParam("productId", testProduct.getId())
                .when().get("/product")
                .then().statusCode(200).body("items[0].text", equalTo("Excellent product!"));
    }

    @Test
    public void getLoggedUserReviews_ReturnsPagedReviews() {
        given().spec(getSpec(BASE_URL_REVIEWS, ownerCookie))
                .when().get()
                .then().statusCode(200).body("items[0].creatorId", equalTo(reviewOwner.getId().intValue()));
    }

    @Test
    public void createReview_ValidUser_CreatesReview() {
        ReviewDTO newReview = new ReviewDTO();
        newReview.setProductId(testProduct.getId());
        newReview.setCreatorId(otherUser.getId());
        newReview.setRating(4);
        newReview.setText("Very good!");
        newReview.setRecommended(true);

        given().spec(getSpec(BASE_URL_REVIEWS, otherUserCookie))
                .body(newReview).when().post()
                .then().statusCode(201).body("text", equalTo("Very good!"));
    }

    @Test
    public void createReview_MismatchCreatorId_ThrowsUnauthorized() {
        ReviewDTO spoofedReview = new ReviewDTO();
        spoofedReview.setProductId(testProduct.getId());
        spoofedReview.setCreatorId(reviewOwner.getId());
        spoofedReview.setRating(1);

        given().spec(getSpec(BASE_URL_REVIEWS, otherUserCookie))
                .body(spoofedReview).when().post()
                .then().statusCode(401);
    }

    @Test
    public void updateReview_AsCreator_UpdatesReview() {
        ReviewDTO updateDto = new ReviewDTO();
        updateDto.setId(testReview.getId());
        updateDto.setRating(3);
        updateDto.setText("Actually, it is average.");
        updateDto.setRecommended(false);

        given().spec(getSpec(BASE_URL_REVIEWS, ownerCookie))
                .body(updateDto).when().put()
                .then().statusCode(200).body("rating", equalTo(3));
    }

    @Test
    public void updateReview_AsOtherUser_ThrowsUnauthorized() {
        ReviewDTO updateDto = new ReviewDTO();
        updateDto.setId(testReview.getId());
        updateDto.setRating(1);

        given().spec(getSpec(BASE_URL_REVIEWS, otherUserCookie))
                .body(updateDto).when().put()
                .then().statusCode(401);
    }

    @Test
    public void deleteReview_AsCreator_DeletesReview() {
        given().spec(getSpec(BASE_URL_REVIEWS, ownerCookie))
                .pathParam("id", testReview.getId())
                .when().delete("/{id}").then().statusCode(200);
    }

    @Test
    public void deleteReview_AsAdmin_DeletesReview() {
        given().spec(getSpec(BASE_URL_REVIEWS, adminCookie))
                .pathParam("id", testReview.getId())
                .when().delete("/{id}").then().statusCode(200);
    }

    @Test
    public void deleteReview_AsOtherUser_ThrowsUnauthorized() {
        given().spec(getSpec(BASE_URL_REVIEWS, otherUserCookie))
                .pathParam("id", testReview.getId())
                .when().delete("/{id}").then().statusCode(401);
    }
}