package com.example.basketballmatching.global.service;

import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class MailService {

    private final JavaMailSender javaMailSender;

    private final RedisService redisService;

    private static final Long EMAIL_TOKEN_EXPIRE = 3L;

    private static final int CODE_LENGTH = 6;

    private static final String EMAIL_PREFIX = "email:auth:";


    @Transactional
    public CheckResponse sendAuthMail(String email) {

        String code = createRandomCode();

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "UTF-8");


        try {
            log.info("이메일 인증번호 전송 시작 : {}", email);

            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSubject("회원가입 이메일 인증번호입니다.");

            String msg = "<div style='margin:20px'>" +
                    "<h1> 안녕하세요 농구 매칭 서비스입니다. </h1>" +
                    "<br/>" +
                    "<p>아래 코드를 입력해주세요.</p>" +
                    "<br/>" +
                    "<div align='center' style='border:1px solid black; font-family:verdana';>" +
                    "<h3 style='color:blue;'>회원가입 인증번호입니다.</h3>" +
                    "<div style='font-size:130%'>" +
                    "CODE : <strong>" + code + "</strong></div>" +
                    "<br/>" +
                    "</div>";

            mimeMessageHelper.setText(msg, true);

            javaMailSender.send(mimeMessage);

            redisService.setDataExpireMinutes(EMAIL_PREFIX + email, code, EMAIL_TOKEN_EXPIRE);



            log.info("이메일 인증번호 전송완료.");

        } catch (MessagingException e) {
            log.error("이메일 전송 오류 : {}", e.getMessage());
            throw new CustomException(INTERNAL_SERVER_ERROR);
        }

        return CheckResponse.of(true, "이메일 인증번호가 전송되었습니다.");

    }



    @Transactional
    public CheckResponse verifyEmailAuth(String email, String code) {

        String data = redisService.getData(EMAIL_PREFIX + email);

        if (data == null || !data.equals(code)) {
            throw new CustomException(INVALID_CODE);
        }

        redisService.deleteData(EMAIL_PREFIX + email);

        redisService.setDataExpireMinutes("email:auth:verified:" + email, code, 10L);

        return CheckResponse.of(true, "이메일 인증에 성공하였습니다.");
    }


    private String createRandomCode() {

        Random random = new Random();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {

            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }

}
