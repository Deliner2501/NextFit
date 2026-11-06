package com.nextfit.service;

import com.nextfit.model.Member;
import com.nextfit.service.FakeRepositories.FakeAttendanceRepository;
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
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Starts the real server on a free port and talks to it like a phone would. */
class AccessServerTest {

    private AccessServer server;
    private AttendanceService attendance;
    private String token;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() throws Exception {
        MemberService members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        Member member = members.register(FakeRepositories.newMember("1065123456"));
        MembershipExpiry expiry = id -> Optional.of(LocalDate.now().plusDays(20));
        attendance = new AttendanceService(members, new FakeAttendanceRepository(), id -> true);
        AccessService access = new AccessService(members, attendance, expiry, Clock.systemDefaultZone());
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 5);
        PhoneAccess phone = new PhoneAccess(members, access, new AccessTokens(key), Clock.systemDefaultZone());
        token = phone.tokenOf(member);
        server = new AccessServer(phone, 0);
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
    void scanningAValidCodeRegistersTheEntryAndNeverCaches() throws Exception {
        HttpResponse<String> response = get("/a/" + token);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("ENTRADA REGISTRADA"));
        assertEquals(1, attendance.findInside().size());
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(""));
    }

    @Test
    void scanningAnInvalidCodeIsDeniedAndDoesNotRegisterAnything() throws Exception {
        HttpResponse<String> response = get("/a/ZZZZZZZZZZ");

        assertTrue(response.body().contains("ACCESO DENEGADO"));
        assertTrue(attendance.findInside().isEmpty());
    }

    @Test
    void otherAddressesAreNotFoundAndPhotoWithoutPhotoIs404() throws Exception {
        assertEquals(404, get("/admin").statusCode());
        assertEquals(404, get("/photo/" + token).statusCode());
        assertEquals(404, get("/photo/ZZZZZZZZZZ").statusCode());
    }
}
