INSERT INTO department (code, name, description)
VALUES ('PURCHASE', '구매팀', '구매요청 승인과 발주 업무 담당'),
       ('FINANCE', '재무팀', '송장 검토와 월 마감 업무 담당'),
       ('IT', 'IT팀', '시스템과 IT 품목 구매요청 부서');

INSERT INTO warehouse (code, name, address)
VALUES ('WH-SEOUL', '서울 중앙 창고', '서울특별시'),
       ('WH-BUSAN', '부산 창고', '부산광역시');

INSERT INTO `role` (code, name, description)
VALUES ('REQUESTER', '구매 요청자', '소속 부서의 구매요청 작성 및 조회'),
       ('BUYER', '구매 담당자', '구매요청 승인과 발주 처리'),
       ('ADMIN', '관리자', '기준정보 관리와 월 마감 실행');
