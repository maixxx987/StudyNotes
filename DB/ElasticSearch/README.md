# Elasticsearch Java Client使用笔记

> Elasticsearch Client 是 ES 官方推出的最新的API, 从7.15开始推荐使用, 替换原有的Rest Client



## 配置相关

### Maven依赖

```xml
<!-- 正常依赖 -->
<dependency>
    <groupId>co.elastic.clients</groupId>
    <artifactId>elasticsearch-java</artifactId>
    <version>7.17.7</version>
</dependency>
        
<!-- 如果提示有找不到jakarta.json相关的类可以修改如下 -->
<dependency>
    <groupId>jakarta.json</groupId>
    <artifactId>jakarta.json-api</artifactId>
    <version>2.1.1</version>
</dependency>
<dependency>
    <groupId>co.elastic.clients</groupId>
    <artifactId>elasticsearch-java</artifactId>
    <version>7.17.7</version>
    <exclusions>
        <exclusion>
            <groupId>jakarta.json</groupId>
            <artifactId>jakarta.json-api</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```



### 配置文件

```yaml
elasticsearch:
  host: debain.wsl
  port: 9200
```



### 配置Bean

```java
@Bean
public ElasticsearchClient creat() {
    RestClient restClient = RestClient.builder(new HttpHost("debain.wsl", 9200)).build();
    ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
    return new ElasticsearchClient(transport);
}
```



## 具体的API操作

### 索引

```java
/**
 * 判断索引是否存在
 *
 * @param index 索引
 */
public Boolean indexIsExist(String index) throws IOException {
    BooleanResponse booleanResponse = elasticsearchClient
        .indices()
        .exists(existsRequest -> existsRequest
                .index(index));
    log.info("[indexIsExist]索引:{}, 是否存在:{}", index, booleanResponse.value());
    return booleanResponse.value();
}



/**
 * 创建索引
 *
 * @param index 索引
 */
public void createIndex(String index) throws IOException {
    CreateIndexResponse createIndexResponse = elasticsearchClient
        .indices()
        .create(createIndexRequest -> createIndexRequest
                .index(index)
                .mappings(mapping -> mapping
                          .properties("id", objectBuilder -> objectBuilder.keyword(keyword -> keyword.index(true)))
                          .properties("name", objectBuilder -> objectBuilder.text(keyword -> keyword
                                  // 排序
                                  .fielddata(true)
                                  // 分词
                                  .analyzer("ik_max_word")))
                          .properties("age", objectBuilder -> objectBuilder.integer(intProperty -> intProperty))
                          .properties("birthday", objectBuilder -> objectBuilder.date(dateProperty -> dateProperty))
                          .properties("isDeleted", objectBuilder -> objectBuilder.boolean_(boolProperty -> boolProperty))));
    log.info("[createIndex]索引:{}, 是否创建成功:{}", index, createIndexResponse.acknowledged());
}


/**
 * 删除索引
 *
 * @param index 索引
 */
public Boolean deleteIndex(String index) {
    DeleteIndexResponse deleteResponse = elasticsearchClient
        .indices()
        .delete(builder -> builder.index(index));
    log.info("[deleteIndex]索引:{}, 是否删除成功:{}", index,  deleteResponse.acknowledged());
    return deleteResponse.acknowledged();
}


/**
 * 新增/更新别名
 *
 * @param index 索引
 * @param alias 别名
 */
public Boolean updateAliases(String index, String alias) throws IOException {
    UpdateAliasesResponse updateAliasesResponse = elasticsearchClient
        .indices()
        .updateAliases(updateBuilder -> updateBuilder
                       .actions(actionBuilder -> actionBuilder
                                .add(addBuilder -> addBuilder
                                     .index(index)
                                     .alias(alias))));
    log.info("[updateAliases]索引:{}, 更新别名:{}, 是否成功:{}", index, alias, updateAliasesResponse.acknowledged());
    return false;
}


 /**
  * 给索引设置别名
  *
  * @param index 索引
  * @param alias 别名
  */
public boolean putAlias(String index, String alias) {
    PutAliasResponse putAliasResponse = elasticsearchClient
        .indices()
        .putAlias(aliasBuilder -> aliasBuilder
                  .index(index)
                  .isWriteIndex(true)
                  .name(alias));
    log.info("[putAlias]索引:{}, 添加别名:{}, 是否成功:{}", index, alias, putAliasResponse.acknowledged());
    return putAliasResponse.acknowledged();
}


/**
 * 根据别名获取索引
 *
 * @param alias 别名
 */
public Map<String, IndexAliases> getIndexByAlias(String alias) throws IOException {
    Map<String, IndexAliases> resultMap = elasticsearchClient
        .indices()
        .getAlias(aliasBuilder -> aliasBuilder.name(alias))
        .result();
    resultMap.forEach((index, indexAliases) -> {
        log.info("[getIndexByAlias]索引:{}, 别名:{}", index, JSON.toJSONString(indexAliases));
    });
    return resultMap;
}


/**
 * 删除索引别名
 *
 * @param index 索引
 * @param alias 别名
 */
public Boolean deleteAlias(String index, String alias) {
    DeleteAliasResponse deleteAliasResponse = elasticsearchClient
        .indices()
        .deleteAlias(aliasBuilder -> aliasBuilder
                     .index(index)
                     .name(aliasBuilder));
    log.info("[deleteAlias]索引:{}, 别名:{} 是否删除成功:{}", index,alias, deleteAliasResponse.acknowledged());
    return deleteAliasResponse.acknowledged();
}

/**
 * 获取索引的属性列表
 *  
 * @param index 索引名称
 * @return
 */
public Map<String, Property> getIndexProperties(String index) {
    // 查询索引创建情况
    GetIndexResponse getIndexResponse = elasticsearchClient.indices().get(getIndexRequest -> getIndexRequest.index(index));
    IndexState indexState = getIndexResponse.get(index);
    if (indexState == null || indexState.mappings() == null) {
        log.error("[getIndexProperties]索引:{} 不存在", index);
        return null;
    }
    Map<String, Property> propertyMap = indexState.mappings().properties();
    propertyMap.forEach((propertyName, property) -> {
        log.info("[getIndexProperties]索引:{}, 属性名:{} 属性:{}", index, propertyName, JSON.toJSONString(property));
    });
    return propertyMap;
}

/**
 * 复制索引数据
 *
 * @param sourceIndex 源索引
 * @param destIndex   新索引
 */
public ReindexResponse reIndex(String sourceIndex, String destIndex) throws IOException {
    ReindexResponse reindexResponse = elasticsearchClient.reindex(index -> index
                                                                  .dest(dest -> dest
                                                                        .index(destIndex))
                                                                  .source(source -> source
                                                                          .index(sourceIndex))
                                                                  .conflicts(Conflicts.Proceed));
    log.error("[reIndex]源索引:{}, 新索引:{} 失败数:{}", sourceIndex, destIndex, reindexResponse.failures().size());
    return reindexResponse;
}
```



### doc查询

```java
/**
 * 根据esId获取
 * 
 * @param index 索引
 * @param esId  es doc的id值
 */
public <T> T getEsId(String index, String esId, Class<T> clazz) throws IOException {
    GetResponse<T> getResponse = elasticsearchClient.get(getRequest -> getRequest.index(index).id(esId), clazz);
    return getResponse.source();
}


/**
 * 查询
 *
 * @param clazz 返回实体类
 */
public <T> Map<String, T> search(Class<T> clazz) throws IOException {
    long total = 0L;
    Map<String, T> resultMap = new HashMap<>();
    SearchResponse<T> response = elasticsearchClient.search(searchRequest -> searchRequest
            .query(query -> query.bool(builder -> builder
                    // 精确查询
                    // 如果要取反则是mustNot(如 != , NOT LIKE, NOT IN)
                    .must(must -> must.term(term -> term.field("name").value("王")))
                    .must(must -> must.term(term -> term.field("age").value(10)))
                    // 模糊查询
                    // 1.match 分词匹配检索，可以对查询条件分词，查到更多匹配的内容，结合不同的分词器，可以得到不同的效果
                    // 2.wildcard 通配符检索功能就像传统的SQL like一样，如果数据在es，你又想得到传统的“模糊查询”结构时
                    // 3.fuzzy 纠错检索，让输入条件有容错性
                    .must(must -> must.wildcard(fuzzy -> fuzzy.field("name").value("*王*")))
                    // 范围查询, 日期也是可以的
                    // 大于 -> gt
                    // 大于等于 -> gte
                    // 小于 -> lt
                    // 小于等于 -> lte
                    .must(must -> must.range(range -> range.field("age").gte(JsonData.of(10))))
                    // IN查询
                    .must(must -> must.terms(terms -> terms.field("name").terms(queryField -> queryField.value(
                            Lists.newArrayList("张三", "李四", "王五").stream().map(FieldValue::of).collect(Collectors.toList())))))
            ))
            // 分页起始
            .from(0)
            // 分页大小
            .size(10)
            // 排序
            .sort(sortBuilder -> sortBuilder
                    .field(field -> field.field("name").order(SortOrder.Asc))
            ), clazz);

    if (Objects.nonNull(response.hits())) {
        if (Objects.nonNull(response.hits().total())) {
            // 查询总数. 当数量过大且total的relation不为eq的时候, 总数不准. 需要设置一下
            total = response.hits().total().value();
        }
        if (!CollectionUtils.isEmpty(response.hits().hits())) {
            // id为es的Id, source为对象
            response.hits().hits().forEach(hit -> resultMap.put(hit.id(), hit.source()));
        }
    }
    return resultMap;
}


```



### doc增删改

```java
/**
 * 批量插入
 *
 * @param index    索引
 * @param dataList 数据
 * @param <T>
 */
public <T> void insertBatch(String index, List<T> dataList) throws IOException {
    List<BulkOperation> list = new ArrayList<>();
    for (T data : dataList) {
        list.add(BulkOperation.of(builder -> builder.create(create -> create.document(data).index(index))));
    }
    BulkResponse bulkResponse = elasticsearchClient.bulk(builder -> builder.index(index).operations(list));
    log.info("[insertBatch]是否错误:{}", bulkResponse.errors());
}


/**
 * 新增
 *
 * @param index    索引
 * @param idVal    id值
 * @param paramMap 实体参数map key-字段名 value-字段值
 */
public void create(String index, tring idVal, Map<String, Object> paramMap) throws IOException {
    CreateResponse createResponse = elasticsearchClient.create(create -> create.index(index).id(idVal).document(paramMap));
}

/**
 * 根据esId更新
 *
 * @param index    索引
 * @param esId     es doc的Id
 * @param paramMap 实体参数map key-字段名 value-字段值
 */
public void update(String index, String esId, Map<String, Object> paramMap) throws IOException {
    UpdateResponse<Map> updateResponse = elasticsearchClient.update(update -> update.index(index).id(esId).doc(paramMap), Map.class);
}

/**
 * 根据条件精确删除
 *
 * @param index 索引
 */
public void deleteByQuery(String index) throws IOException {
    DeleteByQueryResponse deleteResponse = elasticsearchClient.deleteByQuery(delete -> delete
            .index(index)
            .query(query -> query
                    .bool(bool -> bool.must(must -> must.term(term -> term.field("name").value("王"))))));
}
```


## 参考内容

- [Java Client | Elastic](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/index.html)
- [Elastic Stack-3：新版 ElasticSearch Java Client 尝鲜 - 掘金 (juejin.cn)](https://juejin.cn/post/7046759829255225351)
