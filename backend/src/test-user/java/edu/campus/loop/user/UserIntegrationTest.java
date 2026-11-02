package edu.campus.loop.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = UserStandaloneApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"USER_DB_URL=jdbc:h2:mem:user-tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=30000"})
class UserIntegrationTest {
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired UserDatabase database;
    @Autowired PasswordHasher hasher;
    @Autowired UserService service;
    @Autowired AuthRateLimiter limiter;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final String PASSWORD = " campus123 ";

    @BeforeEach void clean() {
        database.transaction(c -> {
            try (var s = c.createStatement()) { s.executeUpdate("DELETE FROM auth_tokens"); s.executeUpdate("DELETE FROM users"); }
            return null;
        });
        // 每个测试拥有独立限流窗口，测试互不影响；生产不存在重置端点。
        ((Map<?, ?>) ReflectionTestUtils.getField(limiter, "windows")).clear();
    }
    private HttpResponse<String> call(String method, String path, Object body, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1" + path))
                .timeout(Duration.ofSeconds(90)).header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return http.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode data(HttpResponse<String> response) throws Exception { return json.readTree(response.body()).path("data"); }
    private void register(String account) throws Exception {
        assertThat(call("POST", "/auth/register", Map.of("account", account, "password", PASSWORD, "nickname", "同学"), null).statusCode()).isEqualTo(200);
    }
    private String login(String account) throws Exception {
        var response = call("POST", "/auth/login", Map.of("account", account, "password", PASSWORD), null);
        assertThat(response.statusCode()).isEqualTo(200);
        return data(response).path("token").asText();
    }
    @Test void registrationPersistsHashAndPublicFieldsOnly() throws Exception {
        var response = call("POST", "/auth/register", Map.of("account", "00123456", "password", PASSWORD, "nickname", " 张同学 "), null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(data(response).path("account").asText()).isEqualTo("00123456");
        assertThat(data(response).path("nickname").asText()).isEqualTo("张同学");
        assertThat(response.body()).doesNotContain("password", "token_hash");
        assertThat(json.readTree(response.body()).path("traceId").asText()).isNotBlank();
        String stored = database.read(c -> { try (var s=c.createStatement(); var r=s.executeQuery("SELECT password_hash FROM users")) { r.next(); return r.getString(1); } });
        assertThat(stored).startsWith("pbkdf2-sha256$600000$").doesNotContain(PASSWORD);
        assertThat(hasher.matches(PASSWORD, stored)).isTrue();
    }
    @Test void duplicateNumericAccountIsRejected() throws Exception {
        register("100001");
        var result = call("POST", "/auth/register", Map.of("account", "100001", "password", PASSWORD, "nickname", "重复"), null);
        assertThat(result.statusCode()).isEqualTo(409);
        assertThat(json.readTree(result.body()).path("code").asText()).isEqualTo("ACCOUNT_EXISTS");
    }
    @Test void malformedAndInvalidBodiesAreRejected() throws Exception {
        assertThat(call("POST", "/auth/register", Map.of(), null).statusCode()).isEqualTo(400);
        assertThat(call("POST", "/auth/register", Map.of("account", "a", "password", PASSWORD, "nickname", "同学"), null).statusCode()).isEqualTo(400);
        assertThat(call("POST", "/auth/register", Map.of("account", "100001", "password", "short", "nickname", "同学"), null).statusCode()).isEqualTo(400);
        assertThat(call("POST", "/auth/register", Map.of("account", "100001", "password", PASSWORD, "nickname", "  "), null).statusCode()).isEqualTo(400);
    }
    @Test void loginUsesRealPasswordAndPreservesSpaces() throws Exception {
        register("100001");
        var login = call("POST", "/auth/login", Map.of("account", "100001", "password", PASSWORD), null);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(data(login).path("expiresAt").asText()).isNotBlank();
        var wrong = call("POST", "/auth/login", Map.of("account", "100001", "password", PASSWORD.trim()), null);
        var missing = call("POST", "/auth/login", Map.of("account", "999999", "password", PASSWORD), null);
        assertThat(wrong.statusCode()).isEqualTo(401);
        assertThat(missing.statusCode()).isEqualTo(401);
        assertThat(json.readTree(wrong.body()).path("message")).isEqualTo(json.readTree(missing.body()).path("message"));
    }
    @Test void tokenIsHashedAndResponsesAreNotCached() throws Exception {
        register("100001"); String token=login("100001");
        var me=call("GET", "/users/me", null, token);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.headers().firstValue("Cache-Control")).hasValue("no-store");
        String stored=database.read(c -> {try(var s=c.createStatement();var r=s.executeQuery("SELECT token_hash FROM auth_tokens")){r.next();return r.getString(1);}});
        assertThat(stored).hasSize(64).isNotEqualTo(token);
    }
    @Test void missingForgedExpiredAndDisabledCredentialsFail() throws Exception {
        assertThat(call("GET", "/users/me", null, null).statusCode()).isEqualTo(401);
        assertThat(call("GET", "/users/me", null, "x".repeat(43)).statusCode()).isEqualTo(401);
        assertThat(call("GET", "/users/me", null, "malformed").statusCode()).isEqualTo(401);
        register("100001");String expired=login("100001");
        database.transaction(c->{try(var s=c.createStatement()){s.executeUpdate("UPDATE auth_tokens SET expires_at=TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'");}return null;});
        assertThat(call("GET", "/users/me", null, expired).statusCode()).isEqualTo(401);
        String disabled=login("100001");
        database.transaction(c->{try(var s=c.createStatement()){s.executeUpdate("UPDATE users SET status='DISABLED'");}return null;});
        assertThat(call("GET", "/users/me", null, disabled).statusCode()).isEqualTo(401);
        assertThat(call("POST", "/auth/login", Map.of("account","100001","password",PASSWORD),null).statusCode()).isEqualTo(401);
    }
    @Test void twoUsersCannotModifyEachOthersProfileOrAccountStatus() throws Exception {
        register("100002");register("100003");String alice=login("100002"), bob=login("100003");
        long bobId=data(call("GET","/users/me",null,bob)).path("id").asLong();
        var changed=call("PUT","/users/me",Map.of("nickname","Alice","phone","13800138000","avatarUrl","","id",bobId,"status","DISABLED","account","hacked"),alice);
        assertThat(changed.statusCode()).isEqualTo(200);
        assertThat(data(changed).path("account").asText()).isEqualTo("100002");
        assertThat(data(changed).path("status").asText()).isEqualTo("ACTIVE");
        assertThat(data(call("GET","/users/me",null,bob)).path("nickname").asText()).isEqualTo("同学");
    }
    @Test void avatarAndProfileValidationAreEnforcedOnServer() throws Exception {
        register("100001");String token=login("100001");
        for(String avatar:List.of("javascript:alert(1)","https://", "https://user:secret@example.com/avatar.png"))
            assertThat(call("PUT","/users/me",Map.of("nickname","同学","avatarUrl",avatar),token).statusCode()).isEqualTo(400);
        assertThat(call("PUT","/users/me",Map.of("nickname"," "),token).statusCode()).isEqualTo(400);
        assertThat(call("PUT","/users/me",Map.of("nickname","新昵称","avatarUrl","https://example.com/a.png","phone",""),token).statusCode()).isEqualTo(200);
    }
    @Test void invalidPhoneRegistrationDoesNotCreateAnAccount() throws Exception {
        for (String phone : List.of("abc-not-a-phone", "123", "12800138000", "138001380000", "1380013800a", "   ")) {
            var response = call("POST", "/auth/register", Map.of("account", "100004", "password", PASSWORD, "nickname", "同学", "phone", phone), null);
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(json.readTree(response.body()).path("message").asText()).isEqualTo("请输入11位中国大陆手机号，或留空");
        }
        long count = database.read(c -> { try (var s=c.createStatement(); var r=s.executeQuery("SELECT COUNT(*) FROM users")) { r.next(); return r.getLong(1); } });
        assertThat(count).isZero();
    }
    @Test void validAndOptionalPhonesCanBeRegistered() throws Exception {
        int index = 0;
        for (String phone : List.of("", "13800138000", "19900199000")) {
            var response = call("POST", "/auth/register", Map.of("account", "200" + index++, "password", PASSWORD, "nickname", "同学", "phone", phone), null);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(data(response).path("phone").asText()).isEqualTo(phone);
        }
        // 未提供和显式 null 同样是选填，不会使旧客户端请求失效。
        register("100005");
        var body = new java.util.HashMap<String, Object>(Map.of("account", "100006", "password", PASSWORD, "nickname", "同学"));
        body.put("phone", null);
        assertThat(call("POST", "/auth/register", body, null).statusCode()).isEqualTo(200);
    }
    @Test void invalidPhoneUpdatePreservesProfileAndValidPhoneCanBeCleared() throws Exception {
        register("100001"); String token = login("100001");
        var saved = call("PUT", "/users/me", Map.of("nickname", "原昵称", "phone", "13800138000"), token);
        assertThat(saved.statusCode()).isEqualTo(200);
        for (String phone : List.of("abc-not-a-phone", "123", "12800138000", "138001380000", "1380013800a", "   ")) {
            assertThat(call("PUT", "/users/me", Map.of("nickname", "不应保存", "phone", phone), token).statusCode()).isEqualTo(400);
        }
        var unchanged = data(call("GET", "/users/me", null, token));
        assertThat(unchanged.path("nickname").asText()).isEqualTo("原昵称");
        assertThat(unchanged.path("phone").asText()).isEqualTo("13800138000");
        var cleared = call("PUT", "/users/me", Map.of("nickname", "原昵称", "phone", ""), token);
        assertThat(cleared.statusCode()).isEqualTo(200);
        assertThat(data(cleared).path("phone").asText()).isEmpty();
    }
    @Test void blankNicknameHasAnExplicitMessageForRegistrationAndUpdate() throws Exception {
        var response = call("POST", "/auth/register", Map.of("account", "100007", "password", PASSWORD, "nickname", "   "), null);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json.readTree(response.body()).path("message").asText()).isEqualTo("昵称不能为空");
        register("100001"); String token = login("100001");
        var update = call("PUT", "/users/me", Map.of("nickname", "   "), token);
        assertThat(update.statusCode()).isEqualTo(400);
        assertThat(json.readTree(update.body()).path("message").asText()).isEqualTo("昵称不能为空");
        assertThat(data(call("GET", "/users/me", null, token)).path("nickname").asText()).isEqualTo("同学");
    }
    @Test void logoutRevokesOnlyCurrentTokenAndIsIdempotent() throws Exception {
        register("100001");String one=login("100001"),two=login("100001");
        assertThat(call("POST","/auth/logout",null,one).statusCode()).isEqualTo(204);
        assertThat(call("GET","/users/me",null,one).statusCode()).isEqualTo(401);
        assertThat(call("GET","/users/me",null,two).statusCode()).isEqualTo(200);
        assertThat(call("POST","/auth/logout",null,one).statusCode()).isEqualTo(204);
        assertThat(call("POST","/auth/logout",null,"x".repeat(43)).statusCode()).isEqualTo(401);
    }
    @Test void wrongOldPasswordDoesNotChangeAnything() throws Exception {
        register("100001");String token=login("100001");
        assertThat(call("PUT","/users/me/password",Map.of("oldPassword","incorrect1","newPassword","newPass123"),token).statusCode()).isEqualTo(400);
        assertThat(call("GET","/users/me",null,token).statusCode()).isEqualTo(200);
        assertThat(login("100001")).isNotBlank();
    }
    @Test void passwordChangeRevokesAllSessionsAndOldPassword() throws Exception {
        register("100001");String one=login("100001"),two=login("100001");
        assertThat(call("PUT","/users/me/password",Map.of("oldPassword",PASSWORD,"newPassword","newPass123"),one).statusCode()).isEqualTo(204);
        assertThat(call("GET","/users/me",null,one).statusCode()).isEqualTo(401);
        assertThat(call("GET","/users/me",null,two).statusCode()).isEqualTo(401);
        assertThat(call("POST","/auth/login",Map.of("account","100001","password",PASSWORD),null).statusCode()).isEqualTo(401);
        assertThat(call("POST","/auth/login",Map.of("account","100001","password","newPass123"),null).statusCode()).isEqualTo(200);
    }
    @Test void passwordAndTokenChangesRollBackTogetherOnStorageFailure() throws Exception {
        register("100001");String token=login("100001");
        database.transaction(c->{try(var s=c.createStatement()){s.execute("ALTER TABLE auth_tokens ADD CONSTRAINT test_no_revoke CHECK(revoked_at IS NULL)");}return null;});
        try {
            var response=call("PUT","/users/me/password",Map.of("oldPassword",PASSWORD,"newPassword","newPass123"),token);
            assertThat(response.statusCode()).isEqualTo(500);
            assertThat(response.body()).doesNotContain("SQLException","constraint","password_hash");
            assertThat(call("GET","/users/me",null,token).statusCode()).isEqualTo(200);
            assertThat(login("100001")).isNotBlank();
        } finally {
            database.transaction(c->{try(var s=c.createStatement()){s.execute("ALTER TABLE auth_tokens DROP CONSTRAINT test_no_revoke");}return null;});
        }
    }
    @Test void facadeExposesOnlyActiveUsers() throws Exception {
        register("100001");long id=data(call("GET","/users/me",null,login("100001"))).path("id").asLong();
        assertThat(service.existsActiveUser(id)).isTrue();assertThat(service.getActiveUser(id).id()).isEqualTo(id);
        assertThat(service.existsActiveUser(-1)).isFalse();assertThatThrownBy(()->service.getActiveUser(-1)).isInstanceOf(UserFailure.class);
    }
    @Test void rateLimiterBoundsRequestsAndReturnsStructured429() throws Exception {
        for(int i=0;i<60;i++)limiter.check("login-ip:127.0.0.1",120,60_000);
        for(int i=0;i<60;i++)limiter.check("login-ip:127.0.0.1",120,60_000);
        var response=call("POST","/auth/login",Map.of("account","100001","password",PASSWORD),null);
        assertThat(response.statusCode()).isEqualTo(429);
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo("RATE_LIMITED");
        assertThat(response.headers().firstValue("Retry-After")).hasValue("60");
    }
    @Test void fiftyDuplicateRegistrationsCreateExactlyOneUser() throws Exception {
        var responses=concurrent("duplicate-register-50",i->call("POST","/auth/register",Map.of("account","100008","password",PASSWORD,"nickname","并发同学"),null));
        assertThat(responses.stream().filter(r->r.statusCode()==200).count()).isEqualTo(1);
        assertThat(responses.stream().filter(r->r.statusCode()==409).count()).isEqualTo(49);
        long count=database.read(c->{try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM users")){r.next();return r.getLong(1);}});
        assertThat(count).isEqualTo(1);
    }
    @Test void fiftyAccountsCanLoginAndAccessOnlyTheirOwnIdentity() throws Exception {
        String hash=hasher.hash(PASSWORD);
        database.transaction(c->{
            try(var s=c.prepareStatement("INSERT INTO users(account,password_hash,nickname) VALUES(?,?,?)")){
                for(int i=0;i<50;i++){s.setString(1,"300"+i);s.setString(2,hash);s.setString(3,"User "+i);s.addBatch();}s.executeBatch();
            }return null;
        });
        var logins=concurrent("login-50-accounts",i->call("POST","/auth/login",Map.of("account","300"+i,"password",PASSWORD),null));
        List<String> tokens=new ArrayList<>();
        for(int i=0;i<50;i++){
            assertThat(logins.get(i).statusCode()).isEqualTo(200);
            assertThat(data(logins.get(i)).path("user").path("account").asText()).isEqualTo("300"+i);
            tokens.add(data(logins.get(i)).path("token").asText());
        }
        assertThat(tokens).doesNotHaveDuplicates();
        var identities=concurrent("me-50-accounts",i->call("GET","/users/me",null,tokens.get(i)));
        for(int i=0;i<50;i++){
            assertThat(identities.get(i).statusCode()).isEqualTo(200);
            assertThat(data(identities.get(i)).path("account").asText()).isEqualTo("300"+i);
        }
    }
    @Test void numericAccountAndMixedPasswordBoundaries() throws Exception {
        for (String account : List.of("abc123", "12345678901234567", "12", "１２３４"))
            assertThat(call("POST", "/auth/register", Map.of("account",account,"password",PASSWORD,"nickname","同学"),null).statusCode()).isEqualTo(400);
        for (String password : List.of("12345678", "abcdefgh", "Abc12345678901234X", "Abc1234")) {
            assertThat(call("POST", "/auth/register", Map.of("account","123456","password",password,"nickname","同学"),null).statusCode()).isEqualTo(400);
            assertThat(call("POST", "/auth/login", Map.of("account","123456","password",password),null).statusCode()).isEqualTo(400);
        }
        var body=Map.of("account","0012345678901234","password","Abc1234567890123","nickname","同学");
        assertThat(call("POST","/auth/register",body,null).statusCode()).isEqualTo(200);
        assertThat(data(call("POST","/auth/login",body,null)).path("user").path("account").asText()).isEqualTo("0012345678901234");
    }
    private HttpResponse<String> upload(byte[] bytes, String token) throws Exception {
        var out=new java.io.ByteArrayOutputStream();
        out.write("--avatar-test\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.png\"\r\nContent-Type: image/png\r\n\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.write(bytes); out.write("\r\n--avatar-test--\r\n".getBytes());
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/users/me/avatar"))
                .header("Content-Type","multipart/form-data; boundary=avatar-test");
        if(token!=null)request.header("Authorization","Bearer "+token);
        return http.send(request.POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void uploadedAvatarIsCenteredCircularAndPersistsWithoutChangingProfile() throws Exception {
        register("100001");String token=login("100001");
        var source=new java.awt.image.BufferedImage(600,200,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g=source.createGraphics();g.setColor(java.awt.Color.RED);g.fillRect(0,0,600,200);g.setColor(java.awt.Color.BLUE);g.fillRect(200,0,200,200);g.dispose();
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(source,"PNG",bytes);
        assertThat(upload(bytes.toByteArray(),null).statusCode()).isEqualTo(401);
        assertThat(upload("not an image".getBytes(),token).statusCode()).isEqualTo(400);
        var response=upload(bytes.toByteArray(),token);assertThat(response.statusCode()).isEqualTo(200);
        String url=data(response).path("avatarUrl").asText();
        assertThat(data(call("GET","/users/me",null,token)).path("avatarUrl").asText()).isEqualTo(url);
        var imageResponse=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+url)).build(),HttpResponse.BodyHandlers.ofByteArray());
        assertThat(imageResponse.statusCode()).isEqualTo(200);
        var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(imageResponse.body()));
        assertThat(image.getWidth()).isEqualTo(256);assertThat(image.getHeight()).isEqualTo(256);
        assertThat(image.getRGB(0,0)>>>24).isZero();assertThat(image.getRGB(128,128)).isEqualTo(java.awt.Color.BLUE.getRGB());
        assertThat(call("PUT","/users/me",Map.of("nickname","新昵称","avatarUrl",url),token).statusCode()).isEqualTo(200);
        assertThat(data(call("GET","/users/me",null,token)).path("avatarUrl").asText()).isEqualTo(url);
        register("100002");assertThat(data(call("GET","/users/me",null,login("100002"))).path("avatarUrl").asText()).isEmpty();
    }
    private interface Request { HttpResponse<String> send(int i) throws Exception; }
    private List<HttpResponse<String>> concurrent(String name,Request request) throws Exception {
        var pool=Executors.newFixedThreadPool(50);var gate=new CountDownLatch(1);
        List<Future<HttpResponse<String>>> tasks=new ArrayList<>();List<Double> elapsed=java.util.Collections.synchronizedList(new ArrayList<>());
        try{
            for(int i=0;i<50;i++){int index=i;tasks.add(pool.submit(()->{gate.await();long start=System.nanoTime();try{return request.send(index);}finally{elapsed.add((System.nanoTime()-start)/1_000_000.0);}}));}
            long start=System.nanoTime();gate.countDown();List<HttpResponse<String>> results=new ArrayList<>();
            for(var task:tasks)results.add(task.get(120,java.util.concurrent.TimeUnit.SECONDS));
            double total=(System.nanoTime()-start)/1_000_000.0;elapsed.sort(Double::compareTo);
            var summary=Map.of("scenario",name,"concurrency",50,"totalMs",total,"p50Ms",elapsed.get(24),"p95Ms",elapsed.get(47),"maxMs",elapsed.get(49),"statuses",results.stream().collect(java.util.stream.Collectors.groupingBy(HttpResponse::statusCode,java.util.stream.Collectors.counting())));
            Path out=Path.of("target/user/"+name+".json");Files.createDirectories(out.getParent());json.writerWithDefaultPrettyPrinter().writeValue(out.toFile(),summary);
            System.out.println("PERFORMANCE "+json.writeValueAsString(summary));return results;
        }finally{pool.shutdownNow();}
    }
}
