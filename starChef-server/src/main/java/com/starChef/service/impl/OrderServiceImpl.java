package com.starChef.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.starChef.constant.MessageConstant;
import com.starChef.context.BaseContext;
import com.starChef.dto.*;
import com.starChef.entity.*;
import com.starChef.exception.AddressBookBusinessException;
import com.starChef.exception.OrderBusinessException;
import com.starChef.mapper.*;
import com.starChef.result.PageResult;
import com.starChef.service.OrderService;
import com.starChef.utils.WeChatPayUtil;
import com.starChef.vo.OrderPaymentVO;
import com.starChef.vo.OrderStatisticsVO;
import com.starChef.vo.OrderSubmitVO;
import com.starChef.vo.OrderVO;
import com.starChef.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //处理各种业务异常(地址簿为空， 购物车数据为空)
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //查询当前用户购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            //购物车为空
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //2向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);

        orderMapper.insert(orders);////

        List<OrderDetail> orderDetailList = new ArrayList<>();
        //3向订单明细表插入多条数据
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());//设置订单id
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        //4清空当前用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //5封装VO返回数据
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        //模拟订单支付成功
        // 通过WebSocket向客户端推送消息
        Map map = new HashMap();
        map.put("type", 1);//1表示来单提醒， 2表示客户催单
        map.put("orderId", orders.getId());
        String outTradeNo = "11111111111111111";
        map.put("content", "订单号：" + outTradeNo);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 通过WebSocket向客户端推送消息
        Map map = new HashMap();
        map.put("type", 1);//1表示来单提醒， 2表示客户催单
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号：" + outTradeNo);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 历史订单查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<OrderVO> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVO = new ArrayList<>();
        if(page != null && page.getResult() != null){
            for (OrderVO vo : page) {
                Long orderId = vo.getId();
                OrderVO copyOrserVO = new OrderVO();
                BeanUtils.copyProperties(vo, copyOrserVO);
                List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(orderId);
                copyOrserVO.setOrderDetailList(orderDetailList);
                orderVO.add(copyOrserVO);
            }
        }

        return new PageResult(page.getTotal(), orderVO);
    }

    /**
     * 订单详情
     * @param id
     * @return
     */
    public OrderVO orderDetail(Long id) {
        OrderVO orderVO = orderMapper.getById(id);
        orderVO.setOrderDetailList(orderDetailMapper.listByOrderId(id));
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    public void userCancel(Long id){
        OrderVO oldOrder = orderMapper.getById(id);
        Orders newOrders = new Orders();
        BeanUtils.copyProperties(oldOrder, newOrders);

        newOrders.setStatus(Orders.CANCELLED);
        newOrders.setCancelReason(oldOrder.getCancelReason());
        newOrders.setCancelTime(LocalDateTime.now());
        orderMapper.update(newOrders);
    }

    /**
     * 再来一单
     * @param id
     */
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();

        List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(id);
        for (OrderDetail orderDetail : orderDetailList) {
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setUserId(userId);
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);

        }
    }

    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 订单确认
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO){
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }


    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO){
        Orders order = orderMapper.getById(ordersRejectionDTO.getId());
        if(order == null || !Objects.equals(order.getStatus(), Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    public void adminCancel(OrdersCancelDTO ordersCancelDTO){
        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    public void delivery(Long id){
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(orders);
    }

    public void complete(Long id){
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 订单催单
     * @param id
     */
    public void reminder(Long id){
        //根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        //判断订单状态是否为4
        if(ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map map = new HashMap();
        map.put("type", 2);//1表示来单提醒，2表示客户催单
        map.put("orderId", id);
        map.put("content", "订单号：" + ordersDB.getNumber());
        
        String json = JSON.toJSONString(map);

        //通过websocket向客户端推送消息
        webSocketServer.sendToAllClient(json);

    }

}
