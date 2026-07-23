//package com.example.basketballmatching.friend.service.impl;
//
//import com.example.basketballmatching.friend.repository.FriendRepository;
//import com.example.basketballmatching.friend.type.FriendStatus;
//import com.example.basketballmatching.global.dto.CheckResponse;
//import com.example.basketballmatching.notifications.service.NotificationService;
//import com.example.basketballmatching.notifications.type.NotificationType;
//import com.example.basketballmatching.user.entity.UserEntity;
//import com.example.basketballmatching.user.repository.UserRepository;
//import com.example.basketballmatching.user.type.GenderType;
//import com.example.basketballmatching.user.type.UserType;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class FriendServiceImplTest {
//
//
//    @Mock
//    private FriendRepository friendRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private NotificationService notificationService;
//
//    @InjectMocks
//    private FriendServiceImpl friendService;
//
//    private UserEntity requestor;
//
//    private UserEntity receiver;
//
//    @BeforeEach
//    public void setUp() {
//
//        requestor = UserEntity.builder()
//                .userId(1L)
//                .emailAuth(true)
//                .nickname("요청자")
//                .userType(UserType.USER)
//                .build();
//
//        receiver = UserEntity.builder()
//                .userId(2L)
//                .nickname("수신자")
//                .emailAuth(true)
//                .genderType(GenderType.MALE)
//                .userType(UserType.USER)
//                .build();
//
//
//    }
//
//    @Test
//    @DisplayName("친구 요청 테스트")
//    void createFriendTest() {
//        // given
//
//        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(requestor.getUserId()))
//                .thenReturn(Optional.of(requestor));
//
//        when(userRepository.findByNicknameAndDeletedDateTimeIsNull(receiver.getNickname()))
//                .thenReturn(Optional.of(receiver));
//        when(friendRepository.existsFriendBetween(requestor, receiver, FriendStatus.ACCEPT))
//                .thenReturn(false);
//
//        // when
//
//        CheckResponse checkResponse = friendService.createFried(requestor.getUserId(), receiver.getNickname());
//
//
//        // then
//
//        verify(friendRepository).save(any());
//        verify(notificationService).send(
//                eq(NotificationType.REQUEST_FRIEND),
//                eq(receiver),
//                anyString()
//        );
//        assertEquals("친구 요청이 완료되었습니다.", checkResponse.getMessage());
//
//    }
//
//}