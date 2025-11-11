package com.tfg.backend.e2e;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.model.Category;
import com.tfg.backend.model.Product;
import com.tfg.backend.repository.CategoryRepository;
import com.tfg.backend.repository.ProductRepository;
import org.junit.jupiter.api.*;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

//CLIENT SIDE SYSTEM TESTS
@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
        "app.db.init=false", // Deactivate sample data initialization only for this test (DatabaseInitializer class will not run)
        "server.port=8443"
        }
)
public class ProductSystemUITest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String BASE_URL = "https://localhost:4202";

    private final List<Product> testProducts = new ArrayList<>();

    @Autowired
    private ProductRepository productRepository;

    /*

    @Autowired
    private CategoryRepository categoryRepository;

     */

    @BeforeAll
    public static void setUp()  {
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("--headless=new");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @BeforeEach
    public void initTest() {
        driver.get(BASE_URL);
        wait.until(ExpectedConditions.urlContains(BASE_URL));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("content"))); //Loading component div or carousels div
    }

    @AfterEach
    public void cleanTestEnv() {
        testProducts.clear();
        productRepository.deleteAll();
    }

    @AfterAll
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void displayNoProductsTest() {
        int initialFeaturedProducts = driver.findElements(By.cssSelector("[id^='featuredProduct-']")).size();
        int initialRecommendedProducts = driver.findElements(By.cssSelector("[id^='reccomendedProduct-']")).size();
        int initialTopSalesProducts = driver.findElements(By.cssSelector("[id^='topSalesProduct-']")).size();

        assertEquals(0, initialFeaturedProducts, "El número de productos destacados mostrados no es el esperado");
        assertEquals(0, initialRecommendedProducts, "El número de productos recomendados mostrados no es el esperado");
        assertEquals(0, initialTopSalesProducts, "El número de productos top ventas mostrados no es el esperado");

        WebElement featuredMessageDiv = driver.findElement(By.id("featured-content"));
        WebElement recommendedMessageDiv = driver.findElement(By.id("recommended-content"));
        WebElement topSalesMessageDiv = driver.findElement(By.id("topSales-content"));

        assertTrue(featuredMessageDiv.getText().contains("No hay productos destacados disponibles."));
        assertTrue(recommendedMessageDiv.getText().contains("No hay productos recomendados disponibles."));
        assertTrue(topSalesMessageDiv.getText().contains("No hay productos top ventas disponibles."));
    }

    /*

    @Test
    public void displayCreatedProductsTest() { //Example with a featured product
        Category featured = categoryRepository.save(new Category("Destacado", null));
        Product product = new Product("5A6", "Disco duro", null, "Almacenamiento sin límites", 75.49);
        product.getCategories().add(featured);
        testProducts.add(product);
        productRepository.saveAll(testProducts);
        checkSampleFeaturedProductsAreDisplayed();
    }

    @Test
    public void displayChangesInUpdatedProductsTest() {
        Category featured = categoryRepository.save(new Category("Destacado", null));
        Product product = new Product("5A6", "Disco duro", null, "Almacenamiento sin límites", 75.49);
        product.getCategories().add(featured);
        testProducts.add(product);
        List<Product> savedProducts = productRepository.saveAll(testProducts);
        checkSampleFeaturedProductsAreDisplayed();

        Product updatingProduct = savedProducts.getFirst();
        String newName = "Ratón inalámbrico";
        double newCurrentPrice = 24.95;

        updatingProduct.setName(newName);
        updatingProduct.setCurrentPrice(newCurrentPrice);
        productRepository.save(updatingProduct); //Contains the id where the product has been saved previously

        driver.get(BASE_URL);
        wait.until(ExpectedConditions.urlContains(BASE_URL));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("content")));
        List<WebElement> productDivs = driver.findElements(By.cssSelector("[id^='featuredProduct-']"));

        assertEquals(1, productDivs.size());
        assertTrue(productDivs.getFirst().getText().contains(updatingProduct.getReferenceCode()));
        assertTrue(productDivs.getFirst().getText().contains(newName));
        assertTrue(productDivs.getFirst().getText().contains(updatingProduct.getDescription()));
        assertTrue(productDivs.getFirst().getText().contains(String.valueOf(newCurrentPrice)));
    }

    @Test
    public void retireDeletedProductsTest() {
        Category featured = categoryRepository.save(new Category("Destacado", null));
        Product product = new Product("5A6", "Disco duro", null, "Almacenamiento sin límites", 75.49);
        product.getCategories().add(featured);
        testProducts.add(product);
        List<Product> savedProducts = productRepository.saveAll(testProducts);
        checkSampleFeaturedProductsAreDisplayed();

        Product deletingProduct = savedProducts.getFirst();
        productRepository.delete(deletingProduct); //The saved products include their id

        driver.get(BASE_URL);
        wait.until(ExpectedConditions.urlContains(BASE_URL));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("content")));
        List<WebElement> productDivs = driver.findElements(By.cssSelector("[id^='featuredProduct-']"));

        assertEquals(testProducts.size()-1, productDivs.size());
        for (int i = 0; i < productDivs.size(); i++) {
            assertFalse(productDivs.get(i).getText().contains(savedProducts.getFirst().getReferenceCode()));
            assertFalse(productDivs.get(i).getText().contains(savedProducts.getFirst().getName()));
            assertFalse(productDivs.get(i).getText().contains(savedProducts.getFirst().getDescription()));
            assertFalse(productDivs.get(i).getText().contains(String.valueOf(savedProducts.getFirst().getCurrentPrice())));
        }
    }

    private void checkSampleFeaturedProductsAreDisplayed(){
        driver.navigate().refresh();
        wait.until(ExpectedConditions.urlContains(BASE_URL));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("featured-content")));
        List<WebElement> productDiv = driver.findElements(By.cssSelector("[id^='featuredProduct-']"));

        assertEquals(testProducts.size(), productDiv.size());
        for (int i = 0; i < productDiv.size(); i++) {
            assertTrue(productDiv.get(i).getText().contains(testProducts.get(i).getReferenceCode()));
            assertTrue(productDiv.get(i).getText().contains(testProducts.get(i).getName()));
            assertTrue(productDiv.get(i).getText().contains(testProducts.get(i).getDescription()));
            assertTrue(productDiv.get(i).getText().contains(String.valueOf(testProducts.get(i).getCurrentPrice())));
        }
    }

     */
}
