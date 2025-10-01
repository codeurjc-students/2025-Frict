## 📡 Development guide

### 🔎 Index

1. [Introduction](#-introduction)
2. [Technologies Stack](#-technologies-stack)
3. [Tools](#-tools)
4. [Architecture](#-architecture)
5. [Quality Assurance](#-quality-assurance)
6. [Development Process](#-development-process)
7. [Code Environment and Execution](#-code-environment-and-execution)

&nbsp;

### 📍 Introduction

This website follows an SPA (Single-Page Application) architecture, where each of the pages will be composed by combining different independent pages. This way, a faster and smoother experience is achieved by reloading only the necessary components during each user interaction.

Regarding the main components of the system, we have:
- **Client**: Angular project that will make and receive REST requests to obtain the content to be displayed.
- **Server**: Spring Boot project with Maven that will contain the REST API for data management.
- **Database**: MySQL component with a dynamic schema, created within the Spring Boot entities and annotations.

This distribution allows the frontend and backend to be completely separated, so that each works independently. In this way, better scalability and maintainability are achieved by reducing dependencies between them.

The main content of this section is briefly detailed on the table below.

| Feature             | Description                                                                                                                 |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------|
| Type of website     | SPA web application with MVC architecture                                                                                   |
| Main technologies   | Spring Framework (Java), Angular (TypeScript + HTML), MySQL (SQL), JSON, Docker                                             |
| Tools               | IntelliJ IDEA, MySQL Workbench, Git, Postman                                                                                |
| Quality assurance   | Unit, Integration and E2E testing using JUnit, Mockito, Rest Assured and Selenium. Static code analysis via Sonarqube Cloud |
| Deployment          | Containerization with Docker, cloud-integrated deployment on Amazon Web Services                                            |
| Development process | Iterative and incremental through the use of feature branches, issues and pull requests before merging                      |

&nbsp;

### 📋 Technologies Stack

#### 💾 Backend

- [**Spring Boot**](https://spring.io/projects/spring-boot): 
Facilitates the creation and execution of REST services by reducing initial configuration and providing a ready-to-use productive environment.


- [**Spring Data**](https://spring.io/projects/spring-data): 
Simplifies database access and management through repositories and automatic queries, streamlining data persistence.


- [**Spring Security**](https://spring.io/projects/spring-security): 
Handles authentication and authorization, ensuring that resources and endpoints are protected against unauthorized access.


- [**Java**](https://www.java.com/en/): 
Used as the main programming language, offers object-oriented structure and portability.


- [**Maven**](https://maven.apache.org/): 
Simplifies project building, packaging, testing, and dependency management.


- [**MySQL**](https://www.mysql.com/): 
Relational database engine that provides reliable data persistence.

  
#### 📺 Frontend

- [**Angular**](https://angular.dev/): Manages the user interface and client-side logic, allowing dynamic rendering of components and seamless navigation in the SPA.


- [**TypeScript**](https://www.typescriptlang.org/): Provides type safety and tooling support, helping to prevent errors and improve maintainability of the frontend code.


- [**JSON**](https://www.json.org/json-en.html): Serves as the standard format for exchanging data between the Angular frontend and the Spring Boot backend.

#### 🔬 DevOps

- [**Docker**](https://www.docker.com/): Packages the application and its dependencies into isolated containers, ensuring consistent execution across environments.


- [**GitHub Actions**](https://github.com/features/actions): Automates workflows such as building, testing, and deploying the application through continuous integration and continuous delivery (CI/CD).


- [**Docker Compose**](https://docs.docker.com/compose/): Orchestrates multiple Docker containers, allowing easy setup and management of the full application stack.

&nbsp;

### 🔧 Tools

- [**IntelliJ IDEA**](https://www.jetbrains.com/idea/): Provides a powerful IDE for efficient backend development, debugging, and integration with Spring Boot.


- [**MySQL Workbench**](https://www.mysql.com/products/workbench/): Facilitates database design, visualization, and query execution for managing the MySQL schema.


- [**Git**](https://git-scm.com/): Enables version control, collaboration, and tracking of code changes across the development team.


- [**Postman**](https://www.postman.com/home): Allows testing and validation of REST APIs through automated and manual requests.

&nbsp;

### 🏢 Architecture

- **Deployment**: Integrated cloud deployment using [Amazon Web Services](https://aws.amazon.com/es/).


- **API REST**: Follows [OpenAPI](https://www.openapis.org/) description standard, and turned into HTML in order to be visualized directly from GitHub without running the application.

&nbsp;

### ☑️ Quality Assurance

#### ⌛ Testing

The application will be tested using 3 different types of tests, each one assuring that all commits and pull requests meet the established requirements.
- **Unit tests**: Proof that the functionality provided by a method, class or even a component works as expected.


- **Integration tests**: Check that the connections and communications between the different components in the system are working properly. 


- **E2E or System tests**: Revise whether the whole system, or some of its entire components, meet the necessary objectives for which it was designed.

#### 📊 Static code analysis

Using **SonarQube Cloud** and **JaCoCo** plugin for Maven, a first **overall project state** is presented. In this first analysis, the most remarkable aspects are the security issues (most of them caused by the use of test default passwords).

![SonarQube Overall Analysis](/docs/images/sonarqube_overall.png)

Also, regarding **code quality and test coverage**, DatabaseInitializer class is again the worst rated because of the aspect described above.

![SonarQube Code Analysis](/docs/images/sonarqube_code.png)

&nbsp;

### 📅 Development Process

The project is being developed using an iterative and incremental process, which follows the Agile principles and applies some of the good coding practices described by XP (Extreme Programming) and Kanban methodologies, such as:
- Code refactoring to achieve improvement
- Simple, incremental and evolutionary changes
- Quality improvement via Continuous Integration


#### ✏️ Task management

In order to keep control of the pending tasks in each iteration, the project will include:

- **GitHub Issues**: Contains the main information about a task, such as its title, description, priority, deadline or even associated branch.
 

- **GitHub Project**: Contains each one of the issues to be completed during the iteration, divided by columns depending on its progress.
  - Columns: _To Do, In Progress, Under Review, Done_.
  - Priorities: _Low, Medium, High, Very High_.
  - Task size: _XS, S, M, L, XL_.

#### 📀 Git Version control

**Main branch**

Remains stable and always available for deployment.


**Branching strategy** 

Each branch contains will enclose the code changes made to implement a single functionality, and will be merged to the main branch once the implementation has concluded and all required tests have been passed successfully.

**Branching process**

1. Create the branch `iteration-feature` from main (e.g. `2-add-minimal-services`)


2. Commit to the branch. After each commit, client and server unit tests will automatically be triggered.


3. Pull request to the main branch after feature implementation is completed. Before merging, client and server unit, integration and system tests will be automatically triggered.

#### 🚧 Continuous Integration (CI)

Project testing and static code analysis is automated by using CI/CD pipelines and GitHub Actions workflows. 

#### Unit Testing On Commit (unit_test_on_commit.yml)

- Runs all **unit tests** with **every commit** made in a feature branch, or manually


- Tests server and client separately to optimize workflow duration


- Does not run any component during the tests

#### Full System Testing On Pull Request (full_test_on_pull_request.yml)

- Runs **before merging a pull request** from a feature branch to the main branch, or manually


- Runs all **unit, integration and e2e tests**


- Backend component is started before integration tests


- Frontend component is started before e2e UI test

#### SonarQube Static Code Analysis (sonar_analysis.yml)

- Runs only manually (due to its complexity and duration)


- Runs all unit, integration and e2e tests


- Backend and frontend components are started before the analysis


- Auxiliar MySQL Docker container is built before the analysis


- JaCoCo coverage tool is used during the analysis, so SonarQube can show coverage metrics

&nbsp;

### 🏁 Code Environment and Execution

#### Initial requirements
In order to be able to run this project, you must have installed:
- Git
- Node.js (includes npm, Node Package Manager)
- Maven for Windows
- Docker
- Angular CLI

#### Command-based execution

1. Open a new terminal window.


2. Download and access the project repository

```
# Install all necessary files
git clone https://github.com/codeurjc-students/2025-2025-Frict.git Frict

# Access the project folder
cd Frict
```

3. Set up the MySQL database

```
# Create a Docker container with a ready-to-use MySQL database and start it
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=rootpass -e MYSQL_DATABASE=Frict -e MYSQL_USER=appuser -e MYSQL_PASSWORD=apppass -p 3306:3306 mysql:8.0
```
> ℹ️ NOTE: In this case, database credentials are the default ones:
> - Root password: _rootpass_
> - User: _appuser_
> - Password: _apppass_
> 
> This credentials could be modified as desired, considerably increasing database security.

4. Modify `application.properties` file

If you decided to change the database credentials in the previous step, these credentials must be also updated in `application.properties` file within Spring backend. 

```
spring.datasource.username=appuser # Set your custom username
spring.datasource.password=apppass # Set your custom password
```

5. Run the backend component
```
cd backend #Access backend project
mvnw spring-boot:run #Start the Spring application
```

6. Run the frontend component
```
cd frontend #Access frontend project
npm install #Installs all necessary dependencies to run the Angular project
ng serve --proxy-config proxy.conf.json #Starts the Angular project using default proxy
```

7. Open application in browser

Go to https://localhost:4202 in your preferred web browser to access the main page of the application.

#### Running tests
Current tests can be run either for the backend component:
```
cd backend #Access backend project
mvn test #Run all tests
```

Or for the backend component:
```
cd frontend #Access frontend project
ng test --watch=false #Run all tests
```
> ℹ️ NOTE: ProductSystemUITest class requires the frontend component to be up before running its tests for them to be completed successfully. 


#### Using API endpoints
Fully implemented API endpoints are available when backend component is running. Current available endpoints are described below.
```
#Get a product by its id (GET)
https://localhost:443/api/products/1 #Gets information about product with id 1

#Get all available products (GET)
https://localhost:443/api/products/all

#Create a new product (POST)
https://localhost:443/api/products #Creates a product using provided information

#Update an existing product (PUT)
https://localhost:443/api/products/1 #Updates the product with id 1 information using the new data provided

#Delete an existing product (DELETE)
https://localhost:443/api/products/1 #Deletes all the information associated with the product with id 1
```

> ℹ️ NOTE: Product creation and update requires the product data to be provided in the request body. This body should match the following structure:
> 
>{
> 
> "referenceCode": "4A5",
> 
> "name": "Android tablet",
>
> "description": "Entertainment with a bigger screen",
> 
> "price": 130.0
> 
> }

> ℹ️ NOTE: To make this process easier and faster, complete fully-functional endpoints list is available at frict.postman_collection.json file, which could be easily imported by Postman.

&nbsp;