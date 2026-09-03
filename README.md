# Procurement System

기업 구매 거래를 구매요청부터 월 마감까지 연결하는 Spring Boot 기반 백엔드 프로젝트입니다.

단순 CRUD보다 **부분입고·분할송장·누적 3-Way Matching·미착 원장·재실행 가능한 월 마감**처럼 데이터 정합성이 중요한 구매 업무를 구현하는 데 초점을 맞췄습니다.

## 핵심 업무 흐름

```text
구매요청(PR) 작성·제출
  → 구매 담당자의 승인·거절
  → 승인된 요청으로 발주(PO) 생성·발송
  → 발주 라인별 부분입고(GR)
  → 미착 원장 GR_ACCRUAL(+) 계상
  → 분할 송장 접수
  → PO·누적 GR·누적 Invoice 3-Way Matching
      ├─ MATCHED: 접수된 송장 금액만 INVOICE_MATCH(-) 상계
      ├─ HOLD_QUANTITY: 송장 미접수 또는 입고수량 초과
      └─ HOLD_PRICE: 안분 금액의 허용 오차 초과
  → 월 마감 상태·잔액·미해결 HOLD 스냅샷 확정
```

## 주요 설계

### 발주 라인 중심 문서 연결

입고 라인과 송장 라인은 문서 헤더가 아니라 `purchase_order_line`을 직접 참조합니다. 한 발주 라인에 여러 입고와 송장을 연결해 부분입고와 분할송장을 별도 예외 모델 없이 표현합니다.

### 부분 송장 안분 대사

현재까지 접수된 송장수량이 누적 입고수량을 넘지 않으면 다음 기준액과 누적 송장 공급가액을 비교합니다.

```text
expectedAmount  = PO_LINE.unitPrice × invoicedQuantity
amountTolerance = MIN(expectedAmount × 1%, 10,000원)
```

`MATCHED`는 현재 접수된 송장분이 정합하다는 뜻이며 발주 라인의 종결을 의미하지 않습니다. 이미 상계한 금액을 제외한 신규 금액만 원장에 추가합니다.

### 라인별 부가세 계산

송장 공급가액은 수량과 단가를 곱해 소수 둘째 자리에서 반올림하고, 부가세는 각 라인에서 10%를 계산한 뒤 `RoundingMode.DOWN`으로 원 단위 미만을 절사합니다. 헤더 금액은 라인 금액의 합으로 계산합니다.

### 마감 상태와 실행 이력 분리

`close_period`는 기간의 `OPEN/CLOSED` 상태를, `close_run`은 실행별 `attempt_no`와 `SUCCESS/FAILED/SKIPPED` 결과를 저장합니다. 실패 후 재실행은 새 attempt로 기록하고, 이미 닫힌 기간의 재실행은 결과를 바꾸지 않은 채 `SKIPPED` 이력을 남깁니다.

## 기술 구성

- Java 21
- Spring Boot 4.1
- Spring Web MVC, Spring Data JPA, Spring Security, Validation
- MySQL 8.4
- Flyway V1~V12, `ddl-auto=validate`
- JUnit 5, Testcontainers MySQL
- GitHub Actions

## 실행 방법

요구 사항은 Java 21과 Docker입니다.

```bash
docker compose up -d
cd backend
./gradlew bootRun
```

기본 애플리케이션 포트는 `8081`, MySQL 포트는 `3308`입니다.

```bash
curl http://localhost:8081/actuator/health
```

데이터베이스 접속 정보는 환경변수로 변경할 수 있습니다.

| 환경변수 | 기본값 |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3308` |
| `DB_NAME` | `procurement` |
| `DB_USERNAME` | `procurement` |
| `DB_PASSWORD` | `procurement` |
| `SERVER_PORT` | `8081` |

## 테스트

```bash
cd backend
./gradlew test
```

2026-09-03 기준 Testcontainers MySQL 환경에서 전체 **120개 테스트가 통과**했습니다.

검증 범위에는 다음 시나리오가 포함됩니다.

- 부분입고 누적과 초과입고 차단
- 마지막 잔여수량에 대한 동시 입고 방어
- 분할송장 안분 대사와 신규 금액만 상계
- HOLD 자동 재대사와 트랜잭션 롤백
- 월 마감 성공·중복 실행·실패·재실행
- 수동·자동 동시 마감 시 성공 결과 한 벌 생성
- 역할별 API 허용·거부

## API 범위

모든 업무 API는 HTTP Basic 인증을 사용합니다. 헬스 체크만 인증 없이 접근할 수 있습니다.

- `GET /api/auth/me`
- `GET/POST/PUT /api/purchase-requests`
- `POST /api/purchase-requests/{id}/submit`
- `POST /api/purchase-requests/{id}/approve`
- `POST /api/purchase-requests/{id}/reject`
- `POST /api/purchase-requests/{id}/purchase-order`
- `POST /api/purchase-orders/{id}/send`
- `POST /api/purchase-orders/{id}/goods-receipts`
- `POST /api/purchase-orders/{id}/invoices`
- `POST /api/close-periods/{period}/close`

## ADR

- [ADR 0001: 구매 문서를 헤더가 아닌 라인 단위로 연결한다](docs/adr/0001-line-level-document-linkage.md)
- [ADR 0002: 금액과 부가세를 라인별로 계산하고 대사는 송장 수량 기준으로 안분한다](docs/adr/0002-line-level-tax-and-amount-policy.md)
- [ADR 0003: 마감 상태와 실행 이력을 분리한다](docs/adr/0003-separate-close-period-and-run.md)

## 현재 한계와 다음 범위

- 마감 전 문서 취소와 `CANCEL_OFFSET` 원장 상쇄는 아직 구현하지 않았습니다.
- 마감 후 `REVERSAL`, 중복 역분개 차단, 정상값 재기표는 아직 구현하지 않았습니다.
- 감사 로그와 OpenAPI/Swagger UI는 아직 구성하지 않았습니다.
- 이미 존재하는 OPEN 기간에 동시 마감 재시도 두 건이 겹치는 경우의 잠금 제약은 [ADR 0003](docs/adr/0003-separate-close-period-and-run.md)에 기록했습니다.

다음 개발 범위는 취소·역분개·재기표 흐름이며, 구현과 함께 ADR 0004를 작성합니다.
