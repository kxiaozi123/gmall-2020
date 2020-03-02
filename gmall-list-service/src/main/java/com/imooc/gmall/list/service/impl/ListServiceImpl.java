package com.imooc.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.imooc.gmall.SkuLsInfo;
import com.imooc.gmall.SkuLsParams;
import com.imooc.gmall.SkuLsResult;
import com.imooc.gmall.config.RedisUtil;
import com.imooc.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.apache.lucene.queryparser.xml.builders.FilteredQueryBuilder;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {
    @Autowired
    private JestClient jestClient;
    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";
    @Autowired
    private RedisUtil redisUtil;
    //PUT /movie_index/movie/1
    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {
        Index index = new Index.Builder(skuLsInfo)
                .index(ES_INDEX)
                .type(ES_TYPE)
                .id(skuLsInfo.getId()).build();
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
          /*
            1.  定义dsl 语句
            2.  定义动作
            3.  执行动作
            4.  获取结果集
         */
        String query = makeQueryStringForSearch(skuLsParams);
        Search build = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult=null;
        try {
            searchResult = jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }
        SkuLsResult skuLsResult = makeResultForSearch(searchResult,skuLsParams);

        return skuLsResult;
    }

    @Override
    public void incrHotScore(String skuId) {
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String hotKey="hotScore";
        // 保存数据 skuId:33 ,skuId:34
        Double hotScore = jedis.zincrby(hotKey, 1, "skuId:" + skuId);
        // 按照一定的规则，来更新es
        if (hotScore%10==0){
            // 则更新一次es
            updateHotScore(skuId,  Math.round(hotScore));
        }

    }

    private void updateHotScore(String skuId, long hotScore) {
         /*
        1.  编写dsl 语句
        2.  定义动作
        3.  执行
         */
        // POST /gmall/SkuInfo/3/_update
        String upd="{\n" +
                "  \"doc\": {\n" +
                "     \"hotScore\": "+hotScore+"\n" +
                "  }\n" +
                "}";
        Update update = new Update.Builder(upd).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //设置返回结果
    private SkuLsResult makeResultForSearch(SearchResult searchResult, SkuLsParams skuLsParams) {
        SkuLsResult skuLsResult=new SkuLsResult();
        List<SkuLsInfo> skuLsInfoList=new ArrayList<>();
        List<String> attrValueIdList=new ArrayList<>();
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo skuLsInfo = hit.source;
            //获取高亮
            if(hit.highlight!=null && hit.highlight.size()>0)
            {
                Map<String, List<String>> highlight = hit.highlight;
                List<String> skuName = highlight.get("skuName");
                skuLsInfo.setSkuName(skuName.get(0));
            }
            skuLsInfoList.add(skuLsInfo);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoList);
        skuLsResult.setTotal(searchResult.getTotal());
        // 10  (10+3-1)/3
        long totalPages = (searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPages);

        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        for (TermsAggregation.Entry bucket : groupby_attr.getBuckets()) {
            String key = bucket.getKey();
            attrValueIdList.add(key);
        }
        skuLsResult.setAttrValueIdList(attrValueIdList);
        //
        //    private List<String> attrValueIdList;

        return skuLsResult;
    }
    //生成dsl语句
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        // 定义一个查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 创建 bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 判断keyword 是否为空
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            // 创建match
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",skuLsParams.getKeyword());
            // 创建must
            boolQueryBuilder.must(matchQueryBuilder);
            // 设置高亮
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();

            // 设置高亮的规则
            highlighter.field("skuName");
            highlighter.preTags("<span style=color:red>");
            highlighter.postTags("</span>");

            // 将设置好的高亮对象放入查询器中
            searchSourceBuilder.highlight(highlighter);
        }

        // 判断平台属性值Id
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            // 循环
            for (String valueId : skuLsParams.getValueId()) {
                // 创建term
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId);
                // 创建filter 并添加term
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        // 判断 三级分类Id
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            // 创建term
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",skuLsParams.getCatalog3Id());
            // 创建filter 并添加term
            boolQueryBuilder.filter(termQueryBuilder);
        }
        // query --bool
        searchSourceBuilder.query(boolQueryBuilder);

        // 设置分页
        // from 从第几条开始查询
        // 10 条 每页 3  第一页 0 3 ，第二页 3,3 第三页 6，3

        int from = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        // size 每页显示的条数
        searchSourceBuilder.size(skuLsParams.getPageSize());

        // 设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        // 聚合
        // 创建一个对象 aggs:--terms
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr");
        // "field": "skuAttrValueList.valueId"
        groupby_attr.field("skuAttrValueList.valueId");
        // aggs 放入查询器
        searchSourceBuilder.aggregation(groupby_attr);

        String query = searchSourceBuilder.toString();
        System.out.println("query:="+query);
        return query;
    }
}
