package com.hmdp.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.RateLimiter;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.rebbitmq.MQSender;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;


@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private MQSender mqSender;

    private RateLimiter rateLimiter=RateLimiter.create(10);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        //令牌桶算法 限流
        if (!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)){
            return Result.fail("目前网络正忙，请重试");
        }
        //1.执行lua脚本
        Long userId = UserHolder.getUser().getId();

        Long r = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
        //2.判断结果为0
        int result = r.intValue();
        if (result != 0) {
            //2.1不为0代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "该用户重复下单");
        }
        //2.2为0代表有购买资格,将下单信息保存到阻塞队列

        //2.3创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //2.4订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //2.5用户id
        voucherOrder.setUserId(userId);
        //2.6代金卷id
        voucherOrder.setVoucherId(voucherId);

        //2.7将信息放入MQ中
        mqSender.sendSeckillMessage(JSON.toJSONString(voucherOrder));


        //2.7 返回订单id
        return Result.ok(orderId);
    }



}
