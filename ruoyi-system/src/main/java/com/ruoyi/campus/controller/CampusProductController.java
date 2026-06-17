package com.ruoyi.campus.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.campus.service.CampusRecommendService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.campus.domain.CampusProduct;
import com.ruoyi.campus.service.ICampusProductService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 校园二手商品Controller
 * 
 * @author ruoyi
 * @date 2025-11-11
 */
@RestController
@RequestMapping("/campus/product")
public class CampusProductController extends BaseController {
    @Autowired
    private ICampusProductService campusProductService;

    @Autowired
    private CampusRecommendService recommendService;

    /**
     * 查询校园二手商品列表
     */
    // @PreAuthorize("@ss.hasPermi('campus:product:list')") // 【修改】允许所有登录用户访问
    @GetMapping("/list")
    public TableDataInfo list(CampusProduct campusProduct) {
        startPage();
        List<CampusProduct> list = campusProductService.selectCampusProductList(campusProduct);
        return getDataTable(list);
    }

    /**
     * 导出校园二手商品列表
     */
    @PreAuthorize("@ss.hasPermi('campus:product:export')")
    @Log(title = "校园二手商品", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CampusProduct campusProduct) {
        List<CampusProduct> list = campusProductService.selectCampusProductList(campusProduct);
        ExcelUtil<CampusProduct> util = new ExcelUtil<CampusProduct>(CampusProduct.class);
        util.exportExcel(response, list, "校园二手商品数据");
    }

    /**
     * 获取校园二手商品详细信息
     */
    // @PreAuthorize("@ss.hasPermi('campus:product:query')") // 【修改】允许所有登录用户访问
    @GetMapping(value = "/{productId}")
    public AjaxResult getInfo(@PathVariable("productId") Long productId) {
        return success(campusProductService.selectCampusProductByProductId(productId));
    }

    /**
     * 新增校园二手商品
     */
    // @PreAuthorize("@ss.hasPermi('campus:product:add')") // 【修改】允许所有登录用户发布商品
    @Log(title = "校园二手商品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CampusProduct campusProduct) {
        campusProduct.setUserId(getUserId());
        return toAjax(campusProductService.insertCampusProduct(campusProduct));
    }

    /**
     * 修改校园二手商品
     */
    // @PreAuthorize("@ss.hasPermi('campus:product:edit')") // 【修改】允许所有登录用户编辑自己的商品
    @Log(title = "校园二手商品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CampusProduct campusProduct) {
        return toAjax(campusProductService.updateCampusProduct(campusProduct));
    }

    /**
     * 删除校园二手商品
     */
    @PreAuthorize("@ss.hasPermi('campus:product:remove')")
    @Log(title = "校园二手商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{productIds}")
    public AjaxResult remove(@PathVariable Long[] productIds) {
        return toAjax(campusProductService.deleteCampusProductByProductIds(productIds));
    }

    /**
     * 【新增】获取商品详情(增加浏览次数)
     */
    @GetMapping("/detail/{productId}")
    public AjaxResult getDetail(@PathVariable("productId") Long productId) {
        return success(campusProductService.selectProductDetail(productId, getUserId()));
    }

    /**
     * 【新增】更新商品状态
     */
    @PutMapping("/status/{productId}")
    public AjaxResult updateStatus(@PathVariable("productId") Long productId,
            @RequestBody CampusProduct campusProduct) {
        return toAjax(campusProductService.updateProductStatus(productId, campusProduct.getStatus(), getUserId()));
    }

    /**
     * 【新增】切换收藏状态
     */
    @PutMapping("/toggle-favorite/{productId}")
    public AjaxResult toggleFavorite(@PathVariable("productId") Long productId) {
        boolean isFavorited = campusProductService.toggleFavorite(productId, getUserId());
        return AjaxResult.success("操作成功", isFavorited);
    }

    /**
     * 【新增】查询我的商品列表
     */
    @GetMapping("/my-products")
    public TableDataInfo myProducts(CampusProduct campusProduct) {
        startPage();
        campusProduct.setUserId(getUserId());
        List<CampusProduct> list = campusProductService.selectMyProducts(campusProduct);
        return getDataTable(list);
    }

    /**
     * 【新增】查询我的收藏列表
     */
    @GetMapping("/my-favorites")
    public TableDataInfo myFavorites() {
        startPage();
        List<CampusProduct> list = campusProductService.selectMyFavoriteProducts(getUserId());
        return getDataTable(list);
    }

    /**
     * 智能算法API 1：记录用户浏览、收藏等行为 (前端静默调用)
     */
    @PostMapping("/recordBehavior")
    public AjaxResult recordBehavior(Long productId, Integer behaviorType) {
        recommendService.recordBehavior(productId, behaviorType);
        return AjaxResult.success();
    }

    /**
     * 智能算法API 2：获取“猜你喜欢”推荐列表
     */
    @GetMapping("/recommendList")
    public TableDataInfo getRecommendList() {
        int targetCount = 8; // 你原来设定的是 8 个
        List<CampusProduct> finalRecommendList = new ArrayList<>();

        // 1. 优先调用原本的推荐算法大厨
        try {
            List<CampusProduct> cfList = recommendService.getRecommendProducts(targetCount);
            if (cfList != null && !cfList.isEmpty()) {
                finalRecommendList.addAll(cfList);
            }
        } catch (Exception e) {
            // 如果算法报错，不要让程序崩溃，静默拦截，交给下一步兜底
            System.err.println("协同过滤推荐尚未生效或异常：" + e.getMessage());
        }

        // 2. 核心兜底：如果算法算出来的商品不到 8 个（比如前期一条数据都没有）
        if (finalRecommendList.size() < targetCount) {
            int needCount = targetCount - finalRecommendList.size();

            // 去商品表里把所有正常在售(0)的商品捞出来
            CampusProduct query = new CampusProduct();
            query.setStatus("0");
            List<CampusProduct> allProducts = campusProductService.selectCampusProductList(query);

            // 过滤掉算法已经推荐过的，防止重复展示
            List<Long> existIds = finalRecommendList.stream()
                    .map(CampusProduct::getProductId)
                    .collect(Collectors.toList());
            List<CampusProduct> fallbackPool = allProducts.stream()
                    .filter(p -> !existIds.contains(p.getProductId()))
                    .collect(Collectors.toList());

            // 把兜底池子里的商品打乱顺序，模拟推荐的“随机性”
            Collections.shuffle(fallbackPool);

            // 缺几个就补几个
            int addCount = Math.min(needCount, fallbackPool.size());
            finalRecommendList.addAll(fallbackPool.subList(0, addCount));
        }

        // 3. 返回若依系统标准的分页表格数据对象
        return getDataTable(finalRecommendList);
    }
}
