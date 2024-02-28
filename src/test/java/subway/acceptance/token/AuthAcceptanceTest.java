package subway.acceptance.token;

import static org.assertj.core.api.Assertions.*;
import static subway.fixture.acceptance.MemberAcceptanceSteps.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import subway.dto.member.MemberResponse;
import subway.fixture.member.GithubResponses;
import subway.member.JwtTokenProvider;
import subway.utils.database.DatabaseCleanup;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class AuthAcceptanceTest {
	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private DatabaseCleanup databaseCleanup;

	@BeforeEach
	void setUp() {
		databaseCleanup.execute();
	}

	/** given 임의의 사용자를 생성한다.
	 *  when 로그인을 위해 api 요청한다.
	 *  then ID / PW 확인후, 로그인 가능한 토큰이 반환된다.
	 */
	@DisplayName("Bearer Auth")
	@Test
	void bearerAuth() {
		// given
		멤버_생성();

		// when
		Map<String, String> params = new HashMap<>();
		params.put("email", EMAIL);
		params.put("password", PASSWORD);

		ExtractableResponse<Response> response = RestAssured.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.body(params)
			.when().post("/login/token")
			.then().log().all()
			.statusCode(HttpStatus.OK.value())
			.extract();

		// then
		String accessToken = response.jsonPath().getString("accessToken");
		String expectedEmail = jwtTokenProvider.getPrincipal(accessToken);
		assertThat(EMAIL).isEqualTo(expectedEmail);
	}

	/** given 시스템 DB에 등록 되어 있지 않은 임의의 사용자를 정의한다.
	 *  when github oauth를 이용하여 로그인을 한다.
	 *  when 임의의 사용자는 자동으로 회원가입이 된다.
	 *  then 로그인 가능한 토큰이 반환된다.
	 *  then 임의의 사용자가 회원가입이 자동으로 이루어 졌는지 확인한다.
	 */
	@DisplayName("Github Auth")
	@Test
	void githubAuth() {
		// given
		GithubResponses 사용자1 = GithubResponses.사용자1;
		String actualEmail = 사용자1.getEmail();

		// when
		Map<String, String> params = new HashMap<>();
		params.put("code", 사용자1.getCode());

		ExtractableResponse<Response> response = RestAssured.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.body(params)
			.when().post("/login/github")
			.then().log().all()
			.statusCode(HttpStatus.OK.value())
			.extract();

		// then
		String accessToken = response.jsonPath().getString("accessToken");
		String expectedEmail = jwtTokenProvider.getPrincipal(accessToken);
		assertThat(actualEmail).isEqualTo(expectedEmail);

		// then
		MemberResponse memberResponse = 멤버_ME_조회(accessToken).as(MemberResponse.class);
		assertThat(actualEmail).isEqualTo(memberResponse.getEmail());
	}
}
