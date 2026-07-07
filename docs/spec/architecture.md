# 아키텍처와 패키지 구조

- 문서 상태: 기준
- 작성일: 2026-05-13
- 상위 문서: [Nyang-Nyang Bot Spec](index.md)
- 관련 문서:
  - [클린 아키텍처 채택 결정](decision-clean-architecture.md)
  - [데이터 모델/마이그레이션 계획](data-model-migration.md)
  - [HTTP 라우트 명세](api.md)
  - [이벤트 명세](events.md)
  - [테스트 전략](test-strategy.md)

## 1. 기준

Nyang-Nyang Bot은 포트/어댑터 기반 클린 아키텍처를 따른다.

루트 패키지는 네 개만 허용한다.

```text
org.nowstart.nyangnyangbot
  adapter
  application
  config
  domain
```

의존성 방향은 아래 방향만 허용한다.

```text
adapter -> application -> domain
config  -> adapter/application/domain
```

역방향 의존은 금지한다.

```text
domain      -X-> application
domain      -X-> adapter
application -X-> adapter
adapter/in  -X-> adapter/out
```

코드 작성 기준:

- 반복적인 생성자 주입, logger 선언, JPA entity getter/build constructor, utility class private constructor는 Lombok을 사용한다.
- 단순 데이터 전달은 Java `record`를 우선 사용하고, Lombok DTO/class는 상태나 프레임워크 제약 때문에 record가 맞지 않을 때만 사용한다.
- 불변식 검증, 의존성 변환, `@Value` 같은 생성자 파라미터 애노테이션이 필요한 경우에는 명시적 생성자를 유지한다.

## 2. 레이어 책임

| 레이어 | 책임 | 포함 대상 |
| --- | --- | --- |
| `domain` | 비즈니스 개념, 상태, 정책, 순수 규칙 | `FavoriteAccount`, `RoulettePolicy`, `RouletteTable` |
| `application` | 유스케이스, 트랜잭션, 포트 호출, 흐름 조합 | `ManageRouletteService`, `FavoriteLedgerService`, `*UseCase`, `*Port` |
| `adapter/in` | 외부 입력을 application use case로 변환 | Web controller, socket listener entrypoint |
| `adapter/out` | application outbound port 구현 | JPA persistence adapter, Feign/external adapter |
| `config` | Spring 설정, 보안, DI, 운영 설정 | `SecurityConfig`, `FeignConfig`, property |

## 3. 패키지 구조

기준 구조는 다음과 같다.

```text
org.nowstart.nyangnyangbot
  domain
    authorization
    exception
    favorite
    overlay
    roulette
    type
    upbo
    weeklychat

  application
    port
      in
        {feature}
      out
        {feature}
    service
      {feature}

  adapter
    in
      web
        {feature}
    out
      external
        chzzk
        google
      persistence
        common
        {persistence-subject}

  config
    property
    web
```

금지 패키지는 다음과 같다.

```text
domain/model
application/exception
application/model
application/gateway
application/port/in/{feature}/usecase
application/port/out/{feature}/repository
adapter/in/web/common
adapter/out/persistence/entity
adapter/out/persistence/repository
common/exception
global/exception
controller
service
repository
data
```

## 4. Domain 패키지 규칙

`domain`은 기능 이름으로 나눈다. `model` 같은 범용 패키지는 만들지 않는다. 저장/조회 row 형태의 데이터 record는 domain에 두지 않고 해당 `application/port/out/{feature}`의 `*Port` 내부 `*Result`로 둔다.

```text
domain
  attendance
    AttendanceUserState
  chat
    ChatCommandCooldown
  favorite
    FavoriteAccount
    FavoriteLedgerEntry
    FavoriteSourceType
  roulette
    RoulettePolicy
    RouletteActivationValidation
    RouletteItemSnapshot
  upbo
    UpboPolicy
  overlay
    OverlayTokenPolicy
  exception
    NyangNyangException
    ErrorCode
  type
    RewardType
    ConversionMode
    RouletteEventStatus
```

규칙:

- domain은 Spring, JPA, Servlet, Feign, Jackson에 의존하지 않는다.
- domain에는 `@Entity`, `@Service`, `@Transactional`, `@Controller`를 두지 않는다.
- domain 클래스 이름에는 DB 관점의 `Entity` 접미사를 쓰지 않는다.
- 공통 enum이 여러 기능에서 쓰이면 `domain/type`에 둔다.
- 특정 기능 안에서만 쓰는 enum이나 값 객체는 해당 기능 패키지에 둔다.
- 프로젝트 공통 예외와 에러 코드는 `domain/exception`에 둔다.
- `domain/exception`은 HTTP status, JPA, Feign, Servlet, Spring Security 같은 기술 정보를 알지 않는다.

## 5. Application 패키지 규칙

Application은 inbound port, outbound port, service 구현으로 나눈다.

```text
application
  port
    in
      roulette
        ManageRouletteUseCase
        QueryRouletteResultUseCase
        ProcessRouletteDonationUseCase
    out
      roulette
        RoulettePort
  service
    roulette
      ManageRouletteService
      QueryRouletteResultService
      ProcessRouletteDonationService
      RouletteEventStatusService
      RouletteRoundApplyService
    upbo
      ManageUpboService
      QueryUpboService
```

규칙:

- `application/service/{feature}`는 inbound use case 구현체를 둔다.
- 한 application service가 여러 inbound use case를 동시에 구현하지 않는다. `ManageRouletteUseCase`, `QueryRouletteResultUseCase`, `ProcessRouletteDonationUseCase`처럼 유스케이스가 나뉘면 구현체도 `ManageRouletteService`, `QueryRouletteResultService`, `ProcessRouletteDonationService`로 나눈다.
- DB, 외부 API, 트랜잭션이 필요 없는 검증/계산/상태 판정은 service에 두지 않고 `domain/{feature}`의 policy나 값 객체로 올린다.
- inbound port는 `application/port/in/{feature}`에 `*UseCase` 파일을 직접 둔다.
- outbound port는 `application/port/out/{feature}`에 `*Port` 파일을 직접 둔다.
- outbound port는 책임 단위로 나눈다. 하나의 `*Port`에 저장, 조회, 외부 호출 책임을 모두 몰아넣지 않는다.
- `application/port/in/{feature}/usecase`, `application/port/out/{feature}/repository` 같은 2차 분류 패키지는 만들지 않는다.
- 외부에서 application을 호출할 때는 `application.port.in.{feature}.*UseCase`를 본다.
- adapter는 `application.service.*Service` 구현체를 직접 주입하지 않는다.
- application service가 DB나 외부 API가 필요하면 `application.port.out.{feature}.*Port` 인터페이스를 주입한다.
- application service가 다른 기능 유스케이스를 호출해야 하면 구현체가 아니라 inbound use case 인터페이스를 주입한다.
- 같은 기능 내부의 세부 application service 조합은 허용하되, 순환 의존은 만들지 않는다.
- 트랜잭션 경계는 application service에 둔다.

Application 경계 객체 이름은 HTTP 용어를 쓰지 않는다.

- application 경계 객체에는 `Request`, `Response`, `Dto` 접미사를 쓰지 않는다.
- web adapter의 화면 form binding 값은 `Form`, Thymeleaf model 값은 `View`로 표현한다.
- `Request`, `Response` 접미사는 화면용 web adapter에 쓰지 않는다. provider 원본 계약이나 전역 예외 응답처럼 실제 데이터 계약이 있는 경우에만 제한적으로 사용한다.
- 별도 `command`, `query`, `criteria`, `result` 패키지는 기본으로 만들지 않는다.
- 특정 use case에서만 쓰는 입력/결과 값은 해당 `UseCase` 인터페이스 내부 record로 둔다. 예: `ManageRouletteUseCase.CreateTableCommand`, `ManageRouletteUseCase.TableResult`.
- 특정 outbound port에서만 쓰는 저장 입력/조회 조건/결과 값은 해당 `Port` 인터페이스 내부 record로 둔다. 예: `RoulettePort.CreateEventCommand`, `RoulettePort.EventResult`.
- 메서드가 하나뿐인 `UseCase`나 `Port`는 내부 record 이름을 `Command`, `Result`로 단순화할 수 있다.
- 메서드가 여러 개면 `CreateCommand`, `UpdateCommand`, `TableResult`처럼 행위나 결과 대상을 이름에 포함한다.
- 단순 조회 조건은 record를 만들지 않고 메서드 파라미터로 표현한다. 예: `findRecentRounds(userId, limit)`.
- 성공 여부만 감싸는 `Result(boolean success)`는 만들지 않는다. 성공은 정상 반환으로 표현하고, 실패는 예외나 명확한 실패 모델로 표현한다.
- 여러 use case나 port에서 공유되는 값이 순수 비즈니스 개념이면 `domain/{feature}`로 올린다.
- 여러 adapter/application 경계에서 공유되지만 domain 개념은 아니면, 그때만 가장 가까운 feature 패키지에 구체 이름으로 둔다.

UseCase 내부 record 예:

```java
public interface ApplyFavoriteUseCase {

    Result apply(Command command);

    record Command(
            Long userId,
            int amount
    ) {
    }

    record Result(
            Long userId,
            int currentAmount
    ) {
    }
}
```

Port 내부 record 예:

```java
public interface FavoritePort {

    Result apply(Command command);

    record Command(
            Long userId,
            int amount,
            String reason
    ) {
    }

    record Result(
            Long userId,
            int currentAmount
    ) {
    }
}
```

좋은 예:

```java
private final ManageRouletteUseCase manageRouletteUseCase;
private final RoulettePort roulettePort;
```

나쁜 예:

```java
private final ManageRouletteService rouletteService; // adapter/in 에서 금지
private final RouletteRepository rouletteRepository; // application 에서 금지
```

## 6. Inbound Adapter 패키지 규칙

Web adapter는 기능별 패키지로 나눈다.

```text
adapter/in/web
  roulette
    AdminRouletteController
  favorite
    FavoriteController
    FavoriteAdjustmentController
  command
    CommandController
    CommandModelAdvice
```

규칙:

- Controller는 HTTP method/path, 인증 주체, form binding, fragment model 조립만 담당한다.
- Controller는 application inbound use case 인터페이스만 주입한다.
- Controller는 `application.service.*` 구현체를 직접 주입하지 않는다.
- Controller는 persistence entity나 repository를 알면 안 된다.
- Controller는 비즈니스 계산을 하지 않는다.
- 전역 ControllerAdvice는 web feature 패키지에 두지 않고 `config/web`에 둔다.
- 화면 form binding 값은 컨트롤러 내부 record 또는 같은 feature 패키지의 `*Form`으로 둔다.
- Thymeleaf model 전용 값은 컨트롤러 내부 record 또는 같은 feature 패키지의 `*View`로 둔다.
- 화면용 `request`/`response` 하위 패키지는 만들지 않는다.
- 화면 route는 page 또는 Thymeleaf fragment 이름을 반환한다. 화면 조작만을 위한 JSON wrapper를 병행하지 않는다.
- `RestController`, `ResponseEntity`, `@RequestBody`는 외부 데이터 계약이 명확한 route에만 사용한다.

예:

```java
@Controller
@RequestMapping("/admin/roulette")
class AdminRouletteController {

    private final ManageRouletteUseCase manageRouletteUseCase;

    @PostMapping("/items")
    String addItem(@ModelAttribute RouletteItemForm form, Model model) {
        manageRouletteUseCase.addItem(
                form.tableId(),
                form.label(),
                form.probabilityBasisPoints(),
                form.losingItem(),
                form.rewardType(),
                form.conversionMode(),
                form.exchangeFavoriteValue(),
                form.displayOrder()
        );
        model.addAttribute("tables", manageRouletteUseCase.getTables());
        return "features/roulette/components :: roulette-config-region";
    }

    public record RouletteItemForm(
            Long tableId,
            String label,
            Integer probabilityBasisPoints,
            Boolean losingItem,
            String rewardType,
            String conversionMode,
            Integer exchangeFavoriteValue,
            Integer displayOrder
    ) {
    }
}
```

## 7. Outbound Persistence 패키지 규칙

Persistence는 전역 `entity`, `repository` 공용 패키지로 나누지 않는다.

1차 패키지는 영속화 대상 또는 aggregate 이름이다.
각 1차 패키지 루트에는 `*PersistenceAdapter`를 두고, 그 아래에 `entity`, `repository` 하위 패키지를 둔다.

```text
adapter/out/persistence
  common
    BaseEntity

  subscription
    SubscriptionPersistenceAdapter
    entity
      Subscription
    repository
      SubscriptionRepository

  donation
    DonationPersistenceAdapter
    entity
      Donation
    repository
      DonationRepository

  authorization
    AuthorizationPersistenceAdapter
    entity
      AuthorizationAccount
    repository
      AuthorizationRepository

  favorite
    FavoritePersistenceAdapter
    FavoriteAdjustmentPersistenceAdapter
    entity
      FavoriteAccount
      FavoriteHistory
      FavoriteAdjustment
    repository
      FavoriteRepository
      FavoriteHistoryRepository
      FavoriteAdjustmentRepository

  roulette
    RoulettePersistenceAdapter
    entity
      RouletteTable
      RouletteItem
      RouletteEvent
      RouletteRoundResult
    repository
      RouletteTableRepository
      RouletteItemRepository
      RouletteEventRepository
      RouletteRoundResultRepository

  overlay
    OverlayTokenPersistenceAdapter
    OverlayDisplayPersistenceAdapter
    entity
      OverlayToken
      OverlayDisplayEvent
    repository
      OverlayTokenRepository
      OverlayDisplayEventRepository

  upbo
    UpboPersistenceAdapter
    entity
      UpboTemplate
      UserUpbo
    repository
      UpboTemplateRepository
      UserUpboRepository

  weekly
    WeeklyChatRankPersistenceAdapter
    entity
      WeeklyChatRank
    repository
      WeeklyChatRankRepository
```

규칙:

- `adapter/out/persistence/entity`는 만들지 않는다.
- `adapter/out/persistence/repository`는 만들지 않는다.
- `adapter/out/persistence/{subject}` 루트에는 JPA 모델이나 `Repository`를 직접 두지 않는다.
- JPA 모델은 `adapter/out/persistence/{subject}/entity`에 둔다.
- `Repository`는 `adapter/out/persistence/{subject}/repository`에 둔다.
- JPA entity는 persistence 패키지 밖으로 노출하지 않는다.
- Spring Data repository는 같은 persistence subject 안의 adapter에서만 직접 사용한다.
- Persistence adapter는 outbound port를 구현한다.
- Persistence adapter는 JPA entity와 domain/application result 간 변환을 담당한다.
- `BaseEntity`처럼 여러 persistence entity가 공유하는 ORM 기반 클래스만 `persistence/common`에 둔다.
- 여러 entity가 하나의 유스케이스 aggregate로 함께 저장/조회되면 aggregate 패키지에 같이 둔다. 예: `roulette`.

## 8. Outbound External 패키지 규칙

외부 API 연동은 `adapter/out/external/{provider}`에 둔다.
provider 루트에는 outbound port 구현 adapter를 두고, 원본 API client와 원본 request/response 타입은 하위 패키지로 분리한다.

```text
adapter/out/external
  chzzk
    ChzzkClientAdapter
    client
      ChzzkOpenApiClient
    request
      AuthorizationRequest
      MessageRequest
    response
      AuthorizationResponse
      ChatResponse
      DonationResponse
      SessionResponse
      SubscriptionResponse
      SystemResponse
      UserResponse
  google
    GoogleSheetClientAdapter
    client
      GoogleSheetClient
    request
      GoogleSheetRequest
    response
      GoogleSheetRowResponse
```

규칙:

- `adapter/out/external/{provider}` 루트에는 `*ClientAdapter` 또는 `*ExternalAdapter`를 둔다.
- Feign client, SDK client, HTTP client는 `adapter/out/external/{provider}/client`에 둔다.
- 외부 API 요청 타입은 `adapter/out/external/{provider}/request`에 두고 `*Request`로 끝낸다.
- 외부 API 응답 또는 수신 payload 타입은 `adapter/out/external/{provider}/response`에 두고 `*Response`로 끝낸다.
- 외부 연동에서도 `dto` 패키지와 `*Dto` 접미사는 쓰지 않는다.
- provider별 응답 매핑은 external adapter에서 application port 계약 값이나 domain 값으로 변환한다.
- Application은 외부 API 구현체가 아니라 outbound port를 호출한다.
- 외부 API request/response 타입을 domain 객체로 직접 쓰지 않는다.
- 외부 API request/response 타입을 application port 계약으로 직접 쓰지 않는다. 특정 port에서만 필요한 값은 해당 port 내부 record로 둔다.

## 9. 변환 규칙

별도 Mapper 클래스는 만들지 않는다.
변환 책임은 외부 경계에 위치한 adapter 타입이 가진다.

기본 원칙:

- adapter 타입에서 내부 계층 타입으로 나가는 변환은 `to{SpecificTarget}()` 이름을 쓴다.
- `to()` 단독 이름은 쓰지 않는다. Java는 반환 타입만 다른 메서드 오버로딩을 허용하지 않기 때문이다.
- `toCommand()`, `toDomain()`, `toResult()`처럼 넓은 이름도 쓰지 않는다.
- 변환 대상의 행위나 모델 이름을 포함한다. 예: `toCreateTableCommand()`, `toRouletteTable()`, `toSessionResult()`.
- 내부 계층 타입에서 현재 adapter 타입을 생성하는 변환은 `from(...)`을 기본으로 쓴다.
- `from(...)`은 파라미터 타입으로 오버로딩해서 사용한다.
- 생성 대상은 현재 adapter 타입의 클래스 이름으로 이미 드러나므로 `fromTableResult(...)` 같은 파생 이름을 만들지 않는다.
- 변환은 단순 데이터 이동만 담당한다.
- 변환 메서드에 비즈니스 정책, 권한 판단, 저장 로직, 외부 API 호출을 넣지 않는다.
- `ErrorStatusResolver`는 `ErrorCode`와 HTTP status를 연결하는 `config/web` 타입이며, adapter 데이터 변환 규칙 대상이 아니다.

위치별 규칙:

| 위치 | 변환 방향 | 메서드 |
| --- | --- | --- |
| Web controller 내부 `*Form` | HTML form -> UseCase 입력 | 필요하면 `to{Action}Command()` |
| Web controller 내부 `*View` | UseCase Result -> Thymeleaf model | 필요하면 `from()` |
| `adapter/out/persistence/{subject}/entity` | Persistence Entity -> Domain | `to{DomainName}()` |
| `adapter/out/persistence/{subject}/entity` | Domain -> Persistence Entity | `from()` |
| `adapter/out/external/{provider}/request` | Port Command -> Provider Request | `from()` |
| `adapter/out/external/{provider}/response` | Provider Response -> Port Result | `to{ResultName}()` |

금지 규칙:

- `application`에는 adapter 타입 변환용 `to{SpecificTarget}()`, `from(...)` 메서드를 두지 않는다.
- `domain`에는 adapter 타입 변환용 `to{SpecificTarget}()`, `from(...)` 메서드를 두지 않는다.
- `Mapper`, `Converter`, `Assembler`, `Translator` 클래스는 만들지 않는다.
- Web form/view model은 persistence entity를 알지 않는다.
- Persistence entity는 web form/view model을 알지 않는다.
- Domain은 web form/view model, persistence entity, external request/response를 알지 않는다.
- Application은 web form/view model, persistence entity, external request/response를 알지 않는다.

Persistence Entity 예:

```java
@Entity
public class RouletteTable {

    @Id
    private Long id;

    private String title;

    public RouletteTable toRouletteTable() {
        return new RouletteTable(id, title);
    }

    public static RouletteTable from(RouletteTable table) {
        RouletteTable entity = new RouletteTable();
        entity.id = table.id();
        entity.title = table.title();
        return entity;
    }
}
```

Persistence Adapter 예:

```java
@Override
public RouletteTable save(RouletteTable table) {
    RouletteTable saved = repository.save(
            RouletteTable.from(table)
    );

    return saved.toRouletteTable();
}
```

요약:

- adapter는 변환을 담당한다.
- application은 유스케이스 흐름을 조합한다.
- domain은 비즈니스 규칙을 담당한다.
- `to{SpecificTarget}()`과 `from(...)`은 adapter 타입에만 둔다.
- `from(...)`은 오버로딩을 사용하고, 별도 Mapper로 분리하지 않는다.

## 10. Exception 규칙

프로젝트 예외는 필요한 최소 custom exception 타입과 공통 에러 코드로 처리한다.

패키지 구조:

```text
domain
  exception
    NyangNyangException
    ErrorCode
```

Spring MVC 예외 처리 구조:

```text
config
  web
    ApiExceptionHandler
    ErrorResponse
    ErrorStatusResolver
```

기본 원칙:

- 공통 예외 타입은 `domain/exception`에 둔다.
- custom exception 클래스는 최대한 만들지 않는다.
- 기본 실패 표현은 `NyangNyangException(ErrorCode)`와 `ErrorCode`로 처리한다.
- `ErrorCode`로 구분 가능한 실패는 별도 exception 클래스로 만들지 않는다.
- 추가 custom exception 클래스는 타입 분기가 반드시 필요한 경우에만 `domain/exception`에 정의한다.
- 개별 feature exception 클래스는 만들지 않는다.
- 실패 구분은 `ErrorCode`로 표현한다.
- `domain`, `application`, `adapter`는 의도적으로 표현해야 하는 실패만 `NyangNyangException(ErrorCode)`로 변환해 던진다.
- `ErrorCode`는 비즈니스 의미만 가진다.
- `ErrorCode`는 HTTP status, JPA, Feign, Servlet, Spring Security 같은 기술 정보를 가지지 않는다.
- 예외 메시지는 `ErrorCode` 내부에서 관리한다.
- 전역 ControllerAdvice는 `config/web`에 둔다.
- HTTP status 매핑은 `config/web/ErrorStatusResolver`에서 처리한다.
- 예외 HTTP 응답 모델은 `config/web/ErrorResponse`에 둔다.
- adapter 밖으로 JPA, Feign, Servlet 예외를 직접 노출하지 않는다.

예외 클래스:

```java
public class NyangNyangException extends RuntimeException {

    private final ErrorCode errorCode;

    public NyangNyangException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public NyangNyangException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.message(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
```

ErrorCode 예:

```java
public enum ErrorCode {

    FAVORITE_ACCOUNT_NOT_FOUND(
            "호감도 계정을 찾을 수 없습니다."
    ),

    INSUFFICIENT_FAVORITE_AMOUNT(
            "호감도가 부족합니다."
    ),

    ROULETTE_TABLE_NOT_FOUND(
            "룰렛 테이블을 찾을 수 없습니다."
    ),

    INVALID_ROULETTE_PROBABILITY(
            "룰렛 확률이 올바르지 않습니다."
    ),

    DUPLICATE_DONATION_EVENT(
            "이미 처리된 후원 이벤트입니다."
    );

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
```

사용 예:

```java
if (currentAmount < amount) {
    throw new NyangNyangException(
            ErrorCode.INSUFFICIENT_FAVORITE_AMOUNT
    );
}

return rouletteRepository.findById(tableId)
        .orElseThrow(() -> new NyangNyangException(
                ErrorCode.ROULETTE_TABLE_NOT_FOUND
        ));
```

HTTP Status 매핑:

```java
final class ErrorStatusResolver {

    private ErrorStatusResolver() {
    }

    static HttpStatus toStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case FAVORITE_ACCOUNT_NOT_FOUND,
                 ROULETTE_TABLE_NOT_FOUND ->
                    HttpStatus.NOT_FOUND;

            case INSUFFICIENT_FAVORITE_AMOUNT,
                 INVALID_ROULETTE_PROBABILITY ->
                    HttpStatus.BAD_REQUEST;

            case DUPLICATE_DONATION_EVENT ->
                    HttpStatus.CONFLICT;
        };
    }
}
```

Exception Handler:

```java
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(NyangNyangException.class)
    ResponseEntity<ErrorResponse> handle(
            NyangNyangException ex
    ) {
        return ResponseEntity
                .status(
                        ErrorStatusResolver.toStatus(
                                ex.errorCode()
                        )
                )
                .body(
                        ErrorResponse.from(ex)
                );
    }
}
```

ErrorResponse:

```java
public record ErrorResponse(
        String code,
        String message
) {

    public static ErrorResponse from(
            NyangNyangException exception
    ) {
        return new ErrorResponse(
                exception.errorCode().name(),
                exception.errorCode().message()
        );
    }
}
```

금지 규칙:

- domain에 HTTP status를 넣지 않는다.
- domain에 Spring, JPA, Feign 예외 타입을 넣지 않는다.
- adapter 밖으로 JPA, Feign, Servlet 예외를 직접 노출하지 않는다.
- application에 별도 공용 exception 패키지를 만들지 않는다.
- `common.exception`, `global.exception` 같은 루트 패키지는 만들지 않는다.
- 불필요한 custom exception 클래스를 만들지 않는다.

요약:

- 비즈니스 실패는 `NyangNyangException(ErrorCode)`로 처리한다.
- 실패 구분은 `ErrorCode`로 표현한다.
- HTTP status는 `config/web`에서만 처리한다.
- 기술 정보는 domain exception에 넣지 않는다.

## 11. Config 패키지 규칙

```text
config
  SecurityConfig
  FeignConfig
  SwaggerConfig
  LocalDummyDataInitializer
  property
    ChzzkProperty
    GoogleProperty
  web
    ApiExceptionHandler
    ErrorResponse
    ErrorStatusResolver
```

규칙:

- config는 DI, 보안, 운영 설정만 담당한다.
- config에 비즈니스 정책을 넣지 않는다.
- `config/web`은 Spring MVC 전역 예외 처리와 HTTP status 매핑만 담당한다.
- `config/web`은 예외 타입을 새로 만들지 않고 `domain/exception`의 `NyangNyangException`, `ErrorCode`만 처리한다.
- `config/web`에는 feature controller, feature form/view model, use case 호출 로직을 두지 않는다.
- local dummy data처럼 persistence 타입을 직접 써야 하는 초기화 코드는 config 예외로 둔다.

## 12. 의존성 상세 규칙

허용:

```text
adapter/in/web -> application/port/in
adapter/out/external -> application/port/out
adapter/out/external -> domain
adapter/out/persistence -> application/port/out
adapter/out/persistence -> domain
application/port -> domain
application/service -> application/port/in
application/service -> application/port/out
application/service -> domain
config -> application/service
config -> adapter
config -> domain
```

금지:

```text
adapter/in/web -> application/service
adapter/in/web -> domain
adapter/in/web -> adapter/out/persistence
application -> adapter
application -> Spring Data Repository
application -> JPA Entity
domain -> application
domain -> adapter
domain -> Spring/JPA
```

## 13. 네이밍 규칙

패키지명과 클래스 접미사는 역할을 드러내되, persistence entity 클래스에는 `Entity` 접미사를 붙이지 않는다. `entity` 패키지는 ORM 위치를 나타내고, 클래스명은 Hibernate 기본 물리 네이밍으로 매핑될 테이블명과 맞춘다.

| 패키지 | 클래스 이름 |
| --- | --- |
| `domain/{feature}` | 비즈니스 상태/규칙/정책/불변식 이름. `Entity`, `Dto`, `Request`, `Response`, 저장 row 모델 금지. 예: `FavoriteAccount`, `AttendanceUserState`, `RoulettePolicy` |
| `domain/exception` | 공통 프로젝트 예외와 에러 코드만 둔다. 예: `NyangNyangException`, `ErrorCode` |
| `application/port/in/{feature}` | 사용자/시스템 행위 + `UseCase`. 예: `ManageRouletteUseCase`, `QueryRouletteResultUseCase` |
| `UseCase` 내부 입력 record | 상태 변경 입력은 행위 + `Command`. 조회는 가능하면 메서드 파라미터를 사용한다. 예: `ManageRouletteUseCase.CreateTableCommand` |
| `UseCase` 내부 결과 record | 결과 이름 + `Result`. 예: `ManageRouletteUseCase.TableResult` |
| `application/port/out/{feature}` | 책임 단위 저장소/외부 의존 계약 + `Port`. 예: `RoulettePort`, `AuthorizationPort` |
| `Port` 내부 입력 record | 저장/변경 입력은 행위 + `Command`. 조회 조건은 가능하면 메서드 파라미터를 사용한다. 예: `RoulettePort.CreateEventCommand` |
| `Port` 내부 결과 record | 결과 이름 + `Result`. 예: `RoulettePort.EventResult` |
| `application/service/{feature}` | use case 구현체 또는 application 내부 조합 서비스 + `Service`. 예: `ManageRouletteService`, `ProcessRouletteDonationService`, `RouletteRoundApplyService` |
| `adapter/in/web/{feature}` controller 내부 `*Form` | HTML form binding 값. 예: `RouletteItemForm`, `FavoriteAdjustmentApplyForm` |
| `adapter/in/web/{feature}` controller 내부 `*View` | Thymeleaf model 전용 값. 예: `RouletteItemView`, `FavoriteHistoryView` |
| `adapter/out/persistence/{subject}` | outbound port 구현체 + `PersistenceAdapter`. 예: `RoulettePersistenceAdapter` |
| `adapter/out/persistence/{subject}/entity` | JPA 모델. `Entity` 접미사 없이 도메인 테이블명과 맞춘다. 예: `RouletteEvent` |
| `adapter/out/persistence/{subject}/repository` | Spring Data 저장소 + `Repository`. 예: `RouletteEventRepository` |
| `adapter/out/external/{provider}` | outbound port 구현체 + `ClientAdapter` 또는 `ExternalAdapter`. 예: `ChzzkClientAdapter` |
| `adapter/out/external/{provider}/client` | API/SDK client + `Client`. 예: `ChzzkOpenApiClient` |
| `adapter/out/external/{provider}/request` | provider 원본 요청 타입 + `Request`. 예: `AuthorizationRequest`, `MessageRequest` |
| `adapter/out/external/{provider}/response` | provider 원본 응답/수신 타입 + `Response`. 예: `DonationResponse`, `SessionResponse` |
| `config/web` | Spring MVC 전역 예외 처리 타입. 예: `ApiExceptionHandler`, `ErrorResponse`, `ErrorStatusResolver` |

## 14. 기능별 경계

### Favorite

- 호감도 잔액과 원장 규칙은 `domain/favorite`에 둔다.
- 출석, 업보, 룰렛, 관리자 조정은 Favorite use case를 통해 잔액을 바꾼다.
- 잔액 변경은 원장 거래와 같은 트랜잭션에서 처리한다.
- idempotency key가 있는 거래는 중복 반영하지 않는다.

### Roulette

- 룰렛 확률 검증과 선택 정책은 `domain/roulette`에 둔다.
- 룰렛 입력 검증, 후원 금액 파싱, 조회/시뮬레이션 제한값, 이벤트 상태 판정처럼 포트가 필요 없는 순수 규칙도 `domain/roulette`에 둔다.
- 룰렛 테이블/항목/이벤트/회차 저장은 `adapter/out/persistence/roulette`가 담당하고, application에 노출되는 저장/조회 결과는 `RoulettePort` 내부 `*Result`로 표현한다.
- 후원 이벤트로 룰렛을 실행하는 호출 계약은 inbound use case로 둔다.
- 후원 이벤트 처리 서비스는 구현체가 아니라 `ProcessRouletteDonationUseCase`를 주입한다.
- Web controller는 `ManageRouletteUseCase`, `QueryRouletteResultUseCase` 같은 inbound use case를 주입한다.

### Overlay

- OBS 표시 이벤트 모델은 `domain/overlay`에 둔다.
- 오버레이 토큰과 표시 이벤트 저장은 `adapter/out/persistence/overlay`가 담당한다.
- 오버레이 API controller는 overlay inbound use case만 호출한다.

### Upbo

- 업보 템플릿과 사용자 보유 업보는 `domain/upbo`에 둔다.
- 업보 저장은 `adapter/out/persistence/upbo`가 담당한다.
- 업보가 호감도로 전환되면 Favorite use case를 호출한다.

### Authorization

- 인증 계정 모델은 `domain/authorization`에 둔다.
- OAuth와 CHZZK API 세부 request/response 타입은 `adapter/out/external`에 둔다.
- Application outbound port가 외부 API 호출 값을 표현해야 하면 provider 원본 request/response 타입이 아니라 해당 port 내부 record를 둔다.
- 인증 저장은 `adapter/out/persistence/authorization`이 담당한다.

## 15. 아키텍처 테스트 기준

아키텍처 테스트는 다음을 강제해야 한다.

- 루트 패키지는 `adapter`, `application`, `config`, `domain`만 존재한다.
- `domain`은 `application`, `adapter`, Spring, JPA에 의존하지 않는다.
- `application`은 `adapter`에 의존하지 않는다.
- `adapter/in/web`은 `adapter/out/persistence`에 의존하지 않는다.
- `adapter/in/web`은 `application/service` 구현체에 의존하지 않는다.
- `adapter/in/web`은 `domain`에 직접 의존하지 않는다.
- `application/port/in/{feature}`에는 `*UseCase.java` 인터페이스만 직접 둔다.
- `application/port/out/{feature}`에는 `*Port.java` 인터페이스만 직접 둔다.
- `application/port/in/{feature}/usecase`에 Java 소스가 있으면 실패한다.
- `application/port/out/{feature}/repository`에 Java 소스가 있으면 실패한다.
- `adapter/out/persistence/entity`에 Java 소스가 있으면 실패한다.
- `adapter/out/persistence/repository`에 Java 소스가 있으면 실패한다.
- `adapter/in/web/common`에 Java 소스가 있으면 실패한다.
- `domain/model`에 Java 소스가 있으면 실패한다.
- domain에 저장/조회 row 형태의 `public record`가 있으면 실패한다. 허용 record는 정책/불변식 보조 value로 제한한다.
- `application/model`과 `application/gateway`에 Java 소스가 있으면 실패한다.
- `application/exception`, `common/exception`, `global/exception`에 Java 소스가 있으면 실패한다.
- `domain/exception`이 Spring, JPA, Feign, Servlet, Spring Security 타입에 의존하면 실패한다.
- `config/web` 밖에 `ApiExceptionHandler`, `ErrorStatusResolver`, 예외용 `ErrorResponse`가 있으면 실패한다.
- `application` 또는 `domain`에 web, persistence, external adapter 타입을 변환하는 `to{SpecificTarget}()`, `from(...)` 메서드가 있으면 실패한다.
- `Mapper`, `Converter`, `Assembler`, `Translator` 클래스가 있으면 실패한다.
- `domain/exception`에 불필요한 feature custom exception 클래스가 있으면 실패한다.
- 기존 루트 `controller`, `service`, `repository`, `data`에 Java 소스가 있으면 실패한다.

## 16. 예외

- `config`는 Spring 조립을 위해 구현체를 알 수 있다.
- `LocalDummyDataInitializer`는 local profile용 초기 데이터 생성을 위해 persistence entity/repository를 직접 사용할 수 있다.
- `persistence/common/BaseEntity`는 ORM 공통 필드 때문에 허용한다.
- 전환 중인 기존 코드는 발견 즉시 같은 기준으로 정리한다. 신규 코드는 예외 없이 이 문서를 따른다.
