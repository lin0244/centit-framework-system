package com.centit.framework.system.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.centit.framework.common.ViewDataTransform;
import com.centit.framework.components.OperationLogCenter;
import com.centit.framework.common.JsonResultUtils;
import com.centit.framework.common.ResponseData;
import com.centit.framework.common.ResponseMapData;
import com.centit.framework.core.controller.BaseController;
import com.centit.framework.model.basedata.IOptInfo;
import com.centit.framework.model.basedata.OperationLog;
import com.centit.framework.system.po.OptInfo;
import com.centit.framework.system.po.OptMethod;
import com.centit.framework.system.service.OptInfoManager;
import com.centit.framework.system.service.OptMethodManager;
import com.centit.framework.system.service.SysRoleManager;
import com.centit.support.json.JsonPropertyUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/optinfo")
public class OptInfoController extends BaseController {
  @Resource
  private OptInfoManager optInfoManager;

  @Resource
  private OptMethodManager optMethodManager;

  @Resource
  @NotNull
  private SysRoleManager sysRoleManager;
  /**
   * 系统日志中记录
   */
  private String optId = "OPTINFO";//CodeRepositoryUtil.getCode("OPTID", "optInfo");

  /**
   * 查询所有系统业务
   *
   * @param id       父id
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   */
  @RequestMapping(value = "/sub", method = RequestMethod.GET)
  public void listFromParent(String id, HttpServletRequest request, HttpServletResponse response) {
    Map<String, Object> searchColumn = convertSearchColumn(request);

    if (StringUtils.isNotBlank(id)) {
      searchColumn.put("preOptId", id);
    } else {
      searchColumn.put("NP_TOPOPT", "true");
    }

    List<OptInfo> listObjects = optInfoManager.listObjects(searchColumn);

    for (OptInfo opt : listObjects) {
      //if("...".equals(opt.getOptRoute()))
      opt.setState(optInfoManager.hasChildren(opt.getOptId()) ?
        "closed" : "open");
    }
    JsonResultUtils.writeSingleDataJson(makeMenuFuncsJson(listObjects), response);
  }

  private JSONArray makeMenuFuncsJson(List<OptInfo> menuFunsByUser) {
    return ViewDataTransform.makeTreeViewJson(menuFunsByUser,
      ViewDataTransform.createStringHashMap("id", "optId",
        "optId", "optId",
        "pid", "preOptId",
        "text", "optName",
        "url", "optRoute",
        "icon", "icon",
        "children", "children",
        "isInToolbar", "isInToolbar",
        "state", "state"
      ), (jsonObject, obj) -> {
        jsonObject.put("external", !("D".equals(obj.getPageType())));
      });
  }

  /**
   * 查询所有系统业务
   *
   * @param field    需要显示的字段
   * @param struct   True根据父子节点排序的树形结构，False，排序的列表结构
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   */
  @RequestMapping(method = RequestMethod.GET)
  public void listAll(String[] field, boolean struct, HttpServletRequest request, HttpServletResponse response) {
    Map<String, Object> searchColumn = convertSearchColumn(request);
    List<OptInfo> listObjects = optInfoManager.listObjects(searchColumn);

    if (struct) {
      listObjects = optInfoManager.listObjectFormatTree(listObjects, false);
    }
    if (ArrayUtils.isNotEmpty(field))
      JsonResultUtils.writeSingleDataJson(listObjects, response,
        JsonPropertyUtils.getIncludePropPreFilter(OptInfo.class, field));
    else
      JsonResultUtils.writeSingleDataJson(listObjects, response);
  }


  /**
   * 查询所有需要通过权限管理的业务
   *
   * @param response HttpServletResponse
   */
  @RequestMapping(value = "/poweropts", method = RequestMethod.GET)
  public void listPowerOpts(HttpServletResponse response) {
    List<OptInfo> listObjects = optInfoManager.listSysAndOptPowerOpts();
    listObjects = optInfoManager.listObjectFormatTree(listObjects, true);
    JsonResultUtils.writeSingleDataJson(makeMenuFuncsJson(listObjects), response);
  }


  /**
   * 查询所有项目权限管理的业务
   *
   * @param field    需要显示的字段
   * @param response HttpServletResponse
   */
  @RequestMapping(value = "/itempoweropts", method = RequestMethod.GET)
  public void listItemPowerOpts(String[] field, HttpServletResponse response) {
    List<OptInfo> listObjects = optInfoManager.listItemPowerOpts();
    listObjects = optInfoManager.listObjectFormatTree(listObjects, true);

    if (ArrayUtils.isNotEmpty(field))
      JsonResultUtils.writeSingleDataJson(listObjects, response,
        JsonPropertyUtils.getIncludePropPreFilter(OptInfo.class, field));
    else
      JsonResultUtils.writeSingleDataJson(listObjects, response);
  }


  /**
   * 查询某个部门权限的业务
   *
   * @param field    需要显示的字段
   * @param unitCode unitCode
   * @param response HttpServletResponse
   */
  @RequestMapping(value = "/unitpoweropts/{unitCode}", method = RequestMethod.GET)
  public void listUnitPowerOpts(@PathVariable String unitCode, String[] field,
                                HttpServletResponse response) {
    List<OptInfo> listObjects = optInfoManager.listOptWithPowerUnderUnit(unitCode);
    listObjects = optInfoManager.listObjectFormatTree(listObjects, false);

    if (ArrayUtils.isNotEmpty(field))
      JsonResultUtils.writeSingleDataJson(listObjects, response,
        JsonPropertyUtils.getIncludePropPreFilter(OptInfo.class, field));
    else
      JsonResultUtils.writeSingleDataJson(listObjects, response);
  }

  /**
   * 新增业务
   *
   * @param optInfo  OptInfo
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   */
  @RequestMapping(method = {RequestMethod.POST})
  public void createOptInfo(@Valid OptInfo optInfo, HttpServletRequest request, HttpServletResponse response) {

    if (StringUtils.isBlank(optInfo.getOptRoute())) {
      optInfo.setOptRoute("...");
    }
    // 解决问题新增菜单没有url
    if (StringUtils.isBlank(optInfo.getOptUrl()) || "...".equals(optInfo.getOptUrl())) {
      optInfo.setOptUrl(optInfo.getOptRoute());
    }
    OptInfo parentOpt = optInfoManager.getOptInfoById(optInfo.getPreOptId());
    if (parentOpt == null)
      optInfo.setPreOptId("0");
    optInfoManager.saveNewOptInfo(optInfo);

    //刷新缓存
    sysRoleManager.loadRoleSecurityMetadata();

    JsonResultUtils.writeBlankJson(response);
    /*********log*********/
    OperationLogCenter.logNewObject(request, optId, optInfo.getOptId(), OperationLog.P_OPT_LOG_METHOD_C,
      "新增业务菜单", optInfo);
    /*********log*********/
  }

  /**
   * optId是否已存在
   *
   * @param optId    optId
   * @param response HttpServletResponse
   * @throws IOException IOException
   */
  @RequestMapping(value = "/notexists/{optId}", method = {RequestMethod.GET})
  public void isNotExists(@PathVariable String optId, HttpServletResponse response) throws IOException {
    OptInfo optInfo = optInfoManager.getObjectById(optId);
    JsonResultUtils.writeOriginalObject(null == optInfo, response);
  }

  /**
   * 更新
   *
   * @param optId    主键
   * @param optInfo  OptInfo
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   */
  @RequestMapping(value = "/{optId}", method = {RequestMethod.PUT})
  public void edit(@PathVariable String optId, @Valid OptInfo optInfo,
                   HttpServletRequest request, HttpServletResponse response) {

    OptInfo dbOptInfo = optInfoManager.getObjectById(optId);
    if (null == dbOptInfo) {
      JsonResultUtils.writeErrorMessageJson("当前对象不存在", response);
      return;
    }

    if (!StringUtils.equals(dbOptInfo.getPreOptId(), optInfo.getPreOptId())) {
      OptInfo parentOpt = optInfoManager.getOptInfoById(optInfo.getPreOptId());
      if (parentOpt == null)
        optInfo.setPreOptId(dbOptInfo.getPreOptId());
    }
    /*********log*********/
    OptInfo oldValue = new OptInfo();
    BeanUtils.copyProperties(dbOptInfo, oldValue);
    /*********log*********/

//        for (OptMethod optDef : optInfo.getOptMethods()) {
//            if (StringUtils.isBlank(optDef.getOptCode())) {
//                optDef.setOptCode(optMethodManager.getNextOptCode());
//            }
//         }
    BeanUtils.copyProperties(optInfo, dbOptInfo, "optMethods", "dataScopes");

//        dbOptInfo.addAllOptMethods(optInfo.getOptMethods());
//        dbOptInfo.addAllDataScopes(optInfo.getDataScopes());
    optInfoManager.updateOptInfo(dbOptInfo);
    //刷新缓存
//        sysRoleManager.loadRoleSecurityMetadata();
    /*********log*********/
    OperationLogCenter.logUpdateObject(request, this.optId, dbOptInfo.getOptId(), OperationLog.P_OPT_LOG_METHOD_U,
      "更新业务菜单" + dbOptInfo.getOptId(), dbOptInfo, oldValue);
    /*********log*********/

    JsonResultUtils.writeSuccessJson(response);
  }

  /**
   * 更新操作权限
   *
   * @param optId    主键
   * @param optInfo  OptInfo
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   */
  @RequestMapping(value = "/editpower{optId}", method = {RequestMethod.PUT})
  public void editPower(@PathVariable String optId, @Valid OptInfo optInfo,
                        HttpServletRequest request, HttpServletResponse response) {

    OptInfo dbOptInfo = optInfoManager.getObjectById(optId);
    if (null == dbOptInfo) {
      JsonResultUtils.writeErrorMessageJson("当前对象不存在", response);
      return;
    }

    if (!StringUtils.equals(dbOptInfo.getPreOptId(), optInfo.getPreOptId())) {
      OptInfo parentOpt = optInfoManager.getOptInfoById(optInfo.getPreOptId());
      if (parentOpt == null)
        optInfo.setPreOptId(dbOptInfo.getPreOptId());
    }
    /*********log*********/
    OptInfo oldValue = new OptInfo();
    BeanUtils.copyProperties(dbOptInfo, oldValue);
    /*********log*********/

    for (OptMethod optDef : optInfo.getOptMethods()) {
      if (StringUtils.isBlank(optDef.getOptCode())) {
        optDef.setOptCode(optMethodManager.getNextOptCode());
      }
    }
    BeanUtils.copyProperties(optInfo, dbOptInfo, "optMethods", "dataScopes");

    dbOptInfo.addAllOptMethods(optInfo.getOptMethods());
    dbOptInfo.addAllDataScopes(optInfo.getDataScopes());
    Map<String, List> old = optInfoManager.updateOperationPower(dbOptInfo);
    oldValue.setOptMethods(old.get("methods"));
    oldValue.setDataScopes(old.get("scopes"));
    //刷新缓存
    sysRoleManager.loadRoleSecurityMetadata();
    /*********log*********/
    OperationLogCenter.logUpdateObject(request, this.optId, dbOptInfo.getOptId(), OperationLog.P_OPT_LOG_METHOD_U,
      "更新操作权限" + dbOptInfo.getOptId(), dbOptInfo, oldValue);
    /*********log*********/

    JsonResultUtils.writeSuccessJson(response);
  }

  /**
   * 删除系统业务
   *
   * @param optId    主键
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   */
  @RequestMapping(value = "/{optId}", method = {RequestMethod.DELETE})
  public void delete(@PathVariable String optId, HttpServletRequest request, HttpServletResponse response) {
    OptInfo dboptInfo = optInfoManager.getObjectById(optId);

    optInfoManager.deleteOptInfo(dboptInfo);
    //刷新缓存
    sysRoleManager.loadRoleSecurityMetadata();
    JsonResultUtils.writeBlankJson(response);
    /*********log*********/
    OperationLogCenter.logDeleteObject(request, this.optId, dboptInfo.getOptId(), OperationLog.P_OPT_LOG_METHOD_D,
      "删除业务菜单", dboptInfo);
    /*********log*********/
  }

  /**
   * 查询单条数据
   *
   * @param optId    主键
   * @param response HttpServletResponse
   */
  @RequestMapping(value = "/{optId}", method = {RequestMethod.GET})
  public void getOptInfoById(@PathVariable String optId, HttpServletResponse response) {
    OptInfo dbOptInfo = optInfoManager.getOptInfoById(optId);

    JsonResultUtils.writeSingleDataJson(dbOptInfo, response);
  }

  /**
   * 新增页面时获取OptDef主键
   *
   * @param response HttpServletResponse
   */
  @RequestMapping(value = "/nextOptCode", method = RequestMethod.GET)
  public void getNextOptCode(HttpServletResponse response) {
    String optCode = optMethodManager.getNextOptCode();

    ResponseMapData responseData = new ResponseMapData();
    responseData.addResponseData("optCode", optCode);

    JsonResultUtils.writeResponseDataAsJson(responseData, response);
  }

  /**
   * 新建或更新业务操作
   *
   * @param optId    主键
   * @param optCode  optCode
   * @param optDef   OptMethod
   * @param response HttpServletResponse
   */
  @RequestMapping(value = "/{optId}/{optCode}", method = {RequestMethod.POST, RequestMethod.PUT})
  public void optDefEdit(@PathVariable String optId, @PathVariable String optCode, @Valid OptMethod optDef,
                         HttpServletResponse response) {
    OptInfo optInfo = optInfoManager.getObjectById(optId);
    if (null == optInfo) {
      JsonResultUtils.writeSingleErrorDataJson(
        ResponseData.ERROR_INTERNAL_SERVER_ERROR,
        "数据库不匹配", "数据库中不存在optId为" + optId + "的业务信息。", response);
      return;
    }

    OptMethod dbOptDef = optMethodManager.getObjectById(optCode);
    if (null == dbOptDef) {
      optDef.setOptId(optId);
      optMethodManager.mergeObject(optDef);
    } else {
      BeanUtils.copyProperties(optInfo, dbOptDef, new String[]{"optInfo"});
      optMethodManager.mergeObject(dbOptDef);
    }

    JsonResultUtils.writeSuccessJson(response);
  }

  @RequestMapping(value = "/allOptInfo", method = RequestMethod.GET)
  public void loadAllOptInfo(HttpServletResponse response) {
    List<OptInfo> optInfos = optInfoManager.listObjects();
    JsonResultUtils.writeSingleDataJson(optInfos, response);
  }

  @RequestMapping(value = "/allOptMethod", method = RequestMethod.GET)
  public void loadAllOptMethod(HttpServletResponse response) {
    List<OptMethod> optDefs = optMethodManager.listObjects();
    JsonResultUtils.writeSingleDataJson(optDefs, response);
  }
}
