# Library Rental EDA Frontend Project Prompt

아래 프롬프트는 `library-rental-eda` 백엔드 프로젝트를 직접 테스트하고 연동하기 위한
`React + TypeScript + Next.js App Router` 기반 프론트엔드 프로젝트 생성 요청서입니다.

````text
React + TypeScript + Next.js App Router 기반의 Front End 프로젝트를 새로 생성해줘.

이 프로젝트는 `library-rental-eda` 백엔드의 모든 HTTP API와 Kafka 기반 EDA 흐름을 테스트하기 위한
개발자용 운영 콘솔이다. 단순 랜딩 페이지가 아니라 DDD + Hexagonal Architecture 관점으로
도메인, 유스케이스, 포트, 어댑터를 분리한 프론트엔드 애플리케이션으로 설계한다.

## 1. 프로젝트 목적

- 기존 백엔드 프로젝트 `library-rental-eda`를 직접 테스트하고 연동한다.
- 도서, 회원, 대여카드, 인기 도서 read model, Kafka 이벤트 흐름을 한 화면 체계에서 검증한다.
- 일반 사용자 서비스 UI가 아니라 백엔드 개발자, QA, 아키텍처 리뷰어가 사용하는 테스트 콘솔이다.
- API 요청 후 결과를 단정하지 않고, EDA 특성상 비동기 후속 처리가 있음을 UI에 명확히 표현한다.
- 백엔드에 없는 기능은 프론트에서 임의로 만들어내지 않는다.

## 2. 기술 스택

- Next.js App Router
- React
- TypeScript
- Tailwind CSS
- TanStack Query
- React Hook Form
- Zod
- Vitest
- React Testing Library
- ESLint
- eslint-plugin-boundaries
- Prettier
- API 통신은 공통 fetch client 기반
- 서비스별 API adapter를 분리
- Next.js route handler를 Backend-for-Frontend proxy로 사용
- UI 컴포넌트는 백엔드 URL, fetch, HTTP DTO를 직접 알지 못하게 구성

## 3. 백엔드 서비스 URL

- rental-service: `http://localhost:8080`
- book-service: `http://localhost:8081`
- member-service: `http://localhost:8082`
- bestbook-service: `http://localhost:8084`

환경 변수:

```env
BOOK_SERVICE_URL=http://localhost:8081
MEMBER_SERVICE_URL=http://localhost:8082
RENTAL_SERVICE_URL=http://localhost:8080
BESTBOOK_SERVICE_URL=http://localhost:8084
```

브라우저에서 직접 다른 포트로 호출하지 말고 Next.js route handler proxy를 사용한다.
백엔드 base URL은 route handler와 HTTP adapter에서만 참조한다.

프론트 내부 API proxy 경로:

- `/api/book/**`
- `/api/member/**`
- `/api/rental/**`
- `/api/bestbook/**`

route handler proxy 규칙:

- 파일 위치:
  - `app/api/book/[...path]/route.ts`
  - `app/api/member/[...path]/route.ts`
  - `app/api/rental/[...path]/route.ts`
  - `app/api/bestbook/[...path]/route.ts`
- 프론트 호출 경로:
  - `/api/book/1` -> `${BOOK_SERVICE_URL}/api/book/1`
  - `/api/member/Member/by-id/jenny` -> `${MEMBER_SERVICE_URL}/api/Member/by-id/jenny`
  - `/api/rental/rental-cards/jenny` -> `${RENTAL_SERVICE_URL}/api/rental-cards/jenny`
  - `/api/bestbook/books` -> `${BESTBOOK_SERVICE_URL}/api/books`
- GET, POST 모두 동일 route handler 파일에서 처리한다.
- request body와 query string은 그대로 전달한다.
- 백엔드 response body와 status code를 그대로 반환한다.
- 백엔드 에러 응답도 route handler에서 가공하지 않고 그대로 전달한다.
- route handler에는 비즈니스 로직, DTO 변환, 도메인 규칙을 넣지 않는다.
- route handler는 CORS와 service URL 은닉을 위한 proxy 역할만 담당한다.

## 4. DDD + Hexagonal Architecture 적용 원칙

프론트엔드에서도 백엔드의 bounded context를 존중한다.
이 프로젝트의 프론트 아키텍처는 화면 폴더 중심이 아니라 도메인 경계와 의존성 방향을 먼저 정한다.

bounded context:

- `book`
- `member`
- `rental`
- `bestbook`
- `event-flow`

최종 권장 구조는 다음을 우선한다.

```text
src/
  app/                                  # Next.js App Router, 최외부 composition
    layout.tsx
    page.tsx
    books/
    members/
    rental/
    bestbooks/
    event-flow/
    api/
      book/[...path]/route.ts           # book-service proxy
      member/[...path]/route.ts         # member-service proxy
      rental/[...path]/route.ts         # rental-service proxy
      bestbook/[...path]/route.ts       # bestbook-service proxy
  domain/                               # 순수 도메인, React/Next/fetch 의존 금지
    shared/
      types/
      value-objects/
    book/
      entities/
      value-objects/
      enums/
      ports/
        driven/
    member/
      entities/
      value-objects/
      enums/
      ports/
        driven/
    rental/
      entities/
      value-objects/
      enums/
      ports/
        driven/
    bestbook/
      entities/
      ports/
        driven/
    event-flow/
      entities/
  application/                          # use case, command, result
    book/
      commands/
      results/
      use-cases/
    member/
      commands/
      results/
      use-cases/
    rental/
      commands/
      results/
      use-cases/
    bestbook/
      commands/
      results/
      use-cases/
    event-flow/
      use-cases/
  adapters/
    driven/                             # outbound adapter: backend API 호출
      http/
        fetch-client.ts
        api-error-handler.ts
        base-response.ts
      book/
        http-book-repository.ts
        mappers/
      member/
        http-member-repository.ts
        mappers/
      rental/
        http-rental-repository.ts
        mappers/
      bestbook/
        http-bestbook-repository.ts
        mappers/
    driving/                            # inbound adapter: UI hook/form schema
      book/
      member/
      rental/
      bestbook/
      event-flow/
  di/
    container.ts                        # repository + use case composition root
    providers.tsx                       # React provider
  shared/
    config/
    query/
    ui/
    errors/
    recent-cache/
```

의존성 방향:

```text
app/ui -> adapters/driving -> application -> domain
adapters/driven -> application/domain ports
domain -> no dependency outward
```

금지 규칙:

- `domain`은 React, Next.js, fetch, TanStack Query, Tailwind, browser API를 import 하지 않는다.
- `application`은 React component, route handler, HTTP DTO, fetch client를 import 하지 않는다.
- `ui`는 backend response DTO를 직접 사용하지 않고 domain/application result만 사용한다.
- bounded context 간 domain import를 금지한다. 여러 context 조합은 page 또는 workflow UI에서만 수행한다.
- 백엔드 DTO 필드명 변경은 mapper에서 흡수한다. UI로 DTO를 그대로 흘리지 않는다.

### Domain Layer

도메인 레이어는 백엔드 응답 DTO를 그대로 노출하지 않는다.
프론트 화면에서 사용하는 핵심 개념을 타입으로 표현한다.

예:

- `Book`
- `BookStatus`
- `Member`
- `RentalCard`
- `RentItem`
- `ReturnItem`
- `BestBook`
- `TopicFlow`

가능하면 다음 value object를 둔다.

- `MemberId`
- `Email`
- `BookNo`
- `BookTitle`
- `Isbn`
- `RentalCardNo`
- `Point`
- `LateFee`

프론트 도메인 규칙은 백엔드 비즈니스 규칙을 재구현하지 않는다.
다만 화면 입력 검증과 상태 표시를 위해 필요한 불변성, 포맷 검증, enum 매핑 정도만 둔다.

### Application Layer

application 레이어는 화면 이벤트를 처리하는 use case class 또는 함수, command, result를 둔다.
하나의 사용자 시나리오를 하나의 use case로 표현한다.

예:

- `registerBook`
- `findBookByNo`
- `makeBookAvailable`
- `registerMember`
- `findMemberById`
- `saveMemberPoint`
- `createRentalCard`
- `rentBook`
- `returnBook`
- `markOverdue`
- `clearOverdue`
- `loadBestBooks`

application 레이어는 API 구현체에 직접 의존하지 않고 port interface에 의존한다.

예:

```ts
export interface BookRepositoryPort {
  register(input: RegisterBookCommand): Promise<Book>;
  findByNo(no: number): Promise<Book>;
  makeAvailable(no: number): Promise<Book>;
  makeUnavailable(no: number): Promise<Book>;
}
```

Use case는 TanStack Query를 알지 않는다. TanStack Query 연결은 `adapters/driving` hook에서 담당한다.

### Outbound Adapter

`adapters/driven`은 백엔드 HTTP API를 호출하고, 응답 DTO를 프론트 도메인 타입으로 변환한다.

예:

- `HttpBookRepository`
- `HttpMemberRepository`
- `HttpRentalRepository`
- `HttpBestBookRepository`

이 레이어에서만 `BaseResponse<T>` unwrap, HTTP status 처리, API DTO 변환을 수행한다.
Mapper는 anti-corruption layer 역할을 한다.

예:

```ts
// API DTO: { memberNo: 1, id: "jenny", name: "제니", ... }
// Domain: Member { memberNo, memberId: MemberId, name, email: Email, point: Point }
```

route handler proxy는 이 레이어의 HTTP client가 호출한다.

### Inbound Adapter

`adapters/driving`은 React custom hook과 form schema를 둔다.

예:

- `useBook`
- `useRegisterBook`
- `useMemberById`
- `useSavePoint`
- `useRentalCard`
- `useRentItem`
- `useBestBooks`

이 레이어에서 React Hook Form + Zod schema를 사용해 command를 만든다.
UI 컴포넌트는 fetch를 직접 호출하지 않고 driving hook 또는 application use case를 통해 호출한다.

### Shared Layer

공통 관심사는 `shared`에 둔다.

- `shared/query`: QueryClient 설정
- `shared/ui`: 버튼, 입력, 테이블, 상태 배지, 에러 패널
- `shared/config`: 서비스 URL, proxy path
- `shared/errors`: API error 타입
- `shared/recent-cache`: 목록 API가 없는 리소스를 위한 최근 조회/등록 cache

### Recent Cache 규칙

book-service와 member-service에는 전체 목록 조회 API가 없다.
따라서 목록처럼 보이는 화면은 mock 데이터가 아니라 recent cache를 사용한다.

- recent cache는 Zustand store로 구현한다.
- 브라우저 새로고침 후에도 유지되도록 localStorage persist를 적용한다.
- 등록 성공 시 해당 리소스를 cache에 추가한다.
- 단건 조회 성공 시 해당 리소스를 cache에 추가하거나 최신 값으로 갱신한다.
- cache key:
  - book: `book.no`
  - member: `member.id`
- 최대 50건까지 유지한다.
- 50건을 초과하면 오래된 항목부터 제거한다.
- 도서 목록 화면은 recent cache 도서를 테이블로 표시하고, “도서 번호로 조회” 입력을 함께 제공한다.
- 회원 목록 화면은 recent cache 회원을 테이블로 표시하고, “회원 ID로 조회” 입력을 함께 제공한다.
- recent cache 데이터는 서버 전체 목록이 아니라 “이 브라우저에서 최근 등록/조회한 항목”임을 UI에 표시한다.

### DI Composition Root

`di/container.ts`에서 repository 구현체와 use case 인스턴스를 생성한다.
`di/providers.tsx`에서 React Context로 use case를 주입한다.
테스트에서는 mock repository를 주입할 수 있어야 한다.

### TanStack Query 규칙

- Query Key는 `['context', 'resource', ...params]` 형식을 사용한다.
- 예: `['member', 'detail', memberId]`, `['book', 'detail', no]`, `['rental', 'card', memberId]`
- mutation 성공 시 관련 query를 invalidate 한다.
- 대여 성공 후 invalidate 예:
  - `['rental', 'card', memberId]`
  - `['book', 'detail', itemId]`
  - `['member', 'by-id', memberId]`
  - `['bestbook', 'list']`

### Import Boundary 검증

가능하면 `eslint-plugin-boundaries` 또는 path alias 규칙으로 잘못된 import를 감지한다.

path alias 예:

```json
{
  "@domain/*": ["src/domain/*"],
  "@application/*": ["src/application/*"],
  "@adapters/*": ["src/adapters/*"],
  "@di/*": ["src/di/*"],
  "@shared/*": ["src/shared/*"]
}
```

## 5. 공통 응답 형식

백엔드는 모든 API 응답을 다음 형식으로 감싼다.

```ts
type BaseResponse<T> = {
  code: number;
  message: string;
  data: T;
};
```

프론트의 API adapter는 `BaseResponse<T>`를 받아 `data`를 반환하되,
화면에는 필요한 경우 `message`, `code`도 보여줄 수 있게 metadata를 보존한다.

fetch client 동작 규칙:

- 응답이 2xx이면 JSON을 파싱한 뒤 `BaseResponse<T>`의 `data`를 반환한다.
- 필요하면 `data`, `code`, `message`를 함께 담은 `ApiSuccess<T>` helper를 별도로 제공할 수 있다.
- 응답이 4xx/5xx이면 백엔드 error response를 파싱해 `ApiError`를 throw 한다.
- `ApiError` 구조:

```ts
type ApiError = {
  status: number;
  code?: string | number;
  message: string;
  fieldErrors?: Array<{
    field: string;
    message: string;
  }>;
};
```

- network error, timeout, JSON parse error는 `NetworkError` 또는 `UnexpectedApiError`로 감싸서 throw 한다.
- driven adapter(repository)는 필요하면 `ApiError`를 application/domain에서 이해할 수 있는 error로 변환한다.
- UI는 raw `Response` 객체나 backend DTO wrapper에 직접 의존하지 않는다.

폼 에러 매핑 규칙:

- 백엔드 validation error에 `fieldErrors`가 포함되어 있으면 React Hook Form의 `setError`로 각 필드에 서버 에러 메시지를 표시한다.
- 예: `fieldErrors: [{ field: "email", message: "올바른 이메일 형식이 아닙니다." }]`
- 매핑: `setError("email", { message: "올바른 이메일 형식이 아닙니다." })`
- request DTO와 form field 이름이 다르면 form schema 또는 mapper에서 명시적으로 변환한다.

## 6. API 기능 명세

### 6.1 Book Service

서비스:

- port: `8081`
- base path: `/api/book`

도메인:

- 도서 등록
- 도서 단건 조회
- 도서 대여 가능 상태 수동 변경

enum:

- `BookStatus`: `ENTERED` | `AVAILABLE` | `UNAVAILABLE`
- `Source`: `DONATION` | `SUPPLY`
- `Classification`: `ARTS` | `COMPUTER` | `LITERATURE`
- `Location`: `JEONGJA` | `PANGYO`

API:

- `POST /api/book`
  - 기능: 도서 등록
  - request:
    - `title: string`
    - `description: string`
    - `author: string`
    - `isbn: string`
    - `publicationDate: string`
    - `source: "DONATION" | "SUPPLY"`
    - `classification: "ARTS" | "COMPUTER" | "LITERATURE"`
    - `location: "JEONGJA" | "PANGYO"`
  - response data:
    - `no`
    - `title`
    - `description`
    - `author`
    - `isbn`
    - `publicationDate`
    - `source`
    - `classification`
    - `bookStatus`
    - `location`

- `GET /api/book/{no}`
  - 기능: 도서 번호로 단건 조회

- `POST /api/book/{no}/available`
  - 기능: 도서를 `AVAILABLE` 상태로 수동 변경

- `POST /api/book/{no}/unavailable`
  - 기능: 도서를 `UNAVAILABLE` 상태로 수동 변경

주의:

- 현재 book-service에는 전체 목록 조회 API가 없다.
- 도서 목록 화면은 등록/조회한 도서를 프론트 recent cache에 누적해 보여준다.

### 6.2 Member Service

서비스:

- port: `8082`
- base path: `/api/Member`

주의:

- base path는 `/api/member`가 아니라 대문자 `M`을 포함한 `/api/Member`다.

도메인:

- 회원 등록
- 회원 단건 조회
- 포인트 수동 적립
- 포인트 수동 사용

enum:

- `UserRole`: `ADMIN` | `USER`

API:

- `POST /api/Member/`
  - 기능: 회원 등록
  - request:
    - `id: string`
    - `name: string`
    - `passWord: string`
    - `email: string`
  - response data:
    - `memberNo`
    - `id`
    - `name`
    - `email`
    - `authorities`
    - `point`

- `GET /api/Member/{no}`
  - 기능: 회원 번호로 단건 조회

- `GET /api/Member/by-id/{id}`
  - 기능: 회원 로그인 ID로 단건 조회

- `POST /api/Member/{id}/points/save`
  - 기능: 회원 포인트 수동 적립
  - request:
    - `point: number`

- `POST /api/Member/{id}/points/use`
  - 기능: 회원 포인트 수동 사용
  - request:
    - `point: number`

주의:

- 현재 member-service에는 전체 목록 조회 API가 없다.
- 회원 목록 화면은 등록/조회한 회원을 프론트 recent cache에 누적해 보여준다.

### 6.3 Rental Service

서비스:

- port: `8080`
- base path: `/api/rental-cards`

도메인:

- 대여카드 생성
- 대여카드 조회
- 도서 대여 요청
- 도서 반납 요청
- 연체 표시
- 연체료 정산 요청

enum:

- `RentStatus`: `RENT_AVAILABLE` | `RENT_UNAVAILABLE`

API:

- `POST /api/rental-cards`
  - 기능: 회원 대여카드 생성 또는 기존 카드 반환
  - request:
    - `userId: string`
    - `userNm: string`
  - response data:
    - `rentalCardNo`
    - `userId`
    - `userNm`
    - `rentStatus`
    - `lateFee`
    - `rentItems`
    - `returnItems`

- `GET /api/rental-cards/{memberId}`
  - 기능: 회원 ID로 대여카드 조회

- `GET /api/rental-cards/{memberId}/rent-items`
  - 기능: 현재 대여 중인 도서 목록 조회
  - response item:
    - `itemId`
    - `itemTitle`
    - `rentDate`
    - `overdue`
    - `overdueDate`

- `GET /api/rental-cards/{memberId}/return-items`
  - 기능: 반납 완료 도서 목록 조회
  - response item:
    - `itemId`
    - `itemTitle`
    - `rentDate`
    - `returnDate`

- `POST /api/rental-cards/rent`
  - 기능: 도서 대여 요청 접수
  - request:
    - `userId`
    - `userNm`
    - `itemId`
    - `itemTitle`
  - response status: `202 Accepted`
  - response data:
    - `message`
    - `rentalCardNo`
    - `userId`
    - `userNm`
    - `rentStatus`
    - `lateFee`
    - `rentItems`
    - `returnItems`

- `POST /api/rental-cards/return`
  - 기능: 도서 반납 요청 접수
  - request:
    - `userId`
    - `userNm`
    - `itemId`
    - `itemTitle`
  - response status: `202 Accepted`

- `POST /api/rental-cards/overdue`
  - 기능: 대여 중인 도서를 연체 상태로 표시
  - request:
    - `userId`
    - `userNm`
    - `itemId`
    - `itemTitle`

- `POST /api/rental-cards/clear-overdue`
  - 기능: 연체료 정산 요청 접수
  - request:
    - `userId`
    - `userNm`
    - `point`
  - response status: `202 Accepted`

주의:

- `rent`, `return`, `clear-overdue`는 비동기 EDA 흐름을 시작한다.
- `202 Accepted`는 최종 성공이 아니라 요청 접수다.
- UI에는 “요청 접수됨. 관련 서비스 상태를 새로고침해 확인하세요.”라고 표시한다.

### 6.4 BestBook Service

서비스:

- port: `8084`
- base path: `/api/books`

도메인:

- 인기 도서 read model 조회
- 수동 인기 도서 집계 테스트

API:

- `GET /api/books`
  - 기능: 인기 도서 read model 전체 조회
  - response data item:
    - `id`
    - `itemNo`
    - `itemTitle`
    - `rentCount`

- `GET /api/books/{id}`
  - 기능: 인기 도서 read model ID로 단건 조회

- `POST /api/books`
  - 기능: 수동 테스트용 인기 도서 대여 집계 반영
  - request:
    - `itemNo`
    - `itemTitle`
  - response data: `null`

주의:

- `/api/books`는 bestbook-service API다.
- book-service의 도서 API는 `/api/book`이고 포트도 다르다.

## 7. Kafka / EDA 흐름 메타데이터

프론트는 Kafka를 직접 구독하지 않는다.
대신 API 요청 후 어떤 이벤트가 발생하는지 설명하고, 관련 read/query API refresh를 제공한다.

```yaml
topicMap:
  rental_rent:
    messageType: ItemRented
    producer: rental-service RentalKafkaEventProducer#publishRentalEvent
    consumers:
      - book-service BookEventConsumer#consumeRent
      - member-service MemberEventConsumer#consumeRent
      - bestbook-service BestBookEventConsumer#consumeRent
    userMeaning: 도서 대여 요청 후 도서 상태 변경, 회원 포인트 적립, 인기 도서 집계가 이어진다.

  rental_return:
    messageType: ItemReturned
    producer: rental-service RentalKafkaEventProducer#publishReturnEvent
    consumers:
      - book-service BookEventConsumer#consumeReturn
      - member-service MemberEventConsumer#consumeReturn
    userMeaning: 도서 반납 요청 후 도서 상태 복구와 회원 포인트 적립이 이어진다.

  overdue_clear:
    messageType: OverdueCleared
    producer: rental-service RentalKafkaEventProducer#publishOverdueClearEvent
    consumers:
      - member-service MemberEventConsumer#consumeClear
    userMeaning: 연체료 정산 요청 후 회원 포인트 차감이 이어진다.

  rental_result:
    messageType: EventResult
    producers:
      - book-service BookKafkaEventProducer#publish
      - member-service MemberKafkaEventProducer#publish
    consumer: rental-service RentalEventConsumer#consumeRentalResult
    userMeaning: 참여 서비스 처리 결과를 rental-service가 받아 보상 여부를 판단한다.

  point_use:
    messageType: PointUseCommand
    producer: rental-service RentalKafkaEventProducer#publishPointUseCommand
    consumer: member-service MemberEventConsumer#consumeUsePoint
    userMeaning: 보상 흐름에서 이미 적립된 회원 포인트를 되돌린다.

  rent_cancel:
    messageType: ItemRentCanceled
    producer: rental-service RentalKafkaEventProducer#publishRentCanceledEvent
    consumers:
      - book-service BookEventConsumer#consumeRentCanceled
      - bestbook-service BestBookEventConsumer#consumeRentCanceled
    userMeaning: 대여 실패 보상으로 도서 상태와 인기 도서 집계를 되돌린다.

  return_cancel:
    messageType: ItemReturnCanceled
    producer: rental-service RentalKafkaEventProducer#publishReturnCanceledEvent
    consumer: book-service BookEventConsumer#consumeReturnCanceled
    userMeaning: 반납 실패 보상으로 도서 상태를 대여 불가로 되돌린다.
```

## 8. 사용자 워크플로우

### 테스트 데이터 준비

1. 회원 등록
2. 도서 등록
3. 도서를 `AVAILABLE` 상태로 변경
4. 회원 대여카드 생성
5. 회원, 도서, 대여카드 상태 확인

### 도서 대여 흐름

1. 회원 ID와 도서 번호를 선택
2. `POST /api/rental-cards/rent` 호출
3. 화면에 `202 Accepted`와 접수 메시지 표시
4. 도서 조회 refresh: `UNAVAILABLE` 확인
5. 회원 조회 refresh: 포인트 적립 확인
6. 대여카드 refresh: rentItems 확인
7. 인기 도서 refresh: rentCount 증가 확인

### 도서 반납 흐름

1. 대여 중인 도서 선택
2. `POST /api/rental-cards/return` 호출
3. 화면에 접수 메시지 표시
4. 도서 조회 refresh: `AVAILABLE` 확인
5. 회원 조회 refresh: 포인트 적립 확인
6. 대여카드 refresh: returnItems 확인

### 연체 처리 흐름

1. 대여 중인 도서 선택
2. `POST /api/rental-cards/overdue` 호출
3. rentItems에서 `overdue=true` 확인
4. lateFee 확인

### 연체료 정산 흐름

1. 회원 ID, 이름, 정산 포인트 입력
2. `POST /api/rental-cards/clear-overdue` 호출
3. 화면에 `202 Accepted` 표시
4. 회원 조회 refresh: 포인트 차감 확인
5. 대여카드 refresh: 연체 상태/lateFee 확인

## 9. UI 화면 구성

- `/`
  - 대시보드
  - 서비스별 API base URL 상태
  - 최근 API 요청 결과
  - 빠른 테스트 워크플로우 시작 버튼

- `/books`
  - 도서 등록 폼
  - 도서 번호 조회
  - AVAILABLE / UNAVAILABLE 수동 변경
  - 최근 조회/등록 도서 테이블

- `/members`
  - 회원 등록 폼
  - 회원 번호 조회
  - 회원 ID 조회
  - 포인트 적립/사용 폼
  - 최근 조회/등록 회원 테이블

- `/rental`
  - 대여카드 생성/조회
  - 대여 중 도서 목록
  - 반납 완료 도서 목록
  - 도서 대여 요청 폼
  - 도서 반납 요청 폼
  - 연체 처리 폼
  - 연체료 정산 폼

- `/bestbooks`
  - 인기 도서 목록 조회
  - 인기 도서 단건 조회
  - 수동 집계 테스트 폼

- `/event-flow`
  - Kafka topic producer/consumer 표
  - 대여/반납/연체/보상 흐름 timeline
  - 직접 Kafka 구독은 하지 않는다.
  - API 호출 후 어떤 후속 이벤트가 기대되는지 설명한다.

## 10. UX 요구사항

- 운영 콘솔처럼 조용하고 밀도 있는 UI로 만든다.
- 좌측 사이드바 + 상단 상태바 구조를 사용한다.
- 과한 마케팅 UI, 히어로 섹션, 랜딩 페이지를 만들지 않는다.
- 모든 주요 API 호출에 loading, error, empty state를 구현한다.
- 성공/실패는 toast로 표시하되, 중요한 결과는 화면 패널에도 남긴다.
- `202 Accepted` 응답은 최종 성공으로 표현하지 않는다.
- “요청 접수됨, 관련 조회를 새로고침해 후속 처리 결과를 확인하세요.”라고 표현한다.
- 같은 `memberId`, `itemNo`, `itemTitle`을 반복 입력하지 않도록 최근 입력값을 저장한다.
- API가 없는 목록 조회는 mock으로 꾸미지 말고 recent cache 또는 “현재 API 없음”으로 표현한다.
- 대시보드에는 실제 전체 count API가 없다는 사실을 표시하고, recent cache 또는 bestbook list처럼 가능한 데이터만 요약한다.
- 도서 분류, 소장 위치, 입수 경로, 상태 값은 select, badge, segmented control 등 의미 있는 UI로 표현한다.
- 대여카드 상세 화면은 작업 콘솔처럼 구성한다. 대여 중 도서 목록에서는 반납/연체 액션을 바로 실행할 수 있어야 한다.

## 11. 구현 산출물

다음 순서로 구현한다.

### A. 프로젝트 초기 설정

- `package.json`
- `tsconfig.json` strict 설정
- path alias 설정: `@domain/*`, `@application/*`, `@adapters/*`, `@di/*`, `@shared/*`
- `next.config.js`
- `tailwind.config.ts`
- `.env.local.example`
- ESLint, Prettier, boundaries rule

### B. Domain Layer

- shared 타입: `BaseResponse<T>`, error response, API metadata
- context별 entity, value object, enum
- repository port interface
- event-flow domain metadata

### C. Application Layer

- context별 command
- context별 result
- context별 use case
- use case는 repository port만 의존

### D. Route Handler Proxy

- `app/api/book/[...path]/route.ts`
- `app/api/member/[...path]/route.ts`
- `app/api/rental/[...path]/route.ts`
- `app/api/bestbook/[...path]/route.ts`
- GET, POST proxy 처리
- status code, response body, error body 그대로 전달

### E. Driven Adapters

- service별 HTTP repository 구현체
- fetch client
- BaseResponse unwrap
- API error handler
- API DTO 타입
- DTO to domain mapper

### F. Driving Adapters

- context별 TanStack Query hook
- mutation hook
- React Hook Form + Zod schema
- mutation 성공 시 query invalidation
- backend `fieldErrors`를 React Hook Form `setError`로 매핑

### G. Recent Cache

- Zustand store
- localStorage persist
- 최대 50건 유지
- 등록/조회 성공 시 갱신

### H. DI

- `container.ts`
- `providers.tsx`
- 테스트용 mock repository로 교체 가능한 구조

### I. UI

- root layout
- sidebar
- top status bar
- dashboard
- books page
- members page
- rental page
- bestbooks page
- event-flow page
- common component: Button, Input, Select, Table, Badge, EmptyState, ErrorPanel, LoadingSpinner, Toast

### J. Tests

- domain value object test
- enum mapper test
- use case test with mock repository
- API mapper test
- hook test with mocked use case or repository

## 12. 테스트 요구사항

최소 다음 테스트를 포함한다.

- Domain Layer
  - `Email` 형식 검증
  - `Point` 음수 불가
  - enum 문자열 매핑

- Application Layer
  - use case가 repository port를 올바르게 호출하는지 검증
  - 실패 시 error가 UI 레이어까지 표현 가능한 형태로 전파되는지 검증

- Adapter Layer
  - `BaseResponse<T>` unwrap
  - API DTO에서 domain entity로 변환하는 mapper
  - HTTP error를 `ApiError` 또는 `DomainError`로 변환

- UI/Hook Layer
  - TanStack Query hook loading/success/error 상태
  - mutation 성공 후 invalidate 동작
  - form validation

명령어:

```bash
npm run lint
npm run typecheck
npm run test
npm run build
```

## 13. 코드 컨벤션

- 파일명은 kebab-case를 사용한다. 예: `rental-card.ts`, `http-member-repository.ts`
- 타입, 인터페이스, enum은 PascalCase를 사용한다.
- 함수와 변수는 camelCase를 사용한다.
- Use Case 이름은 `{Verb}{Noun}UseCase` 형식을 사용한다.
- Hook 이름은 `use{Action}` 형식을 사용한다.
- 도메인 용어는 백엔드의 ubiquitous language를 따른다.
- 약어를 남발하지 않는다.
- Entity와 Value Object는 class 또는 readonly object로 불변성을 표현한다.
- UI 컴포넌트는 비즈니스 규칙을 가지지 않는다.
- 복잡한 도메인 규칙, adapter 변환, EDA 흐름 설명에만 한국어 주석을 사용한다.

## 14. 수동 검증 시나리오

프론트엔드 완성 후 다음 시나리오를 UI에서 순서대로 실행할 수 있어야 한다.

1. 회원 등록: `jenny`, `제니`, `1111`, `jenny@example.com`
2. 회원 ID 조회: `jenny`
3. 도서 등록: `누구를 위하여 종은 울리나`, `고전 소설`, `헤밍웨이`, ISBN, 발행일, 분류/위치/입수경로 선택
4. 도서 조회 후 `AVAILABLE` 상태로 변경
5. 대여카드 생성: `jenny`, `제니`
6. 도서 대여 요청: `itemId`, `itemTitle`, `jenny`, `제니`
7. `202 Accepted` 접수 메시지 확인
8. 대여카드 조회로 rentItems 확인
9. 회원 조회로 포인트 적립 확인
10. 도서 조회로 `UNAVAILABLE` 확인
11. 인기 도서 목록 조회로 rentCount 증가 확인
12. 도서 반납 요청
13. 대여카드 조회로 returnItems 확인
14. 도서 조회로 `AVAILABLE` 복원 확인
15. 대여 중 도서가 있다면 연체 표시와 연체료 정산 흐름을 실행

## 15. README 작성 요구사항

README에는 다음 내용을 반드시 포함한다.

- 프로젝트 목적
- 기술 스택
- DDD + Hexagonal Architecture 레이어 설명
- 의존성 방향과 import 금지 규칙
- 환경 변수
- 백엔드 서비스 포트
- 실행 방법
- 테스트 방법
- 수동 검증 시나리오
- API가 없는 기능과 recent cache 사용 이유
- EDA 흐름에서 `202 Accepted`가 최종 성공이 아니라는 설명

## 16. 완료 조건

- [ ] `npm install` 에러 없음
- [ ] `npm run dev` 실행 시 `http://localhost:3000` 접속 가능
- [ ] `npm run lint` 성공
- [ ] `npm run typecheck` 성공
- [ ] `npm run test` 성공
- [ ] `npm run build` 성공
- [ ] `/api/book/1` 호출 시 route handler가 `http://localhost:8081/api/book/1`로 프록시
- [ ] `/api/member/Member/by-id/jenny` 호출 시 route handler가 `http://localhost:8082/api/Member/by-id/jenny`로 프록시
- [ ] `/api/rental/rental-cards/jenny` 호출 시 route handler가 `http://localhost:8080/api/rental-cards/jenny`로 프록시
- [ ] `/api/bestbook/books` 호출 시 route handler가 `http://localhost:8084/api/books`로 프록시
- [ ] 모든 백엔드 HTTP endpoint에 대응하는 화면 또는 액션 존재
- [ ] README에 실행 방법, 환경 변수, 백엔드 서비스 포트, 테스트 순서 작성
- [ ] 백엔드 API가 없는 기능은 임의 구현하지 않고 명확히 표시
- [ ] DDD + Hexagonal Architecture 레이어 규칙이 README에 정리되어 있음
- [ ] `domain` 폴더에 React, fetch, Next.js, TanStack Query import가 없음
- [ ] `application` 폴더에 React component, route handler, HTTP DTO, fetch client import가 없음
- [ ] UI가 backend DTO, fetch client, route handler 구현에 직접 의존하지 않음
- [ ] API mapper와 use case 테스트가 포함됨
- [ ] backend `fieldErrors`가 React Hook Form field error로 표시됨
- [ ] recent cache가 최대 50건까지만 유지됨
````
