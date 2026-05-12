package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.interfaces.Func;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public List<ShopType> queryShopType() {
        //1.查询redis
        String shoptype = stringRedisTemplate.opsForValue().get("cache:shopType");
        //2.有，直接返回
        if(StrUtil.isNotBlank(shoptype)){
            List<ShopType> typeList = JSONUtil.toList(shoptype, ShopType.class);
            return typeList;
        }
        //3.无，查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        //4.无，异常
        if (typeList == null || typeList.isEmpty()) {
            return null;
        }
        //5.有，写入redis
        stringRedisTemplate.opsForValue().set("cache:shopType", JSONUtil.toJsonStr(typeList), 30, TimeUnit.MINUTES);
        //6.返回数据
        return typeList;
    }
}
