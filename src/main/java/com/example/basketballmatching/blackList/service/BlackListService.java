package com.example.basketballmatching.blackList.service;

import com.example.basketballmatching.blackList.dto.BlackListDto;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BlackListService {


    CheckResponse createBlackListUser(Long userId, Long reportId);

    ApiResponse<Page<BlackListDto>> getBlackLists(Long userId, Pageable pageable);

}
