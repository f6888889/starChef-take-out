package com.starChef.service.impl;

import com.starChef.context.BaseContext;
import com.starChef.dto.ShoppingCartDTO;
import com.starChef.entity.Dish;
import com.starChef.entity.Setmeal;
import com.starChef.entity.ShoppingCart;
import com.starChef.mapper.DishMapper;
import com.starChef.mapper.SetmealMapper;
import com.starChef.mapper.ShoppingCartMapper;
import com.starChef.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j

public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;


    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //判断商品在购物车中是否已经存在
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);


        //如果已经存在了，数量加一
        if (list != null && list.size() > 0) {
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        }else{
            //如果不存在，添加到购物车，数量默认为1

            //判断本次到购物车的是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId != null){
                //本次添加到购物车的是菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());

            }else{
                //本次添加到购物车的是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();

                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());

            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 购物车商品减一
     * @return
     */
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO){

        //创建一个shoppingCart
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        //查询当前用户的购物车数据
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        ShoppingCart cart = list.get(0);

        //判断剩余数量是否为1,为一就删除，不为一就减
        Integer number = cart.getNumber();
        if (number == 1) {
            ShoppingCart deleteCart = new ShoppingCart();
            deleteCart.setId(cart.getId());
            deleteCart.setUserId(userId);
            shoppingCartMapper.deleteByShoppingCartId(deleteCart);
        }else{
            cart.setNumber(number - 1);
            shoppingCartMapper.updateNumberById(cart);
        }

    }

    /**
     * 查看购物车
     * @return
     */
    public List<ShoppingCart> showShoppingCart(){
        //设置用户id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }

    /**
     * 清空购物车
     */
    public void cleanShoppingCart(){
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }

}
