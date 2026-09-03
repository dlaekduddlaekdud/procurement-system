# ADR 0002: 금액과 부가세를 라인별로 계산하고 대사는 송장 수량 기준으로 안분한다

- 상태: 수락
- 결정일: 2026-09-03

## 배경

한 문서에 여러 품목이 있으면 부가세를 각 라인에서 계산할지, 헤더 합계에서 한 번 계산할지에 따라 최종 세액이 달라진다. 소수점 처리 방식도 `DOWN`과 `HALF_UP` 중 무엇을 선택하는지에 따라 실제 금액이 달라진다.

3-Way Matching의 금액 비교 역시 같은 금액 정책이다. 여기에서 무엇을 기준액으로 삼는지가 특히 중요하다. [ADR 0001](0001-line-level-document-linkage.md)에서 부분입고와 분할송장을 지원하기로 정했으므로, 하나의 발주 라인에는 대사 시점마다 서로 다른 누적 입고수량과 누적 송장수량이 존재한다. 이때 송장 금액을 발주 라인 전액과 비교하면 정상적인 부분 진행 상태가 금액 불일치로 판정된다.

또한 미착 원장은 입고 한 건마다 `GR_ACCRUAL(+)`을 계상한다. 상계가 전량 대사 시점에만 발생하면 계상과 상계의 속도가 달라지고, 월 마감 시점에 부분 진행 중인 라인의 미착 잔액이 실제보다 크게 남는다.

## 결정

### 금액 계산

송장 금액은 각 라인에서 다음 순서로 계산하고, 헤더 금액은 라인 금액의 합으로 계산한다.

```text
lineSupplyAmount = round(quantity × unitPrice, 소수 둘째 자리, HALF_UP)
lineTaxAmount    = truncate(lineSupplyAmount × 10%, 원 단위, DOWN)
lineTotalAmount  = lineSupplyAmount + lineTaxAmount

headerSupplyAmount = SUM(lineSupplyAmount)
headerTaxAmount    = SUM(lineTaxAmount)
headerTotalAmount  = SUM(lineTotalAmount)
```

세액은 원 단위로 절사한 뒤 소수 둘째 자리로 다시 스케일을 고정한다. 저장 컬럼이 `DECIMAL(19,2)`이므로 스케일을 맞춰야 조회 값과 계산 값이 같은 형태로 비교된다.

### 대사 비교 기준

대사에는 세액을 제외한 공급가액만 사용한다. 비교 대상은 다음과 같다.

```text
receivedQuantity = SUM(POSTED GR_LINE.quantity)
invoicedQuantity = SUM(RECEIVED INVOICE_LINE.quantity)
invoicedAmount   = SUM(RECEIVED INVOICE_LINE.supplyAmount)

expectedAmount   = round(PO_LINE.unitPrice × invoicedQuantity, 소수 둘째 자리, HALF_UP)
amountDifference = ABS(invoicedAmount - expectedAmount)
amountTolerance  = MIN(ABS(expectedAmount) × 1%, 10,000원)
```

- 접수된 송장이 없으면 대사 대상이 아니므로 `HOLD_QUANTITY`로 판정한다.
- 누적 송장수량이 누적 입고수량을 초과하면 `HOLD_QUANTITY`로 판정한다.
- 누적 송장수량이 누적 입고수량 이하이면 수량 판정을 통과한다.
- 금액 차이가 허용 오차를 초과하면 `HOLD_PRICE`로 판정한다.
- 금액 차이가 허용 오차 이내이면 `MATCHED`로 판정한다. 허용 오차 경계값은 포함한다.

발주수량 대비 초과 여부는 입고 등록 단계에서 `PurchaseOrderQuantityExceededException`으로 이미 차단된다. 따라서 대사에서 발주수량을 다시 비교하지 않고 입고분과 송장분만 대조한다.

`MATCHED`는 "현재까지 접수된 송장이 입고분과 정합한다"는 뜻이며 발주 라인이 종결되었다는 뜻이 아니다. 라인 종결 여부는 `orderedQuantity = receivedQuantity = invoicedQuantity` 성립 여부로 별도 판단한다.

`expectedAmount`의 반올림 규칙은 송장 라인 공급가액과 동일해야 한다. 규칙이 다르면 같은 수량과 단가에서도 1원 단위 차이가 발생해 정상 송장이 `HOLD_PRICE`로 판정된다.

## 고려한 대안

### 대안 1: 송장 금액을 발주 라인 전액과 비교

구현은 단순하지만 부분 송장이 항상 금액 불일치가 된다. 발주 10개·1,000원 라인에 6개가 입고되고 6개가 송장 접수되면 차이 4,000원이 허용 오차 100원을 초과해 `HOLD_PRICE`로 기록된다. 정상적인 진행 상태를 가격 이상으로 오분류할 뿐 아니라, 상계가 일어나지 않아 미착 잔액이 6,000원 과대 계상된 채로 마감 스냅샷에 남는다. 부분입고를 지원하기로 한 [ADR 0001](0001-line-level-document-linkage.md)의 결정과 충돌하므로 선택하지 않았다.

### 대안 2: 누적 입고수량과 누적 송장수량의 완전 일치를 요구

수량 통제는 엄격해지지만 입고가 끝난 뒤 송장이 나누어 도착하는 정상 거래가 모두 보류된다. 입고분을 넘지 않는 송장은 대사할 수 있어야 하므로 초과분만 차단한다.

### 대안 3: 헤더 공급가액 합계에서 부가세를 한 번 계산

구현은 단순하지만 라인별 세액을 합한 값과 헤더 세액이 달라질 수 있다. 품목별 세액의 근거를 보존하고 헤더가 라인 합계와 항상 일치하도록 하기 위해 선택하지 않았다.

### 대안 4: 부가세를 원 단위에서 반올림

`RoundingMode.HALF_UP`은 일반적인 반올림 방식이지만 현재 업무 규칙인 원 단위 미만 절사와 결과가 달라진다. 세액 계산 결과를 예측 가능하게 고정하기 위해 `RoundingMode.DOWN`을 선택했다.

### 대안 5: 금액이 조금이라도 다르면 대사를 보류

수량 tolerance 0은 품목 수량을 엄격히 통제하는 데 적합하지만, 금액에는 계산·계약 과정에서 발생하는 소액 차이가 존재할 수 있다. 모든 차이를 HOLD로 처리하면 운영 비용이 커지므로 제한된 허용 오차를 둔다.

### 대안 6: 고정 금액 또는 비율 중 하나만 허용 오차로 사용

비율만 적용하면 고액 발주의 허용액이 과도하게 커지고, 고정 금액만 적용하면 소액 발주의 허용 비율이 과도하게 커진다. 두 값 중 작은 값을 사용해 두 경우를 모두 제한한다.

## 결과

### 장점

- 라인마다 세액 산출 근거가 남고 헤더 합계는 항상 라인 합계와 일치한다.
- 같은 입력은 문서 생성과 대사에서 일관된 금액으로 해석된다.
- 부분 송장이 접수 시점에 바로 상계되므로 `GR_ACCRUAL(+)`과 `INVOICE_MATCH(-)`가 같은 속도로 움직인다. 부분 진행 중인 라인도 월 마감 스냅샷에 실제 잔액으로 반영된다.
- `HOLD_PRICE`가 실제 단가·금액 이상만 가리키므로 HOLD 목록을 그대로 조치 대상으로 쓸 수 있다.

### 비용과 제약

- 원 단위 미만 세액을 라인마다 버리므로 헤더 합계에서 한 번 계산한 세액보다 총 세액이 작을 수 있다.
- 부가세율 10%를 코드 상수로 고정했다. 면세·영세율 품목과 세율 변경은 지원하지 않으며, 지원하려면 품목별 세율 기준정보가 필요하다.
- `expectedAmount`와 송장 라인 공급가액의 반올림 규칙이 함께 유지되어야 한다. 한쪽만 바꾸면 정상 송장이 보류된다.
- `MATCHED`가 라인 종결을 뜻하지 않으므로 종결 판단은 별도 조건으로 관리해야 한다.
- 허용 오차 값은 법적 기준이 아니라 이 프로젝트의 업무 정책이므로 변경 시 코드와 경계값 테스트를 함께 수정해야 한다.

## 검증 결과

- `InvoiceLineAmountsTest`에서 라인별 부가세 원 단위 절사와 라인별 계산 후 합산 결과를 검증했다.
- `InvoiceServiceTest.createsInvoiceWithLineLevelTaxAndHeaderTotals`에서 송장 헤더 금액이 라인별 공급가액·세액·합계의 합으로 저장되는지 검증했다.
- `ThreeWayMatchingPolicyTest.matchesPartialInvoiceAgainstProratedAmount`에서 부분 입고·부분 송장이 안분 기준액과 대조되어 `MATCHED`로 판정되는지 검증했다.
- `ThreeWayMatchingPolicyTest.matchesWhenInvoicedQuantityIsLessThanReceivedQuantity`에서 입고분보다 적은 송장이 보류되지 않는지 검증했다.
- `ThreeWayMatchingPolicyTest.holdsQuantityWhenInvoicedQuantityExceedsReceivedQuantity`와 `holdsQuantityWhenNoInvoiceIsReceived`에서 초과 송장과 송장 미접수가 `HOLD_QUANTITY`로 판정되는지 검증했다.
- `ThreeWayMatchingPolicyTest.holdsPriceWhenPartialInvoiceUnitPriceDiffersFromOrder`에서 부분 송장의 단가 차이가 `HOLD_PRICE`로 판정되는지 검증했다.
- `ThreeWayMatchingPolicyTest.appliesPercentageToleranceWhenItIsSmallerThanFixedTolerance`에서 1%·10,000원 허용 오차의 경계값과 초과값을 검증했다.
- `InvoiceServiceTest.settlesPartialInvoiceImmediatelyAgainstProratedAmount`에서 부분 송장이 즉시 상계되고, 부분 진행이 끝난 뒤 미착 잔액이 0으로 수렴하는지 검증했다.
- `InvoiceServiceTest.settlesOnlyNewAmountAfterPreviousPartialSettlement`에서 이미 상계한 금액을 제외하고 신규 금액만 원장에 생성되는지 검증했다.
