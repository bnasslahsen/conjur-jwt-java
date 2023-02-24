package org.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

import com.cyberark.conjur.sdk.AccessToken;
import com.cyberark.conjur.sdk.ApiClient;
import com.cyberark.conjur.sdk.ApiException;
import com.cyberark.conjur.sdk.Configuration;
import com.cyberark.conjur.sdk.endpoint.AuthenticationApi;
import com.cyberark.conjur.sdk.endpoint.SecretsApi;
import org.apache.commons.lang3.StringUtils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author bnasslahsen
 */
@SpringBootApplication
public class MainJWT {
	private static final String VARIABLE = "variable";

	public static void main(String[] args) throws IOException {
		SpringApplication.run(MainJWT.class, args);
		ApiClient conjurClient = Configuration.getDefaultApiClient();
		AccessToken accessToken = getJwtAccessToken(conjurClient);
		if (accessToken == null) {
			System.err.println("Access token is null, Please enter proper environment variables.");
		}
		else {
			String token = accessToken.getHeaderValue();
			conjurClient.setAccessToken(token);
		}

		try {
			SecretsApi secretsApi = new SecretsApi();
			String username = secretsApi.getSecret(conjurClient.getAccount(), VARIABLE, System.getenv("APP_SECRET_USERNAME"));
			System.out.println("username=" + username);
			String password = secretsApi.getSecret(conjurClient.getAccount(), VARIABLE, System.getenv("APP_SECRET_PASSWORD"));
			System.out.println("password=" + password);
		}
		catch (ApiException e) {
			String message = StringUtils.isEmpty(e.getMessage()) ? e.getResponseBody() : e.getMessage();
			throw new RuntimeException(message, e);
		}
		finally {
			conjurClient.getHttpClient().connectionPool().evictAll();
		}
	}

	private static AccessToken getJwtAccessToken(ApiClient conjurClient) throws IOException {
		AuthenticationApi apiInstance = new AuthenticationApi(conjurClient);
		String xRequestId = UUID.randomUUID().toString();
		String jwtTokenPath =  System.getenv("CONJUR_JWT_TOKEN_PATH");
		String jwt = new String(Files.readAllBytes(Paths.get(jwtTokenPath)));
		try {
			String accessTokenStr = apiInstance.getAccessTokenViaJWT(conjurClient.getAccount(), System.getenv("CONJUR_SERVICE_ID"), xRequestId, jwt);
			return AccessToken.fromEncodedToken(Base64.getEncoder().encodeToString(accessTokenStr.getBytes(StandardCharsets.UTF_8)));
		}
		catch (ApiException e) {
			System.err.println("Status code: " + e.getCode());
			System.err.println("Reason: " + e.getResponseBody());
			System.err.println("Response headers: " + e.getResponseHeaders());
			throw new RuntimeException(e);
		}
	}
}