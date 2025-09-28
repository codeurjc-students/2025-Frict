package com.tfg.backend.e2e;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.model.Product;
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
        "app.db.init=false" // Deactivate sample data initialization only for this test (DatabaseInitializer class will not run)
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
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.m-4"))); //No elements div or list div
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
        int initialProducts = driver.findElements(By.className("product")).size();
        assertEquals(0, initialProducts, "El número de productos mostrados no es el esperado");
        WebElement messageDiv = driver.findElement(By.className("noProducts"));
        assertTrue(messageDiv.getText().contains("No hay productos disponibles para mostrar."));
    }

    @Test
    public void displayCreatedProductsTest() {
        testProducts.add(new Product("5A6", "Disco duro", null, "Almacenamiento sin límites", 75.49));
        testProducts.add(new Product("7B8", "Monitor LED", null, "Pantalla de alta definición", 120.99));
        productRepository.saveAll(testProducts);
        checkSampleProductsAreDisplayed();
    }

    @Test
    public void displayChangesInUpdatedProductsTest() {
        testProducts.add(new Product("9C2", "Teclado mecánico", null, "Con retroiluminación RGB", 45.99));
        List<Product> savedProducts = productRepository.saveAll(testProducts);
        checkSampleProductsAreDisplayed();

        Product updatingProduct = savedProducts.getFirst();
        String newName = "Ratón inalámbrico";
        double newPrice = 24.95;

        updatingProduct.setName(newName);
        updatingProduct.setPrice(newPrice);
        productRepository.save(updatingProduct); //Contains the id where the product has been saved previously

        driver.get(BASE_URL);
        wait.until(ExpectedConditions.urlContains(BASE_URL));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("product")));
        List<WebElement> productDivs = driver.findElements(By.className("product"));

        assertEquals(1, productDivs.size());
        assertTrue(productDivs.getFirst().getText().contains(updatingProduct.getReferenceCode()));
        assertTrue(productDivs.getFirst().getText().contains(newName));
        assertTrue(productDivs.getFirst().getText().contains(updatingProduct.getDescription()));
        assertTrue(productDivs.getFirst().getText().contains(String.valueOf(newPrice)));
    }

    @Test
    public void retireDeletedProductsTest() {
        testProducts.add(new Product("4D7", "Altavoz Bluetooth", null, "Sonido envolvente portátil", 39.99));
        testProducts.add(new Product("8E5", "Cámara web HD", null, "Videollamadas en alta resolución", 59.90));
        List<Product> savedProducts = productRepository.saveAll(testProducts);
        checkSampleProductsAreDisplayed();

        Product deletingProduct = savedProducts.getFirst();
        productRepository.delete(deletingProduct); //The saved products include their id

        driver.get(BASE_URL);
        wait.until(ExpectedConditions.urlContains(BASE_URL));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("product")));
        List<WebElement> productDivs = driver.findElements(By.className("product"));

        assertEquals(testProducts.size()-1, productDivs.size());
        for (int i = 0; i < productDivs.size(); i++) {
            assertFalse(productDivs.get(i).getText().contains(savedProducts.getFirst().getReferenceCode()));
            assertFalse(productDivs.get(i).getText().contains(savedProducts.getFirst().getName()));
            assertFalse(productDivs.get(i).getText().contains(savedProducts.getFirst().getDescription()));
            assertFalse(productDivs.get(i).getText().contains(String.valueOf(savedProducts.getFirst().getPrice())));
        }
    }

    private void checkSampleProductsAreDisplayed(){
        driver.get(BASE_URL);
        wait.until(ExpectedConditions.urlContains(BASE_URL));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("product")));
        List<WebElement> productDivs = driver.findElements(By.className("product"));

        assertEquals(testProducts.size(), productDivs.size());
        for (int i = 0; i < productDivs.size(); i++) {
            assertTrue(productDivs.get(i).getText().contains(testProducts.get(i).getReferenceCode()));
            assertTrue(productDivs.get(i).getText().contains(testProducts.get(i).getName()));
            assertTrue(productDivs.get(i).getText().contains(testProducts.get(i).getDescription()));
            assertTrue(productDivs.get(i).getText().contains(String.valueOf(testProducts.get(i).getPrice())));
        }
    }
}
