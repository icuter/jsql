package cn.icuter.jsql.condition;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author edward
 * @since 2018-08-05
 */
public class Conditions implements Condition {

    private final Combination combination;
    private List<Condition> conditionList;

    Conditions(Combination combination) {
        this.combination = combination;
        conditionList = new LinkedList<Condition>();
    }

    Conditions addCondition(Condition ...condition) {
        Collections.addAll(conditionList, condition);
        return this;
    }

    public String toSql() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int i = 0; i < conditionList.size(); i++) {
            builder.append(conditionList.get(i).toSql());
            if (i != conditionList.size() - 1) {
                builder.append(" ").append(combination.getSymbol()).append(" ");
            }
        }
        return builder.append(")").toString();
    }

    @Override
    public String getField() {
        throw new UnsupportedOperationException("getField does not support in Conditions");
    }

    @Override
    public Object getValue() {
        List<Object> values = new LinkedList<Object>();
        for (Condition condition : conditionList) {
            if (condition.prepareType() == PrepareType.PLACEHOLDER.getType()) {
                values.add(condition.getValue());
            }
        }
        return values;
    }

    @Override
    public int prepareType() {
        return PrepareType.PLACEHOLDER.getType();
    }

}
