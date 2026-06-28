## 🏁 App execution

The application runs entirely on Docker, so no language runtimes or build tools need to be installed locally. The project ships with two distinct Docker Compose configurations depending on your goal:

* **Local Production Environment** — replicates the AWS production topology by running three application instances load-balanced behind HAProxy. Ideal for evaluating the system as it behaves in the cloud.
* **Development Environment** — runs a single application instance with debugging support and self-signed TLS certificates for MinIO. Ideal for working on the source code.

Both environments bundle the full stack: the application, the MySQL relational database, the MongoDB database (replica set enabled), and MinIO object storage. The application containers wait for all data services to report `healthy` before starting, so the very first launch may take a little longer while the databases initialize.

&nbsp;

### ⚙️ Prerequisites and Environment Setup

The only requirement is having Docker installed and available on your system's PATH:

* **Windows / macOS:** Install **[Docker Desktop](https://www.docker.com/products/docker-desktop/)**, which includes Docker Engine, Docker CLI, and Docker Compose.


* **Linux:** Install the **[Docker Engine](https://docs.docker.com/engine/install/)** (which includes the Docker CLI) and the **[Docker Compose V2](https://docs.docker.com/compose/install/linux/)** plugin separately, following the official guide for your distribution.

With Docker ready, you must provide the credentials for the external services (Google sign-in and email delivery). This is the same for **both** environments: before starting the containers, create a `.env` file **in the directory from which you run the command**, with the following content:

```env
GOOGLE_AUTH_CLIENT_ID=your_google_client_id
SENDER_MAIL_HOST=smtp.gmail.com
SENDER_MAIL_ADDRESS=your_email@example.com
SENDER_MAIL_PORT=587
SENDER_MAIL_PASSWORD=your_email_app_password
```

> ℹ️ **NOTE:** These variables are directly responsible for the application's email delivery and third-party authentication functionalities. To enable them, you must provide valid credentials and connection details from a real SMTP server.

&nbsp;

### 🚀 Local Production Environment

This environment replicates the AWS production setup, bringing up three application instances balanced through HAProxy. It does not require cloning the repository, since the Compose artifact is pulled directly from DockerHub.

#### 1. Deploy the stack

From a terminal located in the same folder as your `.env` file, run one of the following commands. Docker Compose will automatically read the file, fetch the required images, and orchestrate all containers in detached mode (running in the background):

```bash
# For the development version (:dev image):
docker compose -f oci://mjpulido/frict:dev-compose up -d

# For the production version (:latest image), with DB ports not exposed:
docker compose -f oci://mjpulido/frict up -d
```

#### 2. Access the application

Once all containers are healthy, open your browser and navigate to:

* **Main Application:** **`http://localhost`** (port 80, served via HAProxy)
* **MinIO Console (optional):** `http://localhost:9001` for managing stored images

#### 3. Managing the stack (Stop / Restart)

* **To stop the application:** You can safely stop the services to free up system resources without losing any data, either via the Docker Desktop interface or by replacing `up -d` with `down`:

```bash
docker compose -f oci://mjpulido/frict down
```

* **To launch it again:** Use **exactly the same `up -d` command** as before. Docker detects that the images and configuration are already on your system, so it skips the download phase and starts the services almost instantly.

&nbsp;

### 🛠️ Development Environment

This environment brings up a single application instance over HTTPS with self-signed TLS certificates for MinIO. Like the production environment, it does **not** require cloning the repository — the Compose artifact is pulled directly from DockerHub. From a terminal located in the same folder as your `.env` file:

```bash
docker compose -f oci://mjpulido/frict:dev-compose-single up -d
```

Once all containers are healthy, the application is accessible at:

* **Main Application:** **`https://localhost`** (port 443)
* **MinIO Console (optional):** `https://localhost:9001` for managing stored images

&nbsp;

### 🔑 Login Credentials

Both environments come pre-configured with the same set of example accounts to test the different organizational roles. You can use the following credentials to log in (using the login page):

| Role | Username | Password | Permissions                                                                                                     |
| :--- | :--- | :--- |:----------------------------------------------------------------------------------------------------------------|
| **Administrator** | `admin` | `adminpass` | Full system access: manage data that affect the entire organization decision-making.                            |
| **Manager** | `manager` | `managerpass` | Assigned shops access: supervise orders management, restock products or manage the shops delivery trucks fleet. |
| **Driver** | `driver` | `driverpass` | Order delivery responsibility: keep track of the associated truck and orders until given to the clients.        |
| **Registered User** | `user` | `pass` | Online shop customer: Place orders, publish reviews and revise current information and previous activity.       |

> ℹ️ **NOTE:** For Google-based external signup and login, and unless an administrator had created an internal account using your email, the system will automatically create or log you in using a Registered User account. Please ensure the GOOGLE_AUTH_CLIENT_ID is correctly configured in your `.env` file for this feature to work correctly.

&nbsp;

### 📦 Default Data

Upon startup, the application's database is automatically populated with a comprehensive set of seed data to facilitate immediate testing and demonstration, using the `DatabaseInitializer` class. This environment includes:

* **Users:** Four predefined accounts representing each system role (Standard User, Admin, Manager, and Driver). The customer and admin accounts are fully populated with mock personal data, including geolocated addresses and saved payment cards.


* **Categories:** A tech-oriented hierarchical catalog featuring main groups like Computers, Components, Peripherals, and Smart Home, alongside promotional tag-categories such as "Top Sellers" and "Recommended".


* **Products:** A catalog of 30 distinct electronic items (e.g., gaming laptops, smartphones, graphics cards, and routers), complete with realistic pricing, simulated discounts, and category assignments.


* **Shops:** Two sample physical locations ("Madrid-Recoletos" and "Alicante") featuring specific operational budgets, precise geographical coordinates, and pre-assigned staff.


* **Orders:** Two sample purchases placed by the default users at the sample shops. They contain multiple products and demonstrate the system's ability to track different fulfillment statuses.


* **Stock:** The main store ("Madrid-Recoletos") is automatically pre-stocked with varying quantities of all 30 products from the global catalog, showcasing local inventory management.


* **Reviews:** Sample user feedback on the products, including a verified 5-star positive review and an unverified 2-star negative review, illustrating how global product ratings are calculated.

---

**At this point, your Docker stack is fully operational**. With the environment securely configured and the seed data properly loaded, the application is ready for comprehensive evaluation and testing.

&nbsp;

[◀️](/docs/pages/02-detailed-features.md) **Page 3. Execution** [▶️](/docs/pages/04-development-guide.md)

[⏪ Return to Index](/README.md)