package com.ruoyi.campus.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.campus.domain.CampusOrder;
import com.ruoyi.campus.service.ICampusOrderService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.campus.domain.dto.CreateOrderDto;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.exception.ServiceException;

/**
 * 校园订单Controller
 * * @author ruoyi
 */
@RestController
@RequestMapping("/campus/order")
public class CampusOrderController extends BaseController {

    @Autowired
    private ICampusOrderService campusOrderService;

    /**
     * 查询校园订单列表 (管理员后台使用)
     */
    @PreAuthorize("@ss.hasPermi('campus:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(CampusOrder campusOrder) {
        startPage();
        List<CampusOrder> list = campusOrderService.selectCampusOrderList(campusOrder);
        return getDataTable(list);
    }

    /**
     * 导出校园订单列表
     */
    @PreAuthorize("@ss.hasPermi('campus:order:export')")
    @Log(title = "校园订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CampusOrder campusOrder) {
        List<CampusOrder> list = campusOrderService.selectCampusOrderList(campusOrder);
        ExcelUtil<CampusOrder> util = new ExcelUtil<CampusOrder>(CampusOrder.class);
        util.exportExcel(response, list, "校园订单数据");
    }

    /**
     * 获取校园订单详细信息
     */
    @GetMapping(value = "/{orderId}")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId) {
        return success(campusOrderService.selectCampusOrderByOrderId(orderId));
    }

    /**
     * 新增校园订单 (管理员后台使用)
     */
    @PreAuthorize("@ss.hasPermi('campus:order:add')")
    @Log(title = "校园订单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CampusOrder campusOrder) {
        return toAjax(campusOrderService.insertCampusOrder(campusOrder));
    }

    /**
     * 修改校园订单 (管理员后台使用)
     */
    @PreAuthorize("@ss.hasPermi('campus:order:edit')")
    @Log(title = "校园订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CampusOrder campusOrder) {
        return toAjax(campusOrderService.updateCampusOrder(campusOrder));
    }

    /**
     * 删除校园订单 (管理员后台使用)
     */
    @PreAuthorize("@ss.hasPermi('campus:order:remove')")
    @Log(title = "校园订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds) {
        return toAjax(campusOrderService.deleteCampusOrderByOrderIds(orderIds));
    }

    // ================= 以下为【前台商城】使用的专属业务接口 =================

    /**
     * 【下单】用户创建订单
     */
    @PostMapping("/create")
    public AjaxResult create(@RequestBody CreateOrderDto createOrderDto) {
        if (createOrderDto.getProductId() == null) {
            return AjaxResult.error("商品ID不能为空");
        }
        if (StringUtils.isEmpty(createOrderDto.getAddress())) {
            return AjaxResult.error("收货地址不能为空");
        }
        try {
            Long orderId = campusOrderService.createOrder(createOrderDto);
            return AjaxResult.success("订单创建成功", orderId);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("创建订单失败，请联系管理员");
        }
    }

    /**
     * 【买家视角】查询我买到的订单
     */
    @GetMapping("/my")
    public TableDataInfo listMyOrders(CampusOrder campusOrder) {
        startPage();
        // 核心隔离逻辑：强制将查询条件中的“买家ID”设为当前登录的人
        campusOrder.setBuyerId(getUserId());
        List<CampusOrder> list = campusOrderService.selectCampusOrderList(campusOrder);
        return getDataTable(list);
    }

    /**
     * 【卖家视角】查询我卖出的订单
     */
    @GetMapping("/sold")
    public TableDataInfo listSoldOrders(CampusOrder campusOrder) {
        startPage();
        // 核心隔离逻辑：强制将查询条件中的“卖家ID”设为当前登录的人
        campusOrder.setSellerId(getUserId());
        List<CampusOrder> list = campusOrderService.selectCampusOrderList(campusOrder);
        return getDataTable(list);
    }

    /**
     * 【买家操作】取消订单
     */
    @PostMapping("/cancel/{orderId}")
    public AjaxResult cancelOrder(@PathVariable("orderId") Long orderId) {
        try {
            campusOrderService.cancelOrder(orderId, getUserId());
            return AjaxResult.success("订单已成功取消");
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 【卖家操作】发货
     */
    @PostMapping("/ship/{orderId}")
    public AjaxResult shipOrder(@PathVariable("orderId") Long orderId) {
        try {
            campusOrderService.shipOrder(orderId, getUserId());
            return AjaxResult.success("发货成功");
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 【买家操作】确认收货
     */
    @PostMapping("/confirm/{orderId}")
    public AjaxResult confirmReceipt(@PathVariable("orderId") Long orderId) {
        try {
            campusOrderService.confirmReceipt(orderId, getUserId());
            return AjaxResult.success("确认收货成功");
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 【买家操作】信用评分
     */
    @PostMapping("/rate/{orderId}")
    public AjaxResult rateOrder(@PathVariable("orderId") Long orderId,
                                @RequestParam("score") Integer score,
                                @RequestParam(value = "comment", required = false) String comment) {
        try {
            campusOrderService.rateOrder(orderId, score, comment, getUserId());
            return AjaxResult.success("评价成功，感谢您的反馈！");
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}