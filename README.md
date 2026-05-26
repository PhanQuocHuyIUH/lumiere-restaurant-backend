# Lumière Restaurant — Backend

Backend dịch vụ chính cho hệ thống quản lý nhà hàng **Lumière**: một nền tảng nhà hàng hợp nhất phục vụ khách hàng quét QR đặt món, phục vụ bàn (waiter POS), bếp (KDS), quản trị (admin) và một dịch vụ AI riêng cho gợi ý món, chatbot, dự báo và combo.

Repo này là **API gateway nghiệp vụ** — nguồn sự thật cho menu, đơn hàng, bàn, ca làm, thanh toán và quyền truy cập của toàn hệ thống.

---

## Hệ sinh thái Lumière

| Thành phần | Mô tả | Repository |
|---|---|---|
| **Backend (repo này)** | Spring Boot REST + WebSocket, PostgreSQL, Redis, tích hợp VNPay/VietQR | — |
| **AI Service** | FastAPI service cho recommendation, chatbot, forecast, combo, vector search | [lumiere-ai-service](https://github.com/PhanQuocHuyIUH/lumiere-ai-service) |
| **Customer Web** | Web khách hàng (quét QR, gọi món tại bàn, thanh toán) | [Lumiere-customer-web](https://github.com/PhanNhatTien090/Lumiere-customer-web) |
| **Kitchen Display (KDS)** | Màn hình bếp realtime, quản lý trạng thái món | [Lumiere-kds-web](https://github.com/PhanNhatTien090/Lumiere-kds-web) |
| **Admin Web** | Quản trị menu, bàn, nhân sự, báo cáo, AI ops | [lumiere-admin-web](https://github.com/PhanNhatTien090/lumiere-admin-web) |
| **Waiter POS** | Ứng dụng POS cho phục vụ — nhận order, in bill, chuyển bàn | [lumiere-waiter-pos](https://github.com/PhanNhatTien090/lumiere-waiter-pos) |

---

## Tính năng chính

- **Quản lý nhà hàng end-to-end**: menu, danh mục, bàn, QR đặt món, đơn hàng, bếp, phục vụ, thanh toán, kho, ca làm, hỗ trợ khách hàng.
- **Realtime qua WebSocket**: đồng bộ trạng thái order/bàn/bếp giữa Customer Web, KDS, Waiter POS và Admin.
- **Bảo mật**: Spring Security + JWT stateless, phân quyền theo vai trò (admin / waiter / kitchen / customer).
- **Thanh toán**: tích hợp **VNPay** (thẻ/QRCode) và **VietQR** (chuyển khoản NAPAS) — sinh QR động, xác minh IPN, hoàn tiền.
- **AI tích hợp**: gọi sang `lumiere-ai-service` qua WebClient có **circuit breaker (Resilience4j)** + timeout per-operation cho gợi ý món, chatbot, forecast doanh thu, sinh combo, batching đơn.
- **Storage**: Cloudinary cho ảnh menu và QR bàn.
- **Migration**: Flyway versioned, schema `validate` ở runtime.
- **Observability**: Swagger UI tại `/api/v1/swagger-ui.html`.

## Tech Stack

| Lớp | Công nghệ |
|---|---|
| Runtime | **Java 21** (virtual threads enabled) |
| Framework | **Spring Boot 3.5**, Spring Security, Spring Data JPA, WebFlux (WebClient), WebSocket |
| Database | **PostgreSQL** (Supabase/local Docker) + Flyway |
| Cache | **Redis** (Lettuce, manual `RedisTemplate`) |
| Auth | JWT (jjwt 0.12) |
| Resilience | Resilience4j Circuit Breaker, Spring Retry |
| Docs | springdoc-openapi (OpenAPI 3) |
| Storage / Util | Cloudinary, Google ZXing (QR) |
| Build | Maven, Docker, docker-compose |

## Kiến trúc module (`iuh.fit.se.lumiere_restaurant_backend.*`)

```
identity      – auth, user, role, JWT
menu          – categories, dishes, options, pricing
table         – tables, QR sessions
ordering      – orders, order items, lifecycle
kitchen       – KDS pipeline, station routing
serving       – waiter workflows
billing       – invoices, VNPay, VietQR, refunds
inventory     – stock, ingredients
shift         – work shifts, attendance
analytics     – reporting, dashboards
support       – customer support, feedback
ai            – AI service client (WebClient + circuit breaker)
shared        – common utils, config, exceptions
```

## Bắt đầu

### Yêu cầu

- JDK 21
- Maven 3.9+ (hoặc dùng `mvnw` đi kèm)
- PostgreSQL 15+ và Redis 7+ (hoặc dùng `compose.yaml`)
- File `.env` ở thư mục gốc (xem `application.yml` để biết các biến cần)

### Chạy bằng Docker

```bash
docker compose up --build
```

### Chạy local

```bash
./mvnw spring-boot:run
```

Mặc định:

- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`

## Biến môi trường chính

| Nhóm | Biến |
|---|---|
| Database | `DB_URL`, `DB_MIGRATION_URL`, `DB_USER`, `DB_PASS` |
| Auth | `JWT_SECRET` |
| Redis | `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL` |
| AI Service | `AI_BASE_URL`, `AI_SERVICE_KEY`, `AI_ENABLED`, `AI_TIMEOUT_*_MS` |
| Payment | `VNPAY_*`, `VIETQR_*` |
| Storage | `CLOUDINARY_*` |
| QR | `QR_BASE_URL`, `QR_SESSION_EXPIRATION` |

---

## License

Đồ án tốt nghiệp (KLTN) — IUH, Khoa Công nghệ Thông tin.
