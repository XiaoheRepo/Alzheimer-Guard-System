package com.xiaohelab.guard.server.auth.service;

import com.xiaohelab.guard.server.auth.dto.LoginRequest;
import com.xiaohelab.guard.server.auth.dto.RegisterRequest;
import com.xiaohelab.guard.server.auth.dto.TokenResponse;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.JwtTokenProvider;
import com.xiaohelab.guard.server.gov.repository.WsTicketRepository;
import com.xiaohelab.guard.server.user.entity.UserEntity;
import com.xiaohelab.guard.server.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link AuthService} 单元测试：注册、登录、刷新、登出黑名单。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider tokenProvider;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock WsTicketRepository wsTicketRepository;
    @Mock HttpServletRequest httpRequest;

    @InjectMocks AuthService authService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(authService, "wsTicketTtl", 30);
    }

    @Test
    void register_should_reject_duplicate_username() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice"); req.setEmail("a@b.c"); req.setPassword("Aa123456!");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.E_GOV_4091);
    }

    @Test
    void register_should_reject_duplicate_email() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("a@b.c")).thenReturn(true);
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice"); req.setEmail("a@b.c"); req.setPassword("Aa123456!");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.E_GOV_4092);
    }

    @Test
    void register_should_persist_and_hash_password() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("a@b.c")).thenReturn(false);
        when(passwordEncoder.encode("Aa123456!")).thenReturn("$2a$hashed");
        when(userRepository.save(any())).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0); u.setId(1L); return u;
        });

        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice"); req.setEmail("a@b.c"); req.setPassword("Aa123456!");
        var r = authService.register(req);

        assertThat(r.getUsername()).isEqualTo("alice");
        assertThat(r.getRole()).isEqualTo("FAMILY");
        verify(passwordEncoder).encode("Aa123456!");
    }

    @Test
    void login_should_reject_wrong_password() {
        UserEntity u = buildUser("alice", "ACTIVE");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrong", u.getPasswordHash())).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setUsername("alice"); req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req, httpRequest))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.E_AUTH_4011);
    }

    @Test
    void login_should_reject_when_account_disabled() {
        UserEntity u = buildUser("alice", "DISABLED");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("ok", u.getPasswordHash())).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setUsername("alice"); req.setPassword("ok");

        assertThatThrownBy(() -> authService.login(req, httpRequest))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.E_GOV_4031);
    }

    @Test
    void login_should_issue_access_and_refresh_tokens() {
        UserEntity u = buildUser("alice", "ACTIVE");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("ok", u.getPasswordHash())).thenReturn(true);
        when(tokenProvider.issueAccessToken(1L, "alice", "FAMILY")).thenReturn("ACCESS");
        when(tokenProvider.issueRefreshToken(1L, "alice", "FAMILY")).thenReturn("REFRESH");
        when(tokenProvider.accessExpirationSeconds()).thenReturn(86400L);

        LoginRequest req = new LoginRequest();
        req.setUsername("alice"); req.setPassword("ok");

        TokenResponse r = authService.login(req, httpRequest);

        assertThat(r.getAccessToken()).isEqualTo("ACCESS");
        assertThat(r.getRefreshToken()).isEqualTo("REFRESH");
        assertThat(r.getUserId()).isEqualTo(1L);
    }

    @Test
    void logout_should_add_token_to_blacklist() {
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000L));
        when(tokenProvider.parse("tok")).thenReturn(claims);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        authService.logout("Bearer tok");

        verify(valueOps).set(anyString(), eq("1"), any(Duration.class));
    }

    @Test
    void logout_should_be_noop_when_header_missing() {
        authService.logout(null);
        verify(redisTemplate, never()).opsForValue();
    }

    private UserEntity buildUser(String username, String status) {
        UserEntity u = new UserEntity();
        u.setId(1L); u.setUsername(username);
        u.setPasswordHash("$2a$hashed"); u.setRole("FAMILY");
        u.setStatus(status); u.setEmailVerified(true);
        return u;
    }
}
