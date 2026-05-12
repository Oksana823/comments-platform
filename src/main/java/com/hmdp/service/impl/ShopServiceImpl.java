package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient  cacheClient;

    @Override
    public Result queryById(Long id) {
        //缓存穿透
        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id,
                CACHE_SHOP_TTL, TimeUnit.MINUTES, Shop.class, this::getById);
//        Shop shop = queryWithPassThrough(id);
//        Shop shop = queryWithMutex(id);
//        Shop shop = queryLogicalExpire(id);
//        Shop shop = cacheClient.queryLogicalExpire(CACHE_SHOP_KEY,id,
//                Shop.class,this::getById,20L,TimeUnit.SECONDS);
        if(shop == null){
            return  Result.fail("The Shop doesn't exist!!!!");
        }
        return Result.ok(shop);
    }
//    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
//    public Shop queryLogicalExpire(Long id){
//        //1.从redis查询缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+ id);
//        //2.判断是否存在
//        if(StrUtil.isBlank(shopJson)){
//            return null ;
//        }
//        //判断过期时间，需要先把json反序列化为对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//
//        //未过期，直接返回店铺信息
//        if(expireTime.isAfter(LocalDateTime.now())){
//            return shop;
//        }
//        //过期，需要重建
//        //获取互斥锁
//        String lockey = LOCK_SHOP_KEY+ id;
//        Boolean islock = tryLock(lockey);
//        //判断是否获取成功
//        if(islock){
//            //成功，开启独立线程
//            CACHE_REBUILD_EXECUTOR.submit(()->{
//                try {
//                    this.saveShop2Redis(id,20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    //释放锁
//                    unlock(lockey);
//                }
//
//            });
//        }
//        //返回过期信息
//        return shop;
//    }
//    public Shop queryWithMutex(Long id){
//        //1.从redis查询缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+ id);
//        //2.判断是否存在
//        if(StrUtil.isNotBlank(shopJson)){
//            //3.存在，直接返回
//            Shop shop = JSONUtil.toBean(shopJson,Shop.class);
//            return shop;
//        }
//        //判断命中是否是空值
//        if(shopJson != null){
//            return  null;
//        }
//        //4.不存在，根据ID查询数据库
//        //尝试获取互斥锁
//        String lockKey = LOCK_SHOP_KEY+id;
//        Shop shop = null;
//        try {
//            Boolean isLock = tryLock(lockKey);
//            if(!isLock){
//                //获取失败
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//            //获取锁成功
//            shop = getById(id);
//            //模拟重建延时
//            Thread.sleep(200);
//            //5.不存在，返回错误
//            if(shop == null){
//                //将空值写入redis
//                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+ id,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return  null;
//            }
//            //6.存在，写入redis
//            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+ id,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            //释放互斥锁
//            unlock(lockKey);
//        }
//        //7.返回数据
//        return shop;
//    }
    public Shop queryWithPassThrough(Long id){
        //1.从redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+ id);
        //2.判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            //3.存在，直接返回
            Shop shop = JSONUtil.toBean(shopJson,Shop.class);
            return shop;
        }
        //判断命中是否是空值
        if(shopJson != null){
            return  null;
        }
        //4.不存在，根据ID查询数据库
        Shop shop = getById(id);
        //5.不存在，返回错误
        if(shop == null){
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+ id,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return  null;
        }
        //6.存在，写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+ id,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //7.返回数据
        return shop;
    }
//    private Boolean tryLock(String key){
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//    private void unlock(String key){
//        stringRedisTemplate.delete(key);
//    }
//    public void saveShop2Redis(Long id,Long Senconds) throws InterruptedException {
//        //查询店铺数据
//        Shop shop = getById(id);
//        Thread.sleep(200);
//        //封装逻辑过期
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(Senconds));
//        //写入redis
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id ,JSONUtil.toJsonStr(redisData));
//    }
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long shopId = shop.getId();
        if(shopId == null){
            return Result.fail("店铺ID不能为空");
        }
        //1.更新数据库
        updateById(shop);
        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shopId);

        return Result.ok();
    }
}
