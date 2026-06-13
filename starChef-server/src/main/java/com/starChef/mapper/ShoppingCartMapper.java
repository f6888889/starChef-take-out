package com.starChef.mapper;

import com.starChef.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    /**
     * 动态条件查询当前用户的购物车数据
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 根据ID修改商品数量
     * @param shoppingCart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart shoppingCart);

    /**
     * 插入购物车数据
     */
    @Insert("insert into shopping_cart (name, image, dish_id, setmeal_id, dish_flavor, number, amount, create_time, user_id) " +
            "VALUES (#{name}, #{image}, #{dishId}, #{setmealId}, #{dishFlavor}, #{number}, #{amount}, #{createTime}, #{userId})")
    void insert(ShoppingCart shoppingCart);

    /**
     * 根据用户id删除购物数全部车据
     * @param userId
     */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId);

    /**
     * 根据购物车id删除购物车数据
     */
    @Delete("delete from shopping_cart where id = #{id} and user_id = #{userId}")
    void deleteByShoppingCartId(ShoppingCart deleteCart);
}
