package com.future.login.service;

import com.future.login.domain.dto.RejesterDTO;
import entity.pojo.util.Result;
import org.springframework.stereotype.Service;

public interface RejesterService {
    Result rejester(RejesterDTO rejesterDTO);

}
