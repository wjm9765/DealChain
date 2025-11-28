# DealChain (딜체인) - AI 기반 안전 중고거래 전자계약 플랫폼

### 현재 백엔드 기능은 완성되었지만, 프론트엔드는 개발 중에 있습니다. 추후 실행 영상 업로드 예정입니다.
<img width="1440" height="810" alt="image" src="https://github.com/user-attachments/assets/a103aa60-987f-4278-a6cd-bc1c9977ca44" />

> **"신뢰할 수 없는 중고거래, 법적 효력을 갖춘 AI 전자계약서로 보호하다."**

[![Java](https://img.shields.io/badge/Java-17-orange?logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![AWS](https://img.shields.io/badge/AWS-Cloud-232F3E?logo=amazon-aws)](https://aws.amazon.com/)

---

## 📖 프로젝트 소개 (Project Overview)

국내 중고거래 시장은 40조 원 규모로 성장했지만, 사기 피해와 분쟁 또한 매년 10만 건 이상 발생하며 급증하고 있습니다. **DealChain**은 기존 플랫폼의 한계인 '구두 약속', 불확실한 '채팅 기록'의 위험성을 해결하고자 합니다.

생성형 AI(AWS Bedrock,sagemaker)를 활용하여 채팅 내역을 기반으로 **법적 효력이 있는 전자 계약서를 자동으로 생성**하고,  **데이터 무결성 검증 시스템**을 통해 안전한 거래 환경을 제공합니다.

### 🎯 핵심 목표
* **사기 예방:** AI가 실시간 채팅 패턴을 확인하고, 거래 위험도를 분석하여 계약서 작성을 권고
* **법적 효력:** 간편 인증 및 전자 서명을 통한 법적 근거 마련
* **무결성 보장:** 해쉬 함수, 거래 추적 테이블, key 값 기반 암호화를 적용하여 계약서의 무결성을 보장

---

## 🏗️ 시스템 아키텍처 (System Architecture)
<img width="1924" height="1638" alt="image" src="https://github.com/user-attachments/assets/c2396089-f63a-486b-a9b8-39ee96c6697b" />

### 🔒 Security & Infrastructure
* **VPC & Subnet Separation:** AWS Public/Private Subnet 분리를 통해 외부 접근을 통제합니다.
* **Data Encryption:**
    * **Data at Rest:** AWS RDS(Aurora) 및 S3 저장 시 AES-256 알고리즘을 사용하여 민감 정보를 암호화합니다.
    * **Data in Transit:** 모든 API 통신은 HTTPS(TLS 1.2+)를 적용하여 전송 구간을 보호합니다. 
* **Identity & Access Management:** Spring Security와 JWT(JSON Web Token)를 이용한 Stateless 인증 방식을 구현했습니다.

---

## ⚡ 주요 기능 (Key Features)

### 1. AI 기반 전자계약서 자동 생성
* 사용자 간의 채팅 내용을 **AWS Bedrock**의 LLM이 분석하여 계약서 초안을 자동으로 작성합니다.
* 거래 물품, 가격, 상태, 특약 사항 등을 AI가 추출하여 법률적 양식에 맞게 변환합니다.
* 전체 계약서에 대해 사용자가 이해하기 쉽게 요약 서비스를 제공하며, 각 세부 항목에 대해 어떤 채팅 내역을 근거로 생성했는지 근거를 제공합니다
<img width="890" height="490" alt="image" src="https://github.com/user-attachments/assets/b08252fc-b4de-410a-b403-e0e8ce0af88a" />

### 2. 간편 인증 및 전자 서명
* **Mock API Integration:** 실제 금융권 수준의 간편 인증 흐름을 모사한 Mock API를 구축하여 본인 인증 및 전자 서명을 수행합니다.
* 서명된 데이터는 사용자의 개인키로 암호화되어 부인 방지(Non-repudiation) 기능을 제공합니다.
<img width="616" height="630" alt="image" src="https://github.com/user-attachments/assets/d95c8b43-2d4b-44fb-928d-78b6891c0e15" />

### 3. AI 사기 탐지
* 거래 패턴과 대화 내용을 분석하여 사기 의심 징후 포착 시 경고를 보냅니다.
* 채팅 할 때마다 AI api를 호출할 시, 과도한 api 호출로 인한 서버 과부하가 발생할 수 있습니다. SQS 큐를 이용하여 비동기 메시지 처리를 하였고
* 각 roomId 별로 구분하여, 메시지를 기준으로 호출하는 것이 아닌 특정 메시지 개수만큼 쌓였을 때 room을 기준으로 기능을 수행합니다.
<img width="739" height="376" alt="image" src="https://github.com/user-attachments/assets/1af4c8e8-4f33-428c-b613-d254baa5b6ea" />

### 4. 웹소켓을 이용한 실시간 채팅 기능 구현
https://github.com/user-attachments/assets/70646d99-434a-4f6d-bfcd-d027ac9fb2d5


### 5. 거래 추적 테이블 작성
* 계약서와 관련된 모든 행위에 관해 누가 언제 어디서 어떤 행위를 했는지를 기록하고, 해쉬값을 유저 아이디로 암호화하여 저장합니다.
* Aurora PostgreSQL을 적용하여 과도한 트래픽에 대비했습니다.
<img width="740" height="526" alt="image" src="https://github.com/user-attachments/assets/d1c1eb5a-162b-4fd3-b411-2059aea09e91" />

---

## 🛠️ 기술 스택 (Tech Stack)

| Category | Technology | Description |
| --- | --- | --- |
| **Language** | Java 17 | 
| **Framework** | Spring Boot 3.x | 
| **Database** | AWS Aurora (PostgreSQL),MySQL | 
| **Security** | Spring Security, JWT | 
| **AI / ML** | AWS Bedrock, SageMaker |
| **Infrastructure** | AWS (Elastic beanstalk, S3, RDS, WAF) | 
