## 🏁 App execution

Due to the application's extensive dependency list, we utilize Docker Compose for execution. This allows for the concurrent management of the application and its required services, including the MySQL database and MinIO image storage.

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

**1. Environment Variables Configuration**

Before running the stack, you must provide the necessary credentials to prevent startup warnings and ensure full functionality. Create a file named `.env` in your working directory with the following structure:

```env
GOOGLE_AUTH_CLIENT_ID=your_google_client_id
SENDER_MAIL_ADDRESS=your_email@example.com
SENDER_MAIL_PORT=587
SENDER_MAIL_PASSWORD=your_email_app_password
```

> ℹ️ **NOTE:** This environment variables are directly responsible for the application's email delivery and third-party authentication functionalities. In order for this feature to work properly, you must provide valid credentials and connection details from a real SMTP server.

**2. Deploy the stack**

Execute the composition directly from the remote registry using a terminal window located in the same folder as the `.env` file. Docker Compose will automatically read it, fetch the required images, and orchestrate all containers simultaneously in detached mode:

```bash
# For the development version:
docker compose -f oci://mjpulido/frict:dev up -d

# For the production (latest) version:
docker compose -f oci://mjpulido/frict up -d
```

&nbsp;

[◀️](/docs/pages/02-detailed-features.md) **Page 3. Execution** [▶️](/docs/pages/04-development-guide.md)

[⏪ Return to Index](/README.md)