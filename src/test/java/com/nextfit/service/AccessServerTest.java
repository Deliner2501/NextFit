package com.nextfit.service;

import com.nextfit.model.Member;
import com.nextfit.service.FakeRepositories.FakeMemberRepository;
import com.nextfit.validation.MemberValidator;
import com.nextfit.web.AccessServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Starts the real server on a free port and talks to it like a phone would. */
class AccessServerTest {

    private AccessServer server;
    private String fixedToken;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() throws Exception {
        MemberService members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        Member member = members.register(FakeRepositories.newMember("1065123456"));
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 5);
        PhoneAccess phone = new PhoneAccess(members, new AccessTokens(key));
        fixedToken = phone.tokenOf(member);
        server = new AccessServer(phone, null, null, null, 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void pingAnswersSoAPhoneCanTestTheConnection() throws Exception {
        HttpResponse<String> response = get("/ping");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("NextFit responde"));
    }

    @Test
    void otherAddressesAreNotFoundAndPhotoWithoutPhotoIs404() throws Exception {
        assertEquals(404, get("/admin").statusCode());
        assertEquals(404, get("/photo/" + fixedToken).statusCode());
        assertEquals(404, get("/photo/ZZZZZZZZZZ").statusCode());
    }

    @Test
    void theCarnetCodeOpensThePortalAndNeverRegistersAnything() throws Exception {
        assertEquals(404, get("/m/" + fixedToken).statusCode());
        assertEquals(404, get("/a/" + fixedToken).statusCode());
        assertEquals(404, get("/c/" + fixedToken).statusCode());
        assertEquals(404, get("/q/" + fixedToken).statusCode());
    }
}
