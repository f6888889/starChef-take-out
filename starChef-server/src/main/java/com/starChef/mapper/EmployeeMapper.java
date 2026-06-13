package com.starChef.mapper;

import com.github.pagehelper.Page;
import com.starChef.annotation.AutoFill;
import com.starChef.dto.EmployeePageQueryDTO;
import com.starChef.entity.Employee;
import com.starChef.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);


    /**
     * 插入员工数据
     * @param employee
     */
    @Insert("insert into employee (name, username, password, phone, sex, id_number, create_time, update_time, create_user, update_user) " +
            "values " +
            "(#{name}, #{username}, #{password}, #{phone}, #{sex}, #{idNumber}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @AutoFill(value = OperationType.INSERT)
    void insert(Employee employee);


    /**
     * 分页查询
     * @param employeePageQueryDTO
     */

    Page<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    //修改员工数据，启用禁用员工
    @AutoFill(value = OperationType.UPDATE)
    void update(Employee employee);

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    @Select("select * from employee where id = #{id}")
    Employee getById(Long id);
}
