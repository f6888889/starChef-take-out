package com.starChef.controller.user;

import com.starChef.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "店铺相关接口")
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/status")
    @ApiOperation("获取店铺的营业状态")
    public Result<Integer> getStatus(){
        Integer status = (Integer)redisTemplate.opsForValue().get(KEY);
        log.info("获取店铺的营业状态为：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }

}
