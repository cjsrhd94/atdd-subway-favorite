package nextstep.favorite.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.favorite.application.dto.FavoriteRequest;
import nextstep.member.acceptance.MemberSteps;
import nextstep.auth.application.dto.TokenResponse;
import nextstep.subway.acceptance.fixture.LineFixture;
import nextstep.subway.acceptance.fixture.StationFixture;
import nextstep.subway.dto.line.LineResponse;
import nextstep.subway.dto.station.StationResponse;
import nextstep.utils.AcceptanceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("즐겨찾기 관련 기능")
public class FavoriteAcceptanceTest extends AcceptanceTest {
    private static final String EMAIL = "user@email.com";
    private static final String PASSWORD = "password";
    private static final Integer AGE = 20;
    private String accessToken;

    private Long 교대역;
    private Long 강남역;

    @BeforeEach
    public void setUp() {
        super.setUp();

        교대역 = StationFixture.지하철역_생성_요청("교대역").as(StationResponse.class).getId();
        강남역 = StationFixture.지하철역_생성_요청("강남역").as(StationResponse.class).getId();

        LineFixture.노선_생성_요청("2호선", "green", 10, 교대역, 강남역).as(LineResponse.class).getId();

        MemberSteps.회원_생성_요청(EMAIL, PASSWORD, AGE);
        accessToken = MemberSteps.회원_로그인(EMAIL, PASSWORD).as(TokenResponse.class).getAccessToken();
    }

    @Nested
    class 즐겨찾기_생성 {
        /**
         * When 즐겨찾기를 생성하면
         * Then 즐겨찾기가 생성된다.
         */
        @DisplayName("즐겨찾기를 생성한다.")
        @Test
        void 성공() {
            // when
            ExtractableResponse<Response> 즐겨찾기_등록_응답 = 즐겨찾기_등록_요청(교대역, 강남역);

            // then
            assertThat(즐겨찾기_등록_응답.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        }

        /**
         * When 즐겨찾기를 생성하면
         * Then 출발역과 도착역이 동일 시 에러가 발생한다.
         */
        @DisplayName("출발역과 도착역이 동일하면 즐겨찾기를 등록할 수 없다.")
        @Test
        void 출발역_도착역_동일_즐겨찾기_등록_실패() {
            // when
            ExtractableResponse<Response> 즐겨찾기_등록_응답 = 즐겨찾기_등록_요청(교대역, 교대역);

            // then
            assertThat(즐겨찾기_등록_응답.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        /**
         * When 즐겨찾기를 생성하면
         * Then 연결되지 않은 역인 경우 에러가 발생한다.
         */
        @DisplayName("연결되지 않은 역은 즐겨찾기를 등록할 수 없다.")
        @Test
        void 비정상_경로_즐겨찾기_등록_실패() {
            // given
            Long 연결되지_않은_역 = -9999L;

            // when
            ExtractableResponse<Response> 즐겨찾기_등록_응답 = 즐겨찾기_등록_요청(교대역, 연결되지_않은_역);

            // then
            assertThat(즐겨찾기_등록_응답.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }
    }

    /**
     * Given 즐겨찾기를 생성하고
     * When 즐겨찾기를 조회하면
     * Then 생성한 즐겨찾기가 조회된다.
     */
    @DisplayName("즐겨찾기를 조회한다.")
    @Test
    void 즐겨찾기_조회() {
        // given
        ExtractableResponse<Response> 즐겨찾기_등록_응답 = 즐겨찾기_등록_요청(교대역, 강남역);

        assertThat(즐겨찾기_등록_응답.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // when
        ExtractableResponse<Response> 즐겨찾기_조회_응답 = 즐겨찾기_조회_요청();

        // then
        assertThat(즐겨찾기_조회_응답.jsonPath().getList("source.id", Long.class)).containsExactly(교대역);
        assertThat(즐겨찾기_조회_응답.jsonPath().getList("target.id", Long.class)).containsExactly(강남역);
    }

    @Nested
    class 즐겨찾기_삭제 {
        /**
         * Given 즐겨찾기를 생성하고
         * When 즐겨찾기를 삭제하면
         * Then 생성한 즐겨찾기가 조회되지 않는다.
         */
        @DisplayName("즐겨찾기를 삭제한다.")
        @Test
        void 성공() {
            // given
            ExtractableResponse<Response> 즐겨찾기_등록_응답 = 즐겨찾기_등록_요청(교대역, 강남역);

            assertThat(즐겨찾기_등록_응답.statusCode()).isEqualTo(HttpStatus.CREATED.value());

            // when
            Long 생성한_즐겨찾기_ID = Long.parseLong(즐겨찾기_등록_응답.header("Location").split("/")[2]);
            ExtractableResponse<Response> 즐겨찾기_삭제_응답 = 즐겨찾기_삭제_요청(생성한_즐겨찾기_ID);

            assertThat(즐겨찾기_삭제_응답.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

            // then
            ExtractableResponse<Response> 즐겨찾기_조회_응답 = 즐겨찾기_조회_요청();
            assertThat(즐겨찾기_조회_응답.jsonPath().getList("id", Long.class)).doesNotContain(생성한_즐겨찾기_ID);
        }

        /**
         * When 즐겨찾기를 삭제하면
         * Then 등록하지 않은 즐겨찾기 삭제 시 에러가 발생한다.
         */
        @DisplayName("등록하지 않은 즐겨찾기는 삭제할 수 없다.")
        @Test
        void 미등록_즐겨찾기_삭제_실패() {
            // given
            Long 존재하지_않는_즐겨찾기_ID = -9999L;

            // when
            ExtractableResponse<Response> 즐겨찾기_삭제_응답 = 즐겨찾기_삭제_요청(존재하지_않는_즐겨찾기_ID);

            // then
            assertThat(즐겨찾기_삭제_응답.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }
    }

    private ExtractableResponse<Response> 즐겨찾기_등록_요청(Long source, Long target) {
        return RestAssured
            .given()
            .auth().oauth2(accessToken)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(new FavoriteRequest(source, target))
            .when()
            .post("/favorites")
            .then()
            .extract();
    }

    private ExtractableResponse<Response> 즐겨찾기_조회_요청() {
        return RestAssured
            .given()
            .auth().oauth2(accessToken)
            .when()
            .get("/favorites")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();
    }

    private ExtractableResponse<Response> 즐겨찾기_삭제_요청(Long id) {
        return RestAssured
            .given()
            .auth().oauth2(accessToken)
            .when()
            .delete("/favorites/{id}", id)
            .then()
            .extract();
    }
}