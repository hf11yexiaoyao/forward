package com.ntech.service.inf;

import com.ntech.model.SetMeal;

import java.util.List;

/**
 * 套餐服务接口
 */
public interface ISetMealService {
    boolean add(SetMeal setMeal);
    boolean delete(String userName);
    List<SetMeal> findAll();
    SetMeal findByName(String name);
    boolean modify(SetMeal setMeal);
    int totalCount();
}
