package cn.icuter.jsql.transaction;

import cn.icuter.jsql.datasource.TransactionDataSource;

public interface TransactionOperation {
    void doTransaction(TransactionDataSource dataSource) throws Exception;
}
