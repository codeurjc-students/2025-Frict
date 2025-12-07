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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//CLIENT SIDE SYSTEM TESTS
@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8443", // WARNING: Angular must be running with: ng s -c testing
                "app.db.init=users, categories"
        }
)
public class ProductSystemUITest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String BASE_URL = "https://localhost:4202";

    private final List<Product> testProducts = new ArrayList<>();

    @Autowired
    private ProductRepository productRepository;

    @BeforeAll
    public static void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("--headless=new"); // New headless mode for better stability
        options.addArguments("--no-sandbox"); // Required for some CI environments
        options.addArguments("--disable-dev-shm-usage"); // Required for Docker/CI resources

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @BeforeEach
    public void initTest() {
        driver.get(BASE_URL);
        wait.until(ExpectedConditions.urlContains(BASE_URL));
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
        // Expected messages
        String msgFeatured = "No hay productos destacados disponibles.";
        String msgRecommended = "No hay productos recomendados disponibles.";
        String msgTopSales = "No hay productos top ventas disponibles.";

        // Container locators
        By featuredLoc = By.id("featured-content");
        By recommendedLoc = By.id("recommended-content");
        By topSalesLoc = By.id("topSales-content");

        // Waits explicitly until the text is present
        // Synchronizes ngIf divs with Selenium
        wait.until(ExpectedConditions.textToBePresentInElementLocated(featuredLoc, msgFeatured));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(recommendedLoc, msgRecommended));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(topSalesLoc, msgTopSales));

        // Capture the elements
        WebElement featuredMessageDiv = driver.findElement(featuredLoc);
        WebElement recommendedMessageDiv = driver.findElement(recommendedLoc);
        WebElement topSalesMessageDiv = driver.findElement(topSalesLoc);

        // CI debugging logs
        System.out.println("Featured Content: " + featuredMessageDiv.getText());
        System.out.println("Recommended Content: " + recommendedMessageDiv.getText());
        System.out.println("Top Sales Content: " + topSalesMessageDiv.getText());

        // Logic checks (empty carousels)
        int initialFeaturedProducts = driver.findElements(By.cssSelector("[id^='featuredProduct-']")).size();
        int initialRecommendedProducts = driver.findElements(By.cssSelector("[id^='reccomendedProduct-']")).size();
        int initialTopSalesProducts = driver.findElements(By.cssSelector("[id^='topSalesProduct-']")).size();

        assertEquals(0, initialFeaturedProducts, "El número de productos destacados mostrados no es el esperado");
        assertEquals(0, initialRecommendedProducts, "El número de productos recomendados mostrados no es el esperado");
        assertEquals(0, initialTopSalesProducts, "El número de productos top ventas mostrados no es el esperado");

        // Final message check
        assertTrue(featuredMessageDiv.getText().contains(msgFeatured), "Mensaje de destacados incorrecto");
        assertTrue(recommendedMessageDiv.getText().contains(msgRecommended), "Mensaje de recomendados incorrecto");
        assertTrue(topSalesMessageDiv.getText().contains(msgTopSales), "Mensaje de top ventas incorrecto");
    }

}