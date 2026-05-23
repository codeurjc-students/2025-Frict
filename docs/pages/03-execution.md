## 🏁 App execution

Due to the application's extensive dependency list, Docker Compose is used for execution. This allows for the concurrent management of the application and its required services, including the MySQL database and MinIO image storage.

### Prerequisites

To get the application up and running, ensure you have the following tools installed based on your operating system:

* **Windows / macOS:** Install **[Docker Desktop](https://www.docker.com/products/docker-desktop/)**. This package includes Docker Engine, Docker CLI, and Docker Compose, providing everything needed to orchestrate the application containers.


* **Linux:** You will need to install the **Docker Engine** (which includes the **Docker CLI**) and the **Docker Compose** plugin:
     * **[Install Docker Engine](https://docs.docker.com/engine/install/)**: Follow the official guide for your specific distribution (e.g., Ubuntu, Debian, Fedora).
     * **[Install Docker Compose V2](https://docs.docker.com/compose/install/linux/)**: Ensure the Compose plugin is available to manage the multi-container stack.

&nbsp;

### Docker-based execution

In order to run the application using a Docker Compose container composition, the following steps should be executed. This approach allows for the concurrent management of the main application and its required auxiliary services (**MySQL** database and **MinIO** object storage).

Ensure Docker is installed and added to your system's PATH before proceeding.

#### 1. Environment Variables Configuration

Before running the stack, you must provide the necessary credentials to prevent startup warnings and ensure full functionality. Create a file named `.env` in your working directory with the following structure:

```env
GOOGLE_AUTH_CLIENT_ID=your_google_client_id
SENDER_MAIL_ADDRESS=your_email@example.com
SENDER_MAIL_PORT=587
SENDER_MAIL_PASSWORD=your_email_app_password
```

> ℹ️ **NOTE:** This environment variables are directly responsible for the application's email delivery and third-party authentication functionalities. In order for this feature to work properly, you must provide valid credentials and connection details from a real SMTP server.

&nbsp;

#### 2. Deploy the stack

Execute the composition directly from the remote registry using a terminal window located in the same folder as your `.env` file. Docker Compose will automatically read it, fetch the required images, and orchestrate all containers simultaneously in detached mode (running in the background):

```bash
# For the development version:
docker compose -f oci://mjpulido/frict:dev-compose up -d

# For the production (latest) version, with DB ports not exposed:
docker compose -f oci://mjpulido/frict up -d
```

##### Managing the Stack (Stop / Restart):
* **To stop the application:** You can safely stop the services to free up system resources without losing any data. You can do this via the Docker Desktop interface or by running the same command replacing `up -d` with `down` (e.g., `docker compose -f oci://mjpulido/frict down`).


* **To launch it again:** Use **exactly the same `up -d` command** shown above. Docker is smart enough to detect that the images and configuration are already on your system, so it will bypass the download phase and start the services almost instantaneously.

&nbsp;

#### 3. Access the Application

Once all containers are healthy, open your browser and navigate to:

* **Main Application:** **`http://localhost`** (served via HAProxy on port 80)
* **MinIO Console (optional):** `http://localhost:9001` for managing stored images

&nbsp;

#### 4. Login Credentials

The application comes pre-configured with a set of example accounts to test the different organizational roles. You can use the following credentials to log in (using the login page):

| Role | Username | Password | Permissions                                                                                                     |
| :--- | :--- | :--- |:----------------------------------------------------------------------------------------------------------------|
| **Administrator** | `admin` | `adminpass` | Full system access: manage data that affect the entire organization decision-making.                            |
| **Manager** | `manager` | `managerpass` | Assigned shops access: supervise orders management, restock products or manage the shops delivery trucks fleet. |
| **Driver** | `driver` | `driverpass` | Order delivery responsibility: keep track of the associated truck and orders until given to the clients.        |
| **Registered User** | `user` | `pass` | Online shop customer: Place orders, publish reviews and revise current information and previous activity.       |

> ℹ️ **NOTE:** For Google-based external signup and login, and unless an administrator had created an internal account using your email, the system will automatically create or log you in using a Registered User account. Please ensure the GOOGLE_AUTH_CLIENT_ID is correctly configured in your `.env` file for this feature to work correctly.

&nbsp;

#### 5. Default Data

Upon startup, the application's database is automatically populated with a comprehensive set of seed data to facilitate immediate testing and demonstration, using `DatabaseInitializer` class. This environment includes:

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