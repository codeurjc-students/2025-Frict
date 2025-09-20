package com.tfg.backend.e2e;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.model.Product;
import com.tfg.backend.service.ProductService;
import org.junit.jupiter.api.*;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

//CLIENT SIDE SYSTEM TESTS
@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
public class ProductSystemTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String BASE_URL = "https://localhost:4202";

    @Autowired
    private ProductService productService;

    @BeforeAll
    public static void setUp()  {
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        //options.addArguments("--headless=new");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(BASE_URL);
    }


    @AfterAll
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void displayCreatedProductsTest() {
        wait.until(ExpectedConditions.urlContains(BASE_URL));

        //Initial number of products
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.m-4")));
        int initialProducts = driver.findElements(By.cssSelector("div.m-4")).size();

        Product product = new Product("5A6", "Disco duro", null, "Almacenamiento sin límites", 75.49);
        productService.save(product);
        driver.navigate().refresh();

        // Locate all products
        List<WebElement> productDivs = driver.findElements(By.cssSelector("div.m-4"));

        // Verify the new product is shown
        assertEquals(initialProducts + 1, productDivs.size(), "El número de productos mostrados no es el esperado");
        String createdProduct = productDivs.getLast().getText();
        assertTrue(createdProduct.contains(product.getReferenceCode()));
        assertTrue(createdProduct.contains(product.getName()));
        assertTrue(createdProduct.contains(product.getDescription()));
        assertTrue(createdProduct.contains(String.valueOf(product.getPrice())));
    }
}
