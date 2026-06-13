package com.starChef.service.impl;

import com.fasterxml.jackson.databind.util.BeanUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.starChef.constant.MessageConstant;
import com.starChef.constant.StatusConstant;
import com.starChef.context.BaseContext;
import com.starChef.dto.SetmealDTO;
import com.starChef.dto.SetmealPageQueryDTO;
import com.starChef.entity.Setmeal;
import com.starChef.entity.SetmealDish;
import com.starChef.exception.DeletionNotAllowedException;
import com.starChef.mapper.SetmealDishMapper;
import com.starChef.mapper.SetmealMapper;
import com.starChef.result.PageResult;
import com.starChef.service.SetmealService;
import com.starChef.vo.DishItemVO;
import com.starChef.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());

        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        long total = page.getTotal();
        List<SetmealVO> records = page.getResult();

        return new PageResult(total, records);

    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    public void update(SetmealDTO setmealDTO){
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //修改套餐数据
        setmealMapper.update(setmeal);

        //删除套餐菜品关系数据
        setmealDishMapper.deleteBySetmealId(setmeal.getId());

        //重新插入套餐菜品关系数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes != null && setmealDishes.size() > 0){
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmeal.getId());
            });
            //向菜品表插入n条数据
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    /**
     * 根据id查询套餐和套餐菜品关系
     * @param id
     * @return
     */
    public SetmealVO getByIdWithDish(Long id) {
        //根据id查询套餐数据
        Setmeal setmeal = setmealMapper.getById(id);

        //根据id查询套餐菜品关系数据
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);

        //将数据封装到VO
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);

        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }
    /**
     * 套餐起售停售
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO){
        Setmeal setmeal = new Setmeal();

        BeanUtils.copyProperties(setmealDTO, setmeal);

        //插入一条数据
        setmealMapper.insert(setmeal);

        //获取主键
        Long setmealId = setmeal.getId();

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes != null && setmealDishes.size() > 0){
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            //向菜品表插入n条数据
            setmealDishMapper.insertBatch(setmealDishes);
        }

    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {

        for(Long id : ids){
            Setmeal setmeal = setmealMapper.getById(id);
            if(setmeal.getStatus() == StatusConstant.ENABLE){
                //当前菜品正在起售中
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        setmealMapper.deleteBatch(ids);


    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

}
