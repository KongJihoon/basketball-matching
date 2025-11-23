package com.example.basketballmatching.blackList.service;

import com.example.basketballmatching.blackList.dto.BlackListDto;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BlackListService {


    CheckResponse createBlackListUser(Long userId, Long reportId);

    CommonResponse<Page<BlackListDto>> getBlackLists(Long userId, Pageable pageable);

}
