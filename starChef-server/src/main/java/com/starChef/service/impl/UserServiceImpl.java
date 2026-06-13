package com.starChef.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.starChef.constant.MessageConstant;
import com.starChef.dto.UserLoginDTO;
import com.starChef.entity.User;
import com.starChef.exception.LoginFailedException;
import com.starChef.mapper.UserMapper;
import com.starChef.properties.WeChatProperties;
import com.starChef.service.UserService;
import com.starChef.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    //微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties wechatProperties;

    @Autowired
    private UserMapper userMapper;

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    public User wxLogin(UserLoginDTO userLoginDTO){
        
        String openid = getOpenid(userLoginDTO.getCode());

        //判断openId是否为空，如果为空则登录失败
        if(openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        //判断当前用户是否为新用户
        User user = userMapper.getByOpenid(openid);

        //如果是新用户，自动完成注册
        if(user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        //返回登录成功的用户数据
        return user;
    }

    /**
     * 调用微信接口服务，获取当前微信用户的openId
     * @param code
     * @return
     */
    private String getOpenid(String code) {
        //调用微信接口服务，获取当前微信用户的openId
        Map<String, String> map = new HashMap<>();
        map.put("appid", wechatProperties.getAppid());
        map.put("secret", wechatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
     }

}
