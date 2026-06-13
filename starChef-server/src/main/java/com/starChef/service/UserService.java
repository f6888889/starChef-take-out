package com.starChef.service;

import com.starChef.dto.UserLoginDTO;
import com.starChef.entity.User;

public interface UserService {

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}
