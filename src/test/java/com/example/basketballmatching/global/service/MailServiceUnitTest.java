package com.example.basketballmatching.global.service;

import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static com.example.basketballmatching.global.exception.ErrorCode.INVALID_CODE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceUnitTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private RedisService redisService;



    @InjectMocks
    private MailService mailService;


    @Test
    @DisplayName("이메일 인증번호 전송 테스트 - 전송 호출 확인 및 Redis 인증 코드 저장 확인")
    void sendAuthMailTest() {
        // given

        String email = "test@test.com";

        Session session = Session.getInstance(new Properties());

        MimeMessage mimeMessage = new MimeMessage(session);

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when

        mailService.sendAuthMail(email);
        verify(javaMailSender).send(mimeMessage);


        // then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);

        verify(redisService).setDataExpireMinutes(keyCaptor.capture(), codeCaptor.capture(), ttlCaptor.capture());

        assertEquals("email:auth:" + email, keyCaptor.getValue());
        assertEquals(3L, ttlCaptor.getValue());

        String code = codeCaptor.getValue();

        assertNotNull(code);
        assertEquals(6, code.length());


    }

    @Test
    @DisplayName("이메일 인증번호 전송 실패 테스트 - 메일 전송 실패")
    void sendAuthMailFailTest_Mail_Exception() {
        // given

        String email = "test@test.com";

        Session session = Session.getInstance(new Properties());
        MimeMessage mimeMessage = new MimeMessage(session);

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when

        doThrow(new MailSendException("smtp fail")).when(javaMailSender).send(mimeMessage);

        // then

        CustomException exception = assertThrows(CustomException.class, () -> mailService.sendAuthMail(email));

        verify(redisService, never()).setDataExpireMinutes(anyString(), anyString(), anyLong());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode());


    }

    @Test
    @DisplayName("이메일 인증번호 확인 테스트")
    void verifyEmailAuthTest() {
        // given

        String email = "test@test.com";
        String code = "123456";

        when(redisService.getData("email:auth:" + email)).thenReturn(code);



        // when

//        CheckResponse checkResponse = mailService.verifyEmailAuth(email, code);
        // then

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);


        verify(redisService).deleteData("email:auth:" + email);
        verify(redisService).setDataExpireMinutes(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());



//        assertTrue(checkResponse.isSuccess());
//        assertEquals("이메일 인증에 성공하였습니다.", checkResponse.getMessage());

        assertEquals("email:auth:verified:" + email, keyCaptor.getValue());
        assertEquals(code, valueCaptor.getValue());
        assertEquals(10L, ttlCaptor.getValue());

    }

    @Test
    @DisplayName("이메일 인증번호 확인 실패 테스트 - Redis에 저장된 코드가 없는 경우")
    void verifyEmailAuthFailTest_INVALID_CODE() {
        // given

        String email = "test@test.com";
        String code = "123456";

        when(redisService.getData("email:auth:" + email)).thenReturn(null);


        // when

        CustomException exception = assertThrows(CustomException.class, () -> mailService.verifyEmailAuth(email, code));


        // then

        assertEquals(INVALID_CODE, exception.getErrorCode());

        verify(redisService, never()).deleteData(anyString());
        verify(redisService, never()).setDataExpireMinutes(anyString(), anyString(), anyLong());

    }


}