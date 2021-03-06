package com.centit.framework.system.dao.hibernateimpl;

import com.centit.framework.core.dao.CodeBook;
import com.centit.framework.hibernate.dao.BaseDaoImpl;
import com.centit.framework.hibernate.dao.DatabaseOptUtils;
import com.centit.framework.system.dao.OptMethodDao;
import com.centit.framework.system.po.OptMethod;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository("optMethodDao")
public class OptMethodDaoImpl extends BaseDaoImpl<OptMethod, String> implements OptMethodDao {

    @Transactional
    public List<OptMethod> listOptMethodByOptID(String sOptID) {
        return listObjects("FROM OptMethod WHERE optId =?", sOptID);
    }

    @Transactional
    public List<OptMethod> listOptMethodByRoleCode(String roleCode) {
        return listObjects("FROM OptMethod WHERE optCode in "
                + "(select id.optCode from RolePower where id.roleCode = ?)"
                + " order by optId", roleCode);
    }

    @Transactional
    public void deleteOptMethodsByOptID(String sOptID) {
        DatabaseOptUtils.doExecuteHql(this, "DELETE FROM OptMethod WHERE optId = ?", sOptID);
    }


    public Map<String, String> getFilterField() {
        if (filterField == null) {
            filterField = new HashMap<String, String>();
            filterField.put("OPTID", CodeBook.EQUAL_HQL_ID);
            filterField.put("PREOPTID", CodeBook.EQUAL_HQL_ID);
            filterField.put("ISINTOOLBAR", CodeBook.EQUAL_HQL_ID);
            filterField.put("TOPOPTID", CodeBook.EQUAL_HQL_ID);
            filterField.put("OPTTYPE", CodeBook.EQUAL_HQL_ID);
            filterField.put("OPTNAME", CodeBook.LIKE_HQL_ID);
        }
        return filterField;
    }

    @Transactional
    public String getNextOptCode() {
        return DatabaseOptUtils.getNextValueOfSequence(this, "S_OPTDEFCODE");
    }

    @Override
    public void updateOptMethod(OptMethod optMethod){
      super.updateObject(optMethod);
    }
}
