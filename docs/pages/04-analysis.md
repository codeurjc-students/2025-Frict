## 📊 Analysis

### Screens and navigation
> ℹ️ NOTE: Both online shop and management subsystems have login, register and recover password screens in common, so that the user will be redirected to one of the subsystems depending on the permissions level provided.

#### Online shop subsystem
- [All detailed views](/docs/pages/shop-subsystem.md)
- Screen flow diagram:

![Shop subsystem screens diagram](/docs/sketches/shop/Frict-shop.png)
&nbsp;


#### Management subsystem
- [All detailed views](/docs/pages/management-subsystem.md)
- Screen flow diagram:

![Management subsystem screens diagram](/docs/sketches/management/Frict-management.png)
&nbsp;

### Entities
![Database schema diagram](../images/schema.png)

### Permissions
In order to separate customers and staff functionalities and to prevent internal data from being accessed by any user, each functionality will only be available to its corresponding user roles, which will be:
- **Anon user**: Lowest level of permissions, restricted to product data read only.
- **Registered user**: Identified user which will be able to order products and track their orders.
- **Store manager**: Used by the person in charge of an specific shop. Allowed to check that shop statistics and products, and restock them if necessary.
- **Delivery driver**: Used by truck drivers to check the content of their assigned orders, and to update its status when delivered.
- **Administrator**: Highest level of permissions, will be able to manage all registered shops, check overall company data and restrict the system access to any registered user if needed.


| Basic funtionality                                   | Anon User | Registered User | Store Manager | Delivery driver | Administrator |
| ------------------------------                       | --------- | --------------- | ------------- | --------------- | ------------- |
| Check product info                                   | ✅ | ✅ | ✅ | ✅ | ✅ |
| Manage orders                                        |  | ✅ | ✅ | ✅ | ✅ |
| Manage products                                      |  |  | ✅ |  | ✅ |
| Upload images                                        |  | ✅ | ✅ | ✅ | ✅ |
| Manage trucks                                        |  |  | ✅ |  | ✅ |
| Manage shops                                         |  |  |  |  | ✅ |
| Ban/Unban users                                         |  |  |  |  | ✅ |
| Manage reviews                                       |  | ✅ |  |  | ✅ |
| Check basic use-based and predicted product data     | ✅ | ✅ | ✅ | ✅ | ✅ |
| Check advanced use-based and predicted product data  |  |  | ✅ |  | ✅ |


### Images
- `Product`: Multiple images per product
- `Profile`: Single image per profile

### Charts
- Weekly benefits: line chart
- Sells per shop: bar chart
- Assigned orders per truck: pie chart

### Complementary technologies
- **JS2PDF**: Receipts/Invoices generation
- **JavaMail**: Email order confirmations/alerts
- **OpenStreetMap**: Shops and trucks positioning
- **ng2-charts**: Data charts

### Advanced algorithm/query
- Real-time truck positioning
- Real-time data processing and updating in order to obtain precise predictions
- Product filtering and recommendations based on user´s interests

&nbsp;

[◀️](/docs/pages/03-functionalities.md) **Page 4. Analysis** [▶️](/docs/pages/05-progress-tracking.md)

[⏪ Return to Index](/README.md)
