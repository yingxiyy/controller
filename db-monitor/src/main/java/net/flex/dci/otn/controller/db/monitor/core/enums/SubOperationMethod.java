package net.flex.dci.otn.controller.db.monitor.core.enums;

import lombok.Getter;

/**
 * v2‐diff 子字段	语义	v1 等价操作 "u":{ "to": X }	字段直接被整块替换为 X	$set: { field: X } "u":{ "ne":[…]
 * }	数组新增元素（push）	$push: { field: […] } "u":{ "oe":[…] }	数组移除元素（pull）	$pull: { field: … }
 *
 * @version 1.0
 * @date 7/10/2025 2:32 PM
 */
@Getter
public enum SubOperationMethod {
    to(""),
    ne("$push"),
    oe("$pull");

    private final String mongoOperation;

    SubOperationMethod(String mongoOperation) {
        this.mongoOperation = mongoOperation;
    }
}
